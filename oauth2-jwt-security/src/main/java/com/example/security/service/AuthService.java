package com.example.security.service;

import com.example.security.model.RefreshToken;
import com.example.security.model.Role;
import com.example.security.model.Tenant;
import com.example.security.model.User;
import com.example.security.dto.AuthResponse;
import com.example.security.dto.LoginRequest;
import com.example.security.dto.RegisterRequest;
import com.example.security.repository.RoleRepository;
import com.example.security.repository.TenantRepository;
import com.example.security.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       @Lazy AuthenticationManager authenticationManager,
                       AuditService auditService,
                       RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
        this.redisTemplate = redisTemplate;
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            User user = userDetails.getUser();

            String tenantId = user.getTenant() != null ? user.getTenant().getName() : "default";
            String accessToken = jwtService.generateAccessToken(userDetails, tenantId);

            // Revoke old refresh tokens and issue a new one
            refreshTokenService.revokeAllUserTokens(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            auditService.log(user.getUsername(), "LOGIN", ipAddress, true, "Login successful");

            return new AuthResponse(accessToken, refreshToken.getToken(),
                3600, user.getUsername(), tenantId);
        } catch (Exception e) {
            auditService.log(request.getUsername(), "LOGIN_FAILED", ipAddress, false, e.getMessage());
            throw e;
        }
    }

    public AuthResponse refresh(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        refreshTokenService.verifyExpiry(refreshToken);

        User user = refreshToken.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String tenantId = user.getTenant() != null ? user.getTenant().getName() : "default";
        String newAccessToken = jwtService.generateAccessToken(userDetails, tenantId);

        auditService.log(user.getUsername(), "TOKEN_REFRESH", "N/A", true, "Token refreshed");

        return new AuthResponse(newAccessToken, refreshTokenStr,
            3600, user.getUsername(), tenantId);
    }

    public void logout(String accessToken, String username, String ipAddress) {
        // Blacklist the access token in Redis until it naturally expires
        try {
            long expiry = jwtService.extractExpiration(accessToken).getTime() - System.currentTimeMillis();
            if (expiry > 0) {
                redisTemplate.opsForValue().set(
                    "BLACKLIST:" + accessToken, "revoked", expiry, TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception ignored) {}

        // Revoke all refresh tokens for the user
        userRepository.findByUsername(username).ifPresent(user -> {
            refreshTokenService.revokeAllUserTokens(user);
            auditService.log(username, "LOGOUT", ipAddress, true, "Logout successful");
        });
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        // Assign default USER role
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName("USER").ifPresent(roles::add);
        user.setRoles(roles);

        // Assign tenant
        if (request.getTenantName() != null && !request.getTenantName().isBlank()) {
            tenantRepository.findByName(request.getTenantName()).ifPresent(user::setTenant);
        }

        return userRepository.save(user);
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("BLACKLIST:" + token));
    }
}
