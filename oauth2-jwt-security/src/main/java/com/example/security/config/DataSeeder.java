package com.example.security.config;

import com.example.security.model.Permission;
import com.example.security.model.Role;
import com.example.security.model.Tenant;
import com.example.security.model.User;
import com.example.security.repository.PermissionRepository;
import com.example.security.repository.RoleRepository;
import com.example.security.repository.TenantRepository;
import com.example.security.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedData(UserRepository userRepo,
                                      RoleRepository roleRepo,
                                      TenantRepository tenantRepo,
                                      PermissionRepository permRepo,
                                      PasswordEncoder encoder) {
        return args -> {
            if (userRepo.count() > 0) return; // already seeded

            // Tenants
            Tenant tenantA = tenantRepo.findByName("tenant-a").orElseGet(() -> {
                Tenant t = new Tenant();
                t.setName("tenant-a");
                return tenantRepo.save(t);
            });

            Tenant tenantB = tenantRepo.findByName("tenant-b").orElseGet(() -> {
                Tenant t = new Tenant();
                t.setName("tenant-b");
                return tenantRepo.save(t);
            });

            // Permissions
            Permission read = permRepo.findByName("READ").orElseGet(() -> {
                Permission p = new Permission();
                p.setName("READ");
                return permRepo.save(p);
            });

            Permission write = permRepo.findByName("WRITE").orElseGet(() -> {
                Permission p = new Permission();
                p.setName("WRITE");
                return permRepo.save(p);
            });

            Permission delete = permRepo.findByName("DELETE").orElseGet(() -> {
                Permission p = new Permission();
                p.setName("DELETE");
                return permRepo.save(p);
            });

            // Roles
            Role adminRole = roleRepo.findByName("ADMIN").orElseGet(() -> {
                Role r = new Role();
                r.setName("ADMIN");
                r.setPermissions(Set.of(read, write, delete));
                return roleRepo.save(r);
            });

            Role userRole = roleRepo.findByName("USER").orElseGet(() -> {
                Role r = new Role();
                r.setName("USER");
                r.setPermissions(Set.of(read));
                return roleRepo.save(r);
            });

            // Admin user
            if (!userRepo.findByUsername("admin").isPresent()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("Admin@1234"));
                admin.setEmail("admin@example.com");
                admin.setTenant(tenantA);
                Set<Role> adminRoles = new HashSet<>();
                adminRoles.add(adminRole);
                admin.setRoles(adminRoles);
                userRepo.save(admin);
            }

            // Regular user
            if (!userRepo.findByUsername("user").isPresent()) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(encoder.encode("User@1234"));
                user.setEmail("user@example.com");
                user.setTenant(tenantA);
                Set<Role> userRoles = new HashSet<>();
                userRoles.add(userRole);
                user.setRoles(userRoles);
                userRepo.save(user);
            }

            System.out.println("✅ Database seeded successfully!");
            System.out.println("   admin / Admin@1234  (tenant-a, ROLE_ADMIN)");
            System.out.println("   user  / User@1234   (tenant-a, ROLE_USER)");
        };
    }
}
