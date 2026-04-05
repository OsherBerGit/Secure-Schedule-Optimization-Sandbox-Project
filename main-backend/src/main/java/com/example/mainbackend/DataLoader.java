package com.example.mainbackend;
import com.example.mainbackend.constants.ConstraintTypeLevel;
import com.example.mainbackend.constants.TaskPriorityLevel;
import com.example.mainbackend.constants.TaskStatusLevel;
import com.example.mainbackend.constants.RoleType;
import com.example.mainbackend.constants.SettlementStatusLevel;
import com.example.mainbackend.constants.VacationStatusLevel;
import com.example.mainbackend.entity.*;
import com.example.mainbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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
    private final TaskPriorityRepository taskPriorityRepository;
    private final ConstraintTypeRepository constraintTypeRepository;
    private final TaskRepository taskRepository;
    private final TaskConstraintRepository taskConstraintRepository;
    private final VacationRepository vacationRepository;
    private final SettlementRepository settlementRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final SkillRepository skillRepository;

    @Override
    public void run(String... args) {
        seedTaskStatuses();
        seedSettlementStatuses();
        seedVacationStatuses();
        seedPriorities();
        seedConstraintTypes();
        seedDepartments();
        seedSkills();
        seedRoles();
        seedUsers();
        seedTasks();
        seedVacations();
        seedSettlements();
    }

    @Transactional
    public void seedDepartments() {
        if (departmentRepository.findByName("General").isEmpty()) {
            departmentRepository.save(Department.builder().name("General").build());
            log.info("Seeded department: General");
        }
    }

    @Transactional
    public void seedSkills() {
        if (skillRepository.findByName("Software Engineer").isEmpty()) {
            skillRepository.save(Skill.builder().name("Software Engineer").build());
            log.info("Seeded skill: Software Engineer");
        }
        if (skillRepository.findByName("QA Engineer").isEmpty()) {
            skillRepository.save(Skill.builder().name("QA Engineer").build());
            log.info("Seeded skill: QA Engineer");
        }
        if (skillRepository.findByName("DevOps Engineer").isEmpty()) {
            skillRepository.save(Skill.builder().name("DevOps Engineer").build());
            log.info("Seeded skill: DevOps Engineer");
        }
        if (skillRepository.findByName("Java").isEmpty()) {
            skillRepository.save(Skill.builder().name("Java").build());
            log.info("Seeded skill: Java");
        }
        if (skillRepository.findByName("React").isEmpty()) {
            skillRepository.save(Skill.builder().name("React").build());
            log.info("Seeded skill: React");
        }
        if (skillRepository.findByName("DevOps").isEmpty()) {
            skillRepository.save(Skill.builder().name("DevOps").build());
            log.info("Seeded skill: DevOps");
        }
    }

    @Transactional
    public void seedRoles() {
        if (roleRepository.findByRoleName(RoleType.ADMIN.name()).isEmpty()) {
            roleRepository.save(new Role(null, RoleType.ADMIN.name()));
            log.info("Seeded role: ADMIN");
        }
        if (roleRepository.findByRoleName(RoleType.MANAGER.name()).isEmpty()) {
            roleRepository.save(new Role(null, RoleType.MANAGER.name()));
            log.info("Seeded role: MANAGER");
        }
        if (roleRepository.findByRoleName(RoleType.WORKER.name()).isEmpty()) {
            roleRepository.save(new Role(null, RoleType.WORKER.name()));
            log.info("Seeded role: WORKER");
        }
    }

    // ---
    // Lookup tables
    // ---
    @Transactional
    public void seedTaskStatuses() {
        seedTaskStatus(TaskStatusLevel.OPEN.name(),      "#3B82F6");
        seedTaskStatus(TaskStatusLevel.LOCKED.name(),    "#F59E0B");
        seedTaskStatus(TaskStatusLevel.SCHEDULED.name(), "#A855F7");
        seedTaskStatus(TaskStatusLevel.CLOSED.name(),    "#10B981");
    }

    private void seedTaskStatus(String name, String colorCode) {
        if (taskStatusRepository.findByName(name).isEmpty()) {
            taskStatusRepository.save(TaskStatus.builder().name(name).colorCode(colorCode).build());
            log.info("Seeded task status: {}", name);
        }
    }

    @Transactional
    public void seedSettlementStatuses() {
        seedSettlementStatus(SettlementStatusLevel.PENDING.name(),     "#6B7280");
        seedSettlementStatus(SettlementStatusLevel.ASSIGNED.name(),    "#3B82F6");
        seedSettlementStatus(SettlementStatusLevel.IN_PROGRESS.name(), "#8B5CF6");
        seedSettlementStatus(SettlementStatusLevel.COMPLETED.name(),   "#10B981");
        seedSettlementStatus(SettlementStatusLevel.FAILED.name(),      "#EF4444");
    }

    private void seedSettlementStatus(String name, String colorCode) {
        if (settlementStatusRepository.findByName(name).isEmpty()) {
            settlementStatusRepository.save(SettlementStatus.builder().name(name).colorCode(colorCode).build());
            log.info("Seeded settlement status: {}", name);
        }
    }

    @Transactional
    public void seedVacationStatuses() {
        for (VacationStatusLevel level : VacationStatusLevel.values()) {
            if (vacationStatusRepository.findByName(level.name()).isEmpty()) {
                vacationStatusRepository.save(VacationStatus.builder()
                        .name(level.name())
                        .build());
                log.info("Seeded vacation status: {}", level.name());
            }
        }
    }

    @Transactional
    public void seedPriorities() {
        for (TaskPriorityLevel priorityEnum : TaskPriorityLevel.values()) {
            String name = priorityEnum.name();
            int value = priorityEnum.getWeight();
            if (taskPriorityRepository.findByName(name).isEmpty()) {
                taskPriorityRepository.save(TaskPriority.builder()
                        .name(name)
                        .value(value)
                        .build());
                log.info("Seeded priority: {} (value={})", name, value);
            }
        }
    }

    @Transactional
    public void seedConstraintTypes() {
        String[] descriptions = {
            "Successor cannot start until predecessor finishes",
            "Successor cannot start until predecessor starts",
            "Successor cannot finish until predecessor finishes",
            "Successor cannot finish until predecessor starts"
        };
        for (ConstraintTypeLevel level : ConstraintTypeLevel.values()) {
            if (constraintTypeRepository.findByName(level.name()).isEmpty()) {
                constraintTypeRepository.save(ConstraintType.builder()
                        .name(level.name())
                        .description(level.getDescription())
                        .build());
                log.info("Seeded constraint type: {}", level.name());
            }
        }
    }

    // ---
    // Roles + 15 Workers with real shift schedules
    // ---
    @Transactional
    public void seedUsers() {
        Role adminRole  = roleRepository.findByRoleName(RoleType.ADMIN.name()).orElseThrow();
        Role managerRole = roleRepository.findByRoleName(RoleType.MANAGER.name()).orElseThrow();
        Role workerRole = roleRepository.findByRoleName(RoleType.WORKER.name()).orElseThrow();
        Department generalDepartment = departmentRepository.findByName("General").orElseThrow();
        Skill softwareEngineer = skillRepository.findByName("Software Engineer").orElseThrow();
        Skill qaEngineer = skillRepository.findByName("QA Engineer").orElseThrow();
        Skill devopsEngineer = skillRepository.findByName("DevOps Engineer").orElseThrow();
        Skill java = skillRepository.findByName("Java").orElseThrow();
        Skill react = skillRepository.findByName("React").orElseThrow();
        Skill devOps = skillRepository.findByName("DevOps").orElseThrow();

        // - Generic Users ---
        upsertUser("admin", "admin", "admin", "admin@company.com", 15,
                adminRole,
                shifts(
                        shift(DayOfWeek.SUNDAY,    "09:00", "17:00"),
                        shift(DayOfWeek.MONDAY,    "09:00", "17:00"),
                        shift(DayOfWeek.TUESDAY,   "09:00", "17:00"),
                        shift(DayOfWeek.WEDNESDAY, "09:00", "17:00"),
                        shift(DayOfWeek.THURSDAY,  "09:00", "17:00")
                ), generalDepartment, Set.of(softwareEngineer, qaEngineer, devopsEngineer));

        upsertUser("manager", "manager", "manager", "manager@company.com", 10,
                managerRole,
                shifts(
                        shift(DayOfWeek.SUNDAY,    "09:00", "17:00"),
                        shift(DayOfWeek.MONDAY,    "09:00", "17:00"),
                        shift(DayOfWeek.TUESDAY,   "09:00", "17:00"),
                        shift(DayOfWeek.WEDNESDAY, "09:00", "17:00"),
                        shift(DayOfWeek.THURSDAY,  "09:00", "17:00")
                ), generalDepartment, Set.of(softwareEngineer, qaEngineer));

        upsertUser("user", "user", "user", "user@company.com", 5,
                workerRole,
                shifts(
                        shift(DayOfWeek.SUNDAY,    "08:00", "16:00"),
                        shift(DayOfWeek.MONDAY,    "08:00", "16:00"),
                        shift(DayOfWeek.TUESDAY,   "08:00", "16:00"),
                        shift(DayOfWeek.WEDNESDAY, "08:00", "16:00"),
                        shift(DayOfWeek.THURSDAY,  "08:00", "16:00")
                ), generalDepartment, Set.of(softwareEngineer));

        // - Admin ---
        upsertUser("admin2", "Admin", "User", "admin2@company.com", 15,
                adminRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "09:00", "17:00"),
                    shift(DayOfWeek.MONDAY,    "09:00", "17:00"),
                    shift(DayOfWeek.TUESDAY,   "09:00", "17:00"),
                    shift(DayOfWeek.WEDNESDAY, "09:00", "17:00"),
                    shift(DayOfWeek.THURSDAY,  "09:00", "17:00")
                ), generalDepartment, Set.of(softwareEngineer, qaEngineer, devopsEngineer));

        // - Workers ---
        upsertUser("john", "John", "Doe", "john@company.com", 5,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "08:00", "16:00"),
                    shift(DayOfWeek.MONDAY,    "08:00", "16:00"),
                    shift(DayOfWeek.TUESDAY,   "08:00", "16:00"),
                    shift(DayOfWeek.WEDNESDAY, "08:00", "16:00"),
                    shift(DayOfWeek.THURSDAY,  "08:00", "16:00")
                ), generalDepartment, Set.of(softwareEngineer));

        upsertUser("alice", "Alice", "Smith", "alice@company.com", 4,
                workerRole,
                shifts(
                    shift(DayOfWeek.MONDAY,    "09:00", "17:00"),
                    shift(DayOfWeek.WEDNESDAY, "09:00", "17:00"),
                    shift(DayOfWeek.THURSDAY,  "09:00", "17:00"),
                    shift(DayOfWeek.FRIDAY,    "09:00", "13:00")
                ), generalDepartment, Set.of(qaEngineer));

        upsertUser("bob", "Bob", "Johnson", "bob@company.com", 3,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "07:00", "15:00"),
                    shift(DayOfWeek.MONDAY,    "07:00", "15:00"),
                    shift(DayOfWeek.TUESDAY,   "07:00", "15:00"),
                    shift(DayOfWeek.THURSDAY,  "07:00", "15:00")
                ), generalDepartment, Set.of(devopsEngineer));

        upsertUser("carol", "Carol", "Williams", "carol@company.com", 4,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "10:00", "18:00"),
                    shift(DayOfWeek.MONDAY,    "10:00", "18:00"),
                    shift(DayOfWeek.TUESDAY,   "10:00", "18:00"),
                    shift(DayOfWeek.WEDNESDAY, "10:00", "18:00"),
                    shift(DayOfWeek.THURSDAY,  "10:00", "18:00")
                ), generalDepartment, Set.of(softwareEngineer));

        upsertUser("david", "David", "Brown", "david@company.com", 6,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "08:00", "17:00"),
                    shift(DayOfWeek.MONDAY,    "08:00", "17:00"),
                    shift(DayOfWeek.TUESDAY,   "08:00", "17:00"),
                    shift(DayOfWeek.WEDNESDAY, "08:00", "17:00"),
                    shift(DayOfWeek.THURSDAY,  "08:00", "17:00"),
                    shift(DayOfWeek.FRIDAY,    "08:00", "12:00")
                ), generalDepartment, Set.of(qaEngineer));

        upsertUser("emma", "Emma", "Davis", "emma@company.com", 5,
                workerRole,
                shifts(
                    shift(DayOfWeek.MONDAY,    "09:00", "18:00"),
                    shift(DayOfWeek.TUESDAY,   "09:00", "18:00"),
                    shift(DayOfWeek.WEDNESDAY, "09:00", "18:00"),
                    shift(DayOfWeek.THURSDAY,  "09:00", "18:00")
                ), generalDepartment, Set.of(devopsEngineer));

        upsertUser("frank", "Frank", "Miller", "frank@company.com", 4,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "06:00", "14:00"),
                    shift(DayOfWeek.MONDAY,    "06:00", "14:00"),
                    shift(DayOfWeek.TUESDAY,   "06:00", "14:00"),
                    shift(DayOfWeek.WEDNESDAY, "06:00", "14:00")
                ), generalDepartment, Set.of(softwareEngineer));

        upsertUser("grace", "Grace", "Wilson", "grace@company.com", 5,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "09:00", "17:00"),
                    shift(DayOfWeek.TUESDAY,   "09:00", "17:00"),
                    shift(DayOfWeek.WEDNESDAY, "09:00", "17:00"),
                    shift(DayOfWeek.THURSDAY,  "09:00", "17:00"),
                    shift(DayOfWeek.FRIDAY,    "09:00", "17:00")
                ), generalDepartment, Set.of(qaEngineer));

        upsertUser("henry", "Henry", "Moore", "henry@company.com", 3,
                workerRole,
                shifts(
                    shift(DayOfWeek.MONDAY,    "11:00", "19:00"),
                    shift(DayOfWeek.TUESDAY,   "11:00", "19:00"),
                    shift(DayOfWeek.WEDNESDAY, "11:00", "19:00"),
                    shift(DayOfWeek.THURSDAY,  "11:00", "19:00"),
                    shift(DayOfWeek.FRIDAY,    "11:00", "19:00")
                ), generalDepartment, Set.of(devopsEngineer));

        upsertUser("iris", "Iris", "Taylor", "iris@company.com", 5,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "08:00", "16:00"),
                    shift(DayOfWeek.MONDAY,    "08:00", "16:00"),
                    shift(DayOfWeek.WEDNESDAY, "08:00", "16:00"),
                    shift(DayOfWeek.THURSDAY,  "08:00", "16:00")
                ), generalDepartment, Set.of(softwareEngineer));

        upsertUser("jack", "Jack", "Anderson", "jack@company.com", 6,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "09:00", "18:00"),
                    shift(DayOfWeek.MONDAY,    "09:00", "18:00"),
                    shift(DayOfWeek.TUESDAY,   "09:00", "18:00"),
                    shift(DayOfWeek.WEDNESDAY, "09:00", "18:00"),
                    shift(DayOfWeek.THURSDAY,  "09:00", "18:00"),
                    shift(DayOfWeek.FRIDAY,    "09:00", "13:00")
                ), generalDepartment, Set.of(qaEngineer));

        upsertUser("karen", "Karen", "Thomas", "karen@company.com", 4,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "10:00", "16:00"),
                    shift(DayOfWeek.MONDAY,    "10:00", "16:00"),
                    shift(DayOfWeek.TUESDAY,   "10:00", "16:00"),
                    shift(DayOfWeek.WEDNESDAY, "10:00", "16:00"),
                    shift(DayOfWeek.THURSDAY,  "10:00", "16:00")
                ), generalDepartment, Set.of(devopsEngineer));

        upsertUser("liam", "Liam", "Jackson", "liam@company.com", 5,
                workerRole,
                shifts(
                    shift(DayOfWeek.MONDAY,    "08:00", "17:00"),
                    shift(DayOfWeek.TUESDAY,   "08:00", "17:00"),
                    shift(DayOfWeek.WEDNESDAY, "08:00", "17:00"),
                    shift(DayOfWeek.THURSDAY,  "08:00", "17:00"),
                    shift(DayOfWeek.FRIDAY,    "08:00", "17:00")
                ), generalDepartment, Set.of(softwareEngineer));

        upsertUser("mia", "Mia", "White", "mia@company.com", 4,
                workerRole,
                shifts(
                    shift(DayOfWeek.SUNDAY,    "07:00", "15:00"),
                    shift(DayOfWeek.MONDAY,    "07:00", "15:00"),
                    shift(DayOfWeek.TUESDAY,   "07:00", "15:00"),
                    shift(DayOfWeek.THURSDAY,  "07:00", "15:00"),
                    shift(DayOfWeek.FRIDAY,    "07:00", "11:00")
                ), generalDepartment, Set.of(qaEngineer));

        // The Specialist (Super User with all 3 skills)
        upsertUser("super1", "Clark", "Kent", "specialist@company.com", 5,
                workerRole,
                shifts(
                    shift(DayOfWeek.MONDAY,    "09:00", "17:00"),
                    shift(DayOfWeek.TUESDAY,   "09:00", "17:00"),
                    shift(DayOfWeek.WEDNESDAY, "09:00", "17:00"),
                    shift(DayOfWeek.THURSDAY,  "09:00", "17:00"),
                    shift(DayOfWeek.FRIDAY,    "09:00", "17:00")
                ), generalDepartment, Set.of(java, react, devOps));

        // The Juniors (Only 1 or 2 skills)
        upsertUser("junior1", "Jimmy", "Olsen", "jimmy@company.com", 5,
                workerRole,
                shifts(
                    shift(DayOfWeek.MONDAY,    "09:00", "17:00"),
                    shift(DayOfWeek.TUESDAY,   "09:00", "17:00"),
                    shift(DayOfWeek.WEDNESDAY, "09:00", "17:00")
                ), generalDepartment, Set.of(java));

        upsertUser("junior2", "Lois", "Lane", "lois@company.com", 5,
                workerRole,
                shifts(
                    shift(DayOfWeek.MONDAY,    "09:00", "17:00"),
                    shift(DayOfWeek.TUESDAY,   "09:00", "17:00"),
                    shift(DayOfWeek.WEDNESDAY, "09:00", "17:00")
                ), generalDepartment, Set.of(react, devOps));

        log.info("Seeded {} users", userRepository.count());
    }

    // ---
    // 30 OPEN tasks with varied priorities, durations, and rich descriptions
    // ---
    @Transactional
    public void seedTasks() {
        if (taskRepository.count() > 0) {
            log.info("Tasks already seeded - skipping");
            return;
        }

        TaskPriority low      = taskPriorityRepository.findByName(TaskPriorityLevel.LOW.name()).orElseThrow();
        TaskPriority medium   = taskPriorityRepository.findByName(TaskPriorityLevel.MEDIUM.name()).orElseThrow();
        TaskPriority high     = taskPriorityRepository.findByName(TaskPriorityLevel.HIGH.name()).orElseThrow();
        TaskPriority critical = taskPriorityRepository.findByName(TaskPriorityLevel.CRITICAL.name()).orElseThrow();
        TaskStatus open = taskStatusRepository.findByName(TaskStatusLevel.OPEN.name()).orElseThrow();
        Department generalDepartment = departmentRepository.findByName("General").orElseThrow();
        Skill softwareEngineer = skillRepository.findByName("Software Engineer").orElseThrow();
        Skill qaEngineer = skillRepository.findByName("QA Engineer").orElseThrow();
        Skill devopsEngineer = skillRepository.findByName("DevOps Engineer").orElseThrow();
        Skill java = skillRepository.findByName("Java").orElseThrow();
        Skill react = skillRepository.findByName("React").orElseThrow();
        Skill devOps = skillRepository.findByName("DevOps").orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        // - Infrastructure & DevOps ---
        Task t01 = save(Task.builder().title("Design Database Schema")
                .description("Design the full relational DB schema including all entities, relationships, and indexes.")
                .durationHours(4).deadline(now.plusDays(3)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t02 = save(Task.builder().title("Setup CI/CD Pipeline")
                .description("Configure GitHub Actions workflows for build, test, and deploy stages.")
                .durationHours(6).deadline(now.plusDays(5)).priority(medium).status(open)
                .requiredSkills(Set.of(devopsEngineer)).department(generalDepartment).build());

        Task t03 = save(Task.builder().title("Provision Production Server")
                .description("Provision and harden the AWS EC2 production server with security groups and IAM roles.")
                .durationHours(5).deadline(now.plusDays(6)).priority(high).status(open)
                .requiredSkills(Set.of(devopsEngineer)).department(generalDepartment).build());

        Task t04 = save(Task.builder().title("Configure NGINX Reverse Proxy")
                .description("Set up NGINX as a reverse proxy in front of the Spring Boot app with SSL termination.")
                .durationHours(3).deadline(now.plusDays(7)).priority(medium).status(open)
                .requiredSkills(Set.of(devopsEngineer)).department(generalDepartment).build());

        Task t05 = save(Task.builder().title("Setup Docker Compose Environment")
                .description("Create docker-compose.yml for local dev with MySQL, Redis, and backend services.")
                .durationHours(4).deadline(now.plusDays(4)).priority(medium).status(open)
                .requiredSkills(Set.of(devopsEngineer)).department(generalDepartment).build());

        // - Backend - Auth & Security ---
        Task t06 = save(Task.builder().title("Implement JWT Authentication")
                .description("Build JWT-based login with access/refresh tokens, blacklisting, and role guards.")
                .durationHours(8).deadline(now.plusDays(4)).priority(critical).status(open)
                .requiredSkills(Set.of(softwareEngineer, devopsEngineer)).department(generalDepartment).build());

        Task t07 = save(Task.builder().title("Add Role-Based Access Control")
                .description("Wire Spring Security method-level @PreAuthorize annotations for ADMIN/WORKER separation.")
                .durationHours(4).deadline(now.plusDays(5)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t08 = save(Task.builder().title("Implement Password Reset Flow")
                .description("Email-based OTP password reset with expiry and rate limiting.")
                .durationHours(5).deadline(now.plusDays(8)).priority(medium).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        // - Backend - Core APIs ---
        Task t09 = save(Task.builder().title("Build Tasks REST API")
                .description("CRUD endpoints for task management with pagination, filtering, and status transitions.")
                .durationHours(6).deadline(now.plusDays(5)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t10 = save(Task.builder().title("Build Users REST API")
                .description("CRUD endpoints for user management including role assignment and availability windows.")
                .durationHours(5).deadline(now.plusDays(5)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t11 = save(Task.builder().title("Build Settlements REST API")
                .description("Endpoints for creating, listing, and completing worker-task settlements.")
                .durationHours(5).deadline(now.plusDays(6)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t12 = save(Task.builder().title("Build Vacations REST API")
                .description("Endpoints for vacation request, approval/rejection workflow, and date-range queries.")
                .durationHours(4).deadline(now.plusDays(6)).priority(medium).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t13 = save(Task.builder().title("Build Schedule Run Endpoint")
                .description("POST /api/schedule/run - calls the algorithm service and returns the draft preview.")
                .durationHours(6).deadline(now.plusDays(7)).priority(critical).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t14 = save(Task.builder().title("Build Schedule Save Endpoint")
                .description("POST /api/schedule/save - persists approved assignments to the DB (SCHEDULED + ASSIGNED).")
                .durationHours(4).deadline(now.plusDays(7)).priority(critical).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t15 = save(Task.builder().title("Integrate Priorities & Statuses Lookup APIs")
                .description("Seed endpoints for priorities and task/settlement status lookup tables.")
                .durationHours(3).deadline(now.plusDays(4)).priority(low).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        // - Algorithm Service ---
        Task t16 = save(Task.builder().title("Implement Greedy Scheduling Strategy")
                .description("Assign tasks in priority order to the first available worker respecting role and availability.")
                .durationHours(8).deadline(now.plusDays(6)).priority(critical).status(open)
                .requiredSkills(Set.of(softwareEngineer, qaEngineer))
                .department(generalDepartment).build());

        Task t17 = save(Task.builder().title("Implement Round-Robin Scheduling Strategy")
                .description("Distribute tasks evenly across eligible workers in a round-robin fashion.")
                .durationHours(6).deadline(now.plusDays(7)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t18 = save(Task.builder().title("Implement Memetic Algorithm Strategy")
                .description("Genetic + local-search hybrid for near-optimal scheduling across large task sets.")
                .durationHours(16).deadline(now.plusDays(12)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer, qaEngineer, devopsEngineer)).department(generalDepartment).build());

        Task t19 = save(Task.builder().title("Add Constraint Validation to Algorithm")
                .description("Enforce Finish-to-Start / Start-to-Start lag constraints during candidate generation.")
                .durationHours(6).deadline(now.plusDays(8)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t20 = save(Task.builder().title("Add Vacation Blocking to Algorithm")
                .description("Exclude workers who are on approved vacation from candidate assignment windows.")
                .durationHours(4).deadline(now.plusDays(7)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        // - Frontend ---
        Task t21 = save(Task.builder().title("Frontend - Login & Auth Pages")
                .description("React login form with JWT storage, auto-refresh, and redirect logic.")
                .durationHours(5).deadline(now.plusDays(6)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t22 = save(Task.builder().title("Frontend - Dashboard Page")
                .description("Summary cards for tasks, workers, open settlements, and scheduling stats.")
                .durationHours(5).deadline(now.plusDays(8)).priority(medium).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t23 = save(Task.builder().title("Frontend - Tasks Management Page")
                .description("Table with create/edit/delete modals, status badges, and priority filters.")
                .durationHours(6).deadline(now.plusDays(8)).priority(medium).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t24 = save(Task.builder().title("Frontend - Schedule Page with Gantt Chart")
                .description("Gantt-style schedule view with draft/approve flow and Approve & Save button.")
                .durationHours(8).deadline(now.plusDays(10)).priority(high).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t25 = save(Task.builder().title("Frontend - Settlements Page")
                .description("List worker-task assignments with status tracking and completion action.")
                .durationHours(4).deadline(now.plusDays(9)).priority(medium).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t26 = save(Task.builder().title("Frontend - Vacations Page")
                .description("Vacation request form for workers and approve/reject UI for admins.")
                .durationHours(4).deadline(now.plusDays(9)).priority(low).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        Task t27 = save(Task.builder().title("Frontend - Users Management Page")
                .description("Admin-only table to view, create, edit, and delete users with role badges.")
                .durationHours(4).deadline(now.plusDays(7)).priority(medium).status(open)
                .requiredSkills(Set.of(softwareEngineer)).department(generalDepartment).build());

        // - QA & Delivery ---
        Task t28 = save(Task.builder().title("Write Integration Tests - Backend")
                .description("Spring Boot @SpringBootTest coverage for all REST endpoints.")
                .durationHours(8).deadline(now.plusDays(11)).priority(medium).status(open)
                .requiredSkills(Set.of(qaEngineer)).department(generalDepartment).build());

        Task t29 = save(Task.builder().title("Deploy to Staging & Smoke Test")
                .description("Deploy the full stack to staging and run a smoke test checklist.")
                .durationHours(4).deadline(now.plusDays(12)).priority(high).status(open)
                .requiredSkills(Set.of(qaEngineer)).department(generalDepartment).build());

        Task t30 = save(Task.builder().title("Production Go-Live & Monitoring Setup")
                .description("Deploy to production, configure Grafana/Prometheus dashboards, set up alerting.")
                .durationHours(6).deadline(now.plusDays(14)).priority(critical).status(open)
                .requiredSkills(Set.of(devopsEngineer, qaEngineer, softwareEngineer)).department(generalDepartment).build());

        // The Super Task (Needs all 3 skills, Critical priority)
        Task superTask = save(Task.builder().title("Build Microservice Platform")
                .description("STRESS TEST: High-priority task requiring full-stack and devops expertise.")
                .durationHours(8).deadline(now.plusDays(2)).priority(critical).status(open)
                .requiredSkills(Set.of(java, react, devOps))
                .department(generalDepartment).build());

        // The Trap (Simple task, Low priority, same deadline. Solver should assign to Jimmy, keeping Clark free for the Super Task)
        Task trapTask = save(Task.builder().title("Simple Java Bugfix")
                .description("STRESS TEST: Low-priority bugfix. Needs Java.")
                .durationHours(4).deadline(now.plusDays(2)).priority(low).status(open)
                .requiredSkills(Set.of(java))
                .department(generalDepartment).build());

        log.info("Seeded {} tasks", taskRepository.count());

        // - Finish-to-Start Constraints ---
        ConstraintType fts = constraintTypeRepository
                .findByName(ConstraintTypeLevel.FINISH_TO_START.name()).orElseThrow();

        // DB Schema must finish before: Auth API, Tasks API, Users API, Settlements API
        addConstraint(t01, t06, fts);
        addConstraint(t01, t09, fts);
        addConstraint(t01, t10, fts);
        addConstraint(t01, t11, fts);

        // CI/CD Pipeline before Deploy to Staging
        addConstraint(t02, t29, fts);

        // Server provisioned before NGINX, before Docker
        addConstraint(t03, t04, fts);
        addConstraint(t03, t05, fts);

        // Auth before RBAC, before Password Reset, before Schedule APIs
        addConstraint(t06, t07, fts);
        addConstraint(t06, t08, fts);
        addConstraint(t06, t13, fts);

        // Tasks API + Users API before Settlements API
        addConstraint(t09, t11, fts);
        addConstraint(t10, t11, fts);

        // Schedule Run before Schedule Save
        addConstraint(t13, t14, fts);

        // Greedy strategy before Round-Robin and Memetic
        addConstraint(t16, t17, fts);
        addConstraint(t16, t18, fts);

        // Constraint validation and vacation blocking depend on Greedy base
        addConstraint(t16, t19, fts);
        addConstraint(t16, t20, fts);

        // Login page before Dashboard, then rest of frontend
        addConstraint(t21, t22, fts);
        addConstraint(t21, t23, fts);
        addConstraint(t21, t24, fts);
        addConstraint(t21, t25, fts);
        addConstraint(t21, t26, fts);
        addConstraint(t21, t27, fts);

        // Integration tests depend on all core APIs being built
        addConstraint(t09, t28, fts);
        addConstraint(t10, t28, fts);
        addConstraint(t11, t28, fts);
        addConstraint(t13, t28, fts);

        // Staging deploy depends on integration tests + Docker setup + NGINX + Schedule Save
        addConstraint(t28, t29, fts);
        addConstraint(t04, t29, fts);
        addConstraint(t05, t29, fts);
        addConstraint(t14, t29, fts);

        // Production go-live depends on staging passing
        addConstraint(t29, t30, fts);

        log.info("Seeded task constraints");
    }

    // ---
    // Vacations - create realistic blocked windows for the algorithm
    // ---
    @Transactional
    public void seedVacations() {
        if (vacationRepository.count() > 0) {
            log.info("Vacations already seeded - skipping");
            return;
        }

        VacationStatus approved = vacationStatusRepository.findByName(VacationStatusLevel.APPROVED.name()).orElseThrow();
        VacationStatus pending  = vacationStatusRepository.findByName(VacationStatusLevel.PENDING.name()).orElseThrow();

        // Alice - approved vacation for next 4 days (algorithm must skip her entirely)
        saveVacation("alice", LocalDate.now(), LocalDate.now().plusDays(4), approved);

        // Bob - approved vacation in mid-window (partial block)
        saveVacation("bob", LocalDate.now().plusDays(3), LocalDate.now().plusDays(7), approved);

        // Frank - pending request (algorithm ignores PENDING)
        saveVacation("frank", LocalDate.now().plusDays(5), LocalDate.now().plusDays(9), pending);

        // Karen - approved short vacation
        saveVacation("karen", LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), approved);

        // Henry - pending request overlapping with many deadlines
        saveVacation("henry", LocalDate.now().plusDays(2), LocalDate.now().plusDays(6), pending);

        log.info("Seeded {} vacations", vacationRepository.count());
    }

    private void saveVacation(String nationalId, LocalDate start, LocalDate end, VacationStatus status) {
        userRepository.findByNationalId(nationalId).ifPresent(user -> {
            vacationRepository.save(Vacation.builder()
                    .worker(user).startDate(start).endDate(end).status(status).build());
            log.info("Seeded {} vacation for {} ({} - {})", status.getName(), nationalId, start, end);
        });
    }

    // ---
    // Settlements - seed 2 already-settled tasks so the UI has variety
    // ---
    @Transactional
    public void seedSettlements() {
        if (settlementRepository.count() > 0) {
            log.info("Settlements already seeded - skipping");
            return;
        }

        SettlementStatus pending   = settlementStatusRepository.findByName(SettlementStatusLevel.PENDING.name()).orElseThrow();
        SettlementStatus completed = settlementStatusRepository.findByName(SettlementStatusLevel.COMPLETED.name()).orElseThrow();
        TaskStatus locked = taskStatusRepository.findByName(TaskStatusLevel.LOCKED.name()).orElseThrow();
        TaskStatus closed = taskStatusRepository.findByName(TaskStatusLevel.CLOSED.name()).orElseThrow();
        List<Task> allTasks = taskRepository.findAll();

        if (allTasks.size() < 2) return;

        // First task - john (PENDING/LOCKED) - in-progress assignment
        userRepository.findByNationalId("john").ifPresent(john -> {
            Task t = allTasks.get(0);
            t.setStatus(locked);
            taskRepository.save(t);
            settlementRepository.save(Settlement.builder()
                    .task(t).worker(john).status(pending)
                    .settlementDate(LocalDateTime.now()).build());
            log.info("Seeded PENDING settlement: '{}' - {}", t.getTitle(), john.getFirstName());
        });

        // Second task - carol (COMPLETED/CLOSED) - finished example
        userRepository.findByNationalId("carol").ifPresent(carol -> {
            Task t = allTasks.get(1);
            t.setStatus(closed);
            taskRepository.save(t);
            settlementRepository.save(Settlement.builder()
                    .task(t).worker(carol).status(completed)
                    .settlementDate(LocalDateTime.now().minusDays(2))
                    .completionDate(LocalDateTime.now().minusDays(1)).build());
            log.info("Seeded COMPLETED settlement: '{}' - {}", t.getTitle(), carol.getFirstName());
        });
        log.info("Seeded {} settlements", settlementRepository.count());
    }

    // ---
    // Helpers
    // ---
    private void upsertUser(String nationalId, String firstName, String lastName,
                             String email, int maxTasks, Role role,
                             List<WorkerAvailability> availabilityTemplates, Department department, Set<Skill> skills) {
        User user = userRepository.findByNationalId(nationalId).orElseGet(() -> {
            User u = new User();
            u.setNationalId(nationalId);
            u.setPassword(passwordEncoder.encode(nationalId));
            log.info("Created user: {} (password={})", nationalId, nationalId);
            return u;
        });

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setMaxTasks(maxTasks);
        user.setRole(role);
        user.setDepartment(department);
        user.setSkills(skills);

        // Replace availability windows
        user.getAvailabilities().clear();
        for (WorkerAvailability av : availabilityTemplates) {
            av.setUser(user);
            user.getAvailabilities().add(av);
        }
        userRepository.save(user);
    }

    private static List<WorkerAvailability> shifts(WorkerAvailability... items) {
        return List.of(items);
    }

    private static WorkerAvailability shift(DayOfWeek day, String start, String end) {
        return WorkerAvailability.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }

    private Task save(Task task) {
        return taskRepository.save(task);
    }

    private void addConstraint(Task predecessor, Task successor, ConstraintType type) {
        taskConstraintRepository.save(TaskConstraint.builder()
                .predecessorTask(predecessor)
                .successorTask(successor)
                .constraintType(type)
                .build());
    }
}
