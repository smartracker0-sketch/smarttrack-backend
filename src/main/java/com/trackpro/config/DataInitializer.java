package com.trackpro.config;

import com.trackpro.model.RoleEntity;
import com.trackpro.model.RoleName;
import com.trackpro.model.UserEntity;
import com.trackpro.repository.RoleRepository;
import com.trackpro.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.LinkedHashSet;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties adminProps;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BootstrapAdminProperties adminProps
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProps = adminProps;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureRoles();
        if (adminProps.enabled()) {
            ensureAdminUser();
        }
    }

    private void ensureRoles() {
        Arrays.stream(RoleName.values()).forEach(roleName -> {
            roleRepository.findByName(roleName).orElseGet(() -> {
                RoleEntity role = new RoleEntity();
                role.setName(roleName);
                return roleRepository.save(role);
            });
        });
    }

    private void ensureAdminUser() {
        if (userRepository.existsByEmailIgnoreCase(adminProps.email())) {
            return;
        }
        RoleEntity adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        UserEntity user = new UserEntity();
        user.setEmail(adminProps.email());
        user.setPasswordHash(passwordEncoder.encode(adminProps.password()));
        user.setDisplayName(adminProps.displayName());
        user.setEnabled(true);
        user.setRoles(new LinkedHashSet<>());
        user.getRoles().add(adminRole);
        userRepository.save(user);
    }
}
