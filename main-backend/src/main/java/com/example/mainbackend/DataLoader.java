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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final SettlementStatusRepository settlementStatusRepository;
    private final VacationStatusRepository vacationStatusRepository;
    private final PriorityRepository priorityRepository;
    private final ConstraintTypeRepository constraintTypeRepository;
    private final TaskRepository taskRepository;
    private final TaskConstraintRepository taskConstraintRepository;
    private final VacationRepository vacationRepository;
    private final SettlementRepository settlementRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedTaskStatuses();
        seedSettlementStatuses();
        seedVacationStatuses();
        seedPriorities();
        seedConstraintTypes();
        seedRolesAndUsers();
        seedTasks();
        seedVacations();
        seedSettlements();
    }

    // -------------------------------------------------------------------------
    // Lookup table seeds (unchanged)
    // -------------------------------------------------------------------------

    private void seedTaskStatuses() {
        seedTaskStatus(TaskStatusConstants.TASK_OPEN,      "#3B82F6"); // blue   — available
        seedTaskStatus(TaskStatusConstants.TASK_LOCKED,    "#F59E0B"); // amber  — assigned (manual lock)
        seedTaskStatus(TaskStatusConstants.TASK_SCHEDULED, "#A855F7"); // purple — assigned by algorithm
        seedTaskStatus(TaskStatusConstants.TASK_CLOSED,    "#10B981"); // green  — finished
    }

    private void seedTaskStatus(String name, String colorCode) {
        if (taskStatusRepository.findByName(name).isEmpty()) {
            taskStatusRepository.save(TaskStatus.builder()
                    .name(name).colorCode(colorCode).build());
            log.info("Seeded task status: {} ({})", name, colorCode);
        }
    }

    private void seedSettlementStatuses() {
        seedSettlementStatus(TaskStatusConstants.SETTLEMENT_PENDING,     "#6B7280"); // grey   — waiting
        seedSettlementStatus(TaskStatusConstants.SETTLEMENT_ASSIGNED,    "#3B82F6"); // blue   — assigned by algo
        seedSettlementStatus(TaskStatusConstants.SETTLEMENT_IN_PROGRESS, "#8B5CF6"); // violet — working
        seedSettlementStatus(TaskStatusConstants.SETTLEMENT_COMPLETED,   "#10B981"); // green  — done
        seedSettlementStatus(TaskStatusConstants.SETTLEMENT_FAILED,      "#EF4444"); // red    — failed
    }

    private void seedSettlementStatus(String name, String colorCode) {
        if (settlementStatusRepository.findByName(name).isEmpty()) {
            settlementStatusRepository.save(SettlementStatus.builder()
                    .name(name).colorCode(colorCode).build());
            log.info("Seeded settlement status: {} ({})", name, colorCode);
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
                priorityRepository.save(Priority.builder().name(priorityName).value(value).build());
                log.info("Created priority: {} (value={})", priorityName, value);
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
                constraintTypeRepository.save(ConstraintType.builder()
                        .name(typeName).description(descriptions[i]).build());
                log.info("Created constraint type: {}", typeName);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Roles + Users
    // -------------------------------------------------------------------------

    @Transactional
    public void seedRolesAndUsers() {
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN")));
        Role workerRole = roleRepository.findByRoleName("WORKER")
                .orElseGet(() -> roleRepository.save(new Role(null, "WORKER")));

        // Admin user (also has WORKER role so the algorithm can assign tasks to him)
        upsertUser("admin",  "Admin",   "User",    "admin@company.com",  8, 10, Set.of(adminRole, workerRole));

        // Workers — different availability and maxTasks to make scheduling interesting
        upsertUser("worker", "John",    "Doe",     "john@company.com",   8, 5,  Set.of(workerRole));
        upsertUser("alice",  "Alice",   "Smith",   "alice@company.com",  6, 4,  Set.of(workerRole));
        upsertUser("bob",    "Bob",     "Johnson", "bob@company.com",    7, 3,  Set.of(workerRole));
        upsertUser("carol",  "Carol",   "Williams","carol@company.com",  5, 4,  Set.of(workerRole));
    }

    private void upsertUser(String nationalId, String firstName, String lastName,
                             String email, int availability, int maxTasks, Set<Role> roles) {
        User user = userRepository.findByNationalId(nationalId).orElseGet(() -> {
            User u = new User();
            u.setNationalId(nationalId);
            u.setPassword(passwordEncoder.encode(nationalId)); // password = nationalId
            log.info("Created user: {} (password={})", nationalId, nationalId);
            return u;
        });
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setDailyAvailabilityHours(availability);
        user.setMaxTasks(maxTasks);
        user.setRoles(new HashSet<>(roles));
        userRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // Tasks + Constraints
    // -------------------------------------------------------------------------

    @Transactional
    public void seedTasks() {
        // Only seed if no tasks exist yet
        if (taskRepository.count() > 0) {
            log.info("Tasks already exist — skipping task seed");
            return;
        }

        Priority low      = priorityRepository.findByName(PriorityConstants.LOW).orElseThrow();
        Priority medium   = priorityRepository.findByName(PriorityConstants.MEDIUM).orElseThrow();
        Priority high     = priorityRepository.findByName(PriorityConstants.HIGH).orElseThrow();
        Priority critical = priorityRepository.findByName(PriorityConstants.CRITICAL).orElseThrow();

        // All seeded tasks start as OPEN — ready for the algorithm to pick up
        TaskStatus open = taskStatusRepository.findByName(TaskStatusConstants.TASK_OPEN).orElseThrow();

        Role workerRole = roleRepository.findByRoleName("WORKER").orElseThrow();
        Role adminRole  = roleRepository.findByRoleName("ADMIN").orElseThrow();

        LocalDateTime now = LocalDateTime.now();

        // 10 tasks — unassigned, ready for the algorithm
        Task t1 = save(Task.builder()
                .title("Design Database Schema")
                .description("Design the full relational DB schema for the project")
                .durationHours(4).deadline(now.plusDays(3))
                .priority(high).status(open)
                .requiredRoles(Set.of(workerRole)).build());

        save(Task.builder()
                .title("Setup CI/CD Pipeline")
                .description("Configure GitHub Actions for build and deploy")
                .durationHours(6).deadline(now.plusDays(5))
                .priority(medium).status(open)
                .requiredRoles(Set.of(workerRole)).build());

        Task t3 = save(Task.builder()
                .title("Implement Authentication")
                .description("JWT-based login, refresh tokens and role guards")
                .durationHours(8).deadline(now.plusDays(4))
                .priority(critical).status(open)
                .requiredRoles(Set.of(workerRole, adminRole)).build());

        Task t4 = save(Task.builder()
                .title("Write Unit Tests")
                .description("Cover all service-layer methods with JUnit 5")
                .durationHours(5).deadline(now.plusDays(7))
                .priority(medium).status(open)
                .requiredRoles(Set.of(workerRole)).build());

        Task t5 = save(Task.builder()
                .title("Build REST API — Tasks")
                .description("CRUD endpoints for task management")
                .durationHours(6).deadline(now.plusDays(5))
                .priority(high).status(open)
                .requiredRoles(Set.of(workerRole)).build());

        Task t6 = save(Task.builder()
                .title("Build REST API — Users")
                .description("CRUD endpoints for user management")
                .durationHours(4).deadline(now.plusDays(4))
                .priority(high).status(open)
                .requiredRoles(Set.of(workerRole)).build());

        Task t7 = save(Task.builder()
                .title("Frontend — Login Page")
                .description("React login form with JWT storage")
                .durationHours(3).deadline(now.plusDays(6))
                .priority(medium).status(open)
                .requiredRoles(Set.of(workerRole)).build());

        Task t8 = save(Task.builder()
                .title("Frontend — Dashboard")
                .description("Main dashboard with task and user summaries")
                .durationHours(5).deadline(now.plusDays(8))
                .priority(low).status(open)
                .requiredRoles(Set.of(workerRole)).build());

        Task t9 = save(Task.builder()
                .title("Deploy to Staging")
                .description("Deploy latest build to staging environment")
                .durationHours(3).deadline(now.plusDays(10))
                .priority(high).status(open)
                .requiredRoles(Set.of(adminRole)).build());

        Task t10 = save(Task.builder()
                .title("Code Review & QA")
                .description("Full code review and manual QA pass")
                .durationHours(4).deadline(now.plusDays(9))
                .priority(medium).status(open)
                .requiredRoles(Set.of(workerRole, adminRole)).build());

        log.info("Seeded {} tasks", taskRepository.count());

        // Constraints (Finish-to-Start dependencies)
        ConstraintType fts = constraintTypeRepository
                .findByName(ConstraintTypeConstants.FINISH_TO_START).orElseThrow();

        // t3 (Auth) depends on t1 (DB Schema) — can't implement auth without the DB
        addConstraint(t1, t3, fts);
        // t5 (API Tasks) depends on t1 (DB Schema)
        addConstraint(t1, t5, fts);
        // t7 (Login Page) depends on t3 (Auth)
        addConstraint(t3, t7, fts);
        // t8 (Dashboard) depends on t7 (Login Page)
        addConstraint(t7, t8, fts);
        // t9 (Deploy) depends on t10 (QA)
        addConstraint(t10, t9, fts);
        // t4 (Unit Tests) depends on t5 (API Tasks) and t6 (API Users)
        addConstraint(t5, t4, fts);
        addConstraint(t6, t4, fts);

        log.info("Seeded task constraints");
    }

    private Task save(Task task) {
        return taskRepository.save(task);
    }

    private void addConstraint(Task predecessor, Task successor, ConstraintType type) {
        TaskConstraint tc = TaskConstraint.builder()
                .predecessorTask(predecessor)
                .successorTask(successor)
                .constraintType(type)
                .build();
        taskConstraintRepository.save(tc);
    }

    // -------------------------------------------------------------------------
    // Vacations
    // -------------------------------------------------------------------------

    @Transactional
    public void seedVacations() {
        if (vacationRepository.count() > 0) {
            log.info("Vacations already exist — skipping vacation seed");
            return;
        }

        VacationStatus approved = vacationStatusRepository.findByName(VacationStatusConstants.APPROVED).orElseThrow();
        VacationStatus pending  = vacationStatusRepository.findByName(VacationStatusConstants.PENDING).orElseThrow();

        // Alice is on approved vacation for the next 4 days — algorithm must skip her
        userRepository.findByNationalId("alice").ifPresent(alice -> {
            vacationRepository.save(Vacation.builder()
                    .worker(alice)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusDays(4))
                    .status(approved)
                    .build());
            log.info("Seeded approved vacation for Alice ({} → {})",
                    LocalDate.now(), LocalDate.now().plusDays(4));
        });

        // Bob has a pending vacation request — algorithm ignores PENDING vacations
        userRepository.findByNationalId("bob").ifPresent(bob -> {
            vacationRepository.save(Vacation.builder()
                    .worker(bob)
                    .startDate(LocalDate.now().plusDays(6))
                    .endDate(LocalDate.now().plusDays(10))
                    .status(pending)
                    .build());
            log.info("Seeded pending vacation request for Bob");
        });
    }

    // -------------------------------------------------------------------------
    // Settlements — single source of truth for worker → task assignments
    // -------------------------------------------------------------------------

    @Transactional
    public void seedSettlements() {
        if (settlementRepository.count() > 0) {
            log.info("Settlements already exist — skipping settlement seed");
            return;
        }

        SettlementStatus pending   = settlementStatusRepository
                .findByName(TaskStatusConstants.SETTLEMENT_PENDING).orElseThrow();
        SettlementStatus completed = settlementStatusRepository
                .findByName(TaskStatusConstants.SETTLEMENT_COMPLETED).orElseThrow();
        TaskStatus locked    = taskStatusRepository
                .findByName(TaskStatusConstants.TASK_LOCKED).orElseThrow();
        TaskStatus closed    = taskStatusRepository
                .findByName(TaskStatusConstants.TASK_CLOSED).orElseThrow();

        // First task → assigned to john (PENDING settlement, task LOCKED)
        taskRepository.findAll().stream().findFirst().ifPresent(firstTask ->
            userRepository.findByNationalId("worker").ifPresent(john -> {
                firstTask.setStatus(locked);
                taskRepository.save(firstTask);
                settlementRepository.save(Settlement.builder()
                        .task(firstTask).worker(john)
                        .status(pending)
                        .settlementDate(LocalDateTime.now())
                        .completionDate(null)
                        .build());
                log.info("Seeded settlement (PENDING/LOCKED): '{}' → {}", firstTask.getTitle(), john.getFirstName());
            })
        );

        // Second task → assigned to carol (COMPLETED settlement, task CLOSED)
        taskRepository.findAll().stream().skip(1).findFirst().ifPresent(secondTask ->
            userRepository.findByNationalId("carol").ifPresent(carol -> {
                secondTask.setStatus(closed);
                taskRepository.save(secondTask);
                settlementRepository.save(Settlement.builder()
                        .task(secondTask).worker(carol)
                        .status(completed)
                        .settlementDate(LocalDateTime.now().minusDays(2))
                        .completionDate(LocalDateTime.now().minusDays(1))
                        .build());
                log.info("Seeded settlement (COMPLETED/CLOSED): '{}' → {}", secondTask.getTitle(), carol.getFirstName());
            })
        );

        log.info("Seeded {} settlements", settlementRepository.count());
    }
}

