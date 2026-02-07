package com.example.mainbackend;

import com.example.mainbackend.constants.ConstraintTypeConstants;
import com.example.mainbackend.constants.PriorityConstants;
import com.example.mainbackend.constants.StatusConstants;
import com.example.mainbackend.entity.*;
import com.example.mainbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StatusRepository statusRepository;
    private final PriorityRepository priorityRepository;
    private final ConstraintTypeRepository constraintTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Seed lookup tables (always runs to ensure required values exist)
        seedStatuses();
        seedPriorities();
        seedConstraintTypes();

        // Seed users only if database is empty
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping user seeding");
            return;
        }

        seedRolesAndUsers();
    }

    private void seedStatuses() {
        for (String statusName : StatusConstants.REQUIRED_STATUSES) {
            if (!statusRepository.findByName(statusName).isPresent()) {
                Status status = Status.builder().name(statusName).build();
                statusRepository.save(status);
                log.info("Created status: {}", statusName);
            }
        }
    }

    private void seedPriorities() {
        int value = 1;
        for (String priorityName : PriorityConstants.REQUIRED_PRIORITIES) {
            if (!priorityRepository.findByName(priorityName).isPresent()) {
                Priority priority = Priority.builder()
                        .name(priorityName)
                        .value(value)
                        .build();
                priorityRepository.save(priority);
                log.info("Created priority: {} with value {}", priorityName, value);
            }
            value++;
        }
    }

    private void seedConstraintTypes() {
        String[] descriptions = {
            "Successor cannot start until predecessor finishes",
            "Successor cannot start until predecessor starts",
            "Successor cannot finish until predecessor finishes",
            "Successor cannot finish until predecessor starts"
        };

        for (int i = 0; i < ConstraintTypeConstants.REQUIRED_CONSTRAINT_TYPES.length; i++) {
            String typeName = ConstraintTypeConstants.REQUIRED_CONSTRAINT_TYPES[i];
            if (constraintTypeRepository.findByName(typeName).isEmpty()) {
                ConstraintType type = ConstraintType.builder()
                        .name(typeName)
                        .description(descriptions[i])
                        .build();
                constraintTypeRepository.save(type);
                log.info("Created constraint type: {}", typeName);
            }
        }
    }

    private void seedRolesAndUsers() {
        // Create roles
        Role adminRole = new Role();
        adminRole.setRoleName("ADMIN");
        roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setRoleName("USER");
        roleRepository.save(userRole);

        // Create admin user with admin role
        User adminUser = new User();
        adminUser.setNationalId("admin");
        adminUser.setPassword(passwordEncoder.encode("admin"));
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);
        adminUser.setRoles(roles);
        userRepository.save(adminUser);
        log.info("Created admin user");

        // Create regular user
        User user = new User();
        user.setNationalId("user");
        user.setPassword(passwordEncoder.encode("user"));
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        user.setRoles(userRoles);
        userRepository.save(user);
        log.info("Created regular user");
    }

}
