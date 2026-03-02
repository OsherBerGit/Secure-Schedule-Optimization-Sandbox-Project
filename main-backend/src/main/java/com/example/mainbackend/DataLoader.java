package com.example.mainbackend;

import com.example.mainbackend.constants.ConstraintTypeConstants;
import com.example.mainbackend.constants.PriorityConstants;
import com.example.mainbackend.constants.TaskStatusConstants;
import com.example.mainbackend.constants.VacationStatusConstants;
import com.example.mainbackend.entity.*;
import com.example.mainbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final VacationStatusRepository vacationStatusRepository;
    private final PriorityRepository priorityRepository;
    private final ConstraintTypeRepository constraintTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedTaskStatuses();
        seedVacationStatuses();
        seedPriorities();
        seedConstraintTypes();
        seedRolesAndUsers();
    }

    private void seedTaskStatuses() {
        for (String name : TaskStatusConstants.REQUIRED_STATUSES) {
            if (taskStatusRepository.findByName(name).isEmpty()) {
                taskStatusRepository.save(TaskStatus.builder().name(name).build());
                log.info("Created task status: {}", name);
            }
        }
    }

    private void seedVacationStatuses() {
        for (String name : VacationStatusConstants.REQUIRED_STATUSES) {
            if (vacationStatusRepository.findByName(name).isEmpty()) {
                vacationStatusRepository.save(VacationStatus.builder().name(name).build());
                log.info("Created vacation status: {}", name);
            }
        }
    }

    private void seedPriorities() {
        int value = 1;
        for (String priorityName : PriorityConstants.REQUIRED_PRIORITIES) {
            if (priorityRepository.findByName(priorityName).isEmpty()) {
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

    @Transactional
    public void seedRolesAndUsers() {
        // Ensure ADMIN and WORKER roles exist (create if missing)
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> { Role r = new Role(); r.setRoleName("ADMIN"); return roleRepository.save(r); });

        Role workerRole = roleRepository.findByRoleName("WORKER")
                .orElseGet(() -> { Role r = new Role(); r.setRoleName("WORKER"); return roleRepository.save(r); });

        // Note: stale USER role is left in DB — removing it causes FK issues.
        // The admin user below will be re-assigned to ADMIN+WORKER, fixing the 403.

        // Ensure admin user exists with correct roles
        User adminUser = userRepository.findByNationalId("admin").orElseGet(() -> {
            User u = new User();
            u.setNationalId("admin");
            u.setPassword(passwordEncoder.encode("admin"));
            log.info("Created admin user (nationalId=admin, password=admin)");
            return u;
        });
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminRoles.add(workerRole);
        adminUser.setRoles(adminRoles);
        userRepository.save(adminUser);
        log.info("Ensured admin user has ADMIN + WORKER roles");

        // Ensure worker user exists with correct roles
        User workerUser = userRepository.findByNationalId("worker").orElseGet(() -> {
            User u = new User();
            u.setNationalId("worker");
            u.setPassword(passwordEncoder.encode("worker"));
            log.info("Created worker user (nationalId=worker, password=worker)");
            return u;
        });
        Set<Role> workerRoles = new HashSet<>();
        workerRoles.add(workerRole);
        workerUser.setRoles(workerRoles);
        userRepository.save(workerUser);
        log.info("Ensured worker user has WORKER role");
    }
}
