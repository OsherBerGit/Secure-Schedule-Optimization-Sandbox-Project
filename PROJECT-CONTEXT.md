# Secure-Schedule Project Context
**Last Updated:** 2026-02-07

---

## 1. Project Goal
Automated Task & Shift Scheduling System that assigns tasks to workers based on:
- Constraints (task dependencies)
- Priorities
- Skills (roles)
- Availability (vacations)
- Objective: Minimize idle time

---

## 2. Architecture (Microservices-Lite)

### Main Backend (Current Focus)
- **Technology:** Java 17 + Spring Boot 3
- **Responsibility:** Data Management, Authentication, API exposure
- **Port:** 8080

### Algorithm Engine (Separate Service)
- **Technology:** Java (separate Spring Boot service)
- **Responsibility:** Computational logic (Greedy/Heuristic scheduling algorithms)
- **Communication:** REST API (stateless)

### Frontend
- **Technology:** React + Vite
- **Port:** 5173 (dev server)

---

## 3. Tech Stack (Main Backend)

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3 |
| Language | Java 17 |
| Database | MySQL |
| ORM | Hibernate/JPA |
| Security | JWT (Stateless), BCrypt |
| Validation | Jakarta Validation (@Valid) |
| Tools | Lombok |
| Mapping | Manual DTOs (no ModelMapper) |

---

## 4. Database Schema (Implemented Entities)

### User Entity
```java
@Entity
public class User {
    private Long id;
    private String nationalId;  // ← UNIQUE IDENTIFIER (not username/email)
    private String password;    // BCrypt encrypted
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Double salary;
    private String address;
    private Integer dailyAvailabilityHours;
    private Integer maxTasks;
    private Set<Role> roles;           // Many-to-Many
    private List<Settlement> settlements;
    private List<Vacation> vacations;
}
```

### Role Entity (RBAC)
```java
@Entity
public class Role {
    private Long id;
    private String roleName;  // "ADMIN" or "USER"
}
```

### Task Entity
```java
@Entity
public class Task {
    private Long id;
    private String title;
    private String description;
    private Integer durationHours;
    private LocalDateTime deadline;
    private LocalDateTime startTime;
    
    private Priority priority;           // Many-to-One (Lookup Table)
    private Status status;              // Many-to-One (Lookup Table)
    private User assignedEmployee;      // Many-to-One
    private Set<Role> requiredRoles;    // Many-to-Many
    
    // Bidirectional constraints
    private List<TaskConstraint> outgoingConstraints;  // This task blocks others
    private List<TaskConstraint> incomingConstraints;  // This task is blocked by others
}
```

### TaskConstraint Entity (Dependencies)
```java
@Entity
public class TaskConstraint {
    private Long id;
    private Task predecessorTask;    // Must complete first
    private Task successorTask;      // Depends on predecessor
    private ConstraintType constraintType;  // FINISH_TO_START, etc.
    private Integer lagMinutes;      // Time buffer (default: 0)
}
```

### Lookup Tables (Dynamic Values)
- **Status:** PENDING, IN_PROGRESS, COMPLETED, CANCELLED
- **Priority:** LOW, MEDIUM, HIGH, CRITICAL (with integer values)
- **ConstraintType:** FINISH_TO_START, START_TO_START, FINISH_TO_FINISH, START_TO_FINISH

### Vacation Entity
```java
@Entity
public class Vacation {
    private Long id;
    private User worker;
    private LocalDate startDate;
    private LocalDate endDate;
}
```

### Settlement Entity (Schedule History)
```java
@Entity
public class Settlement {
    private Long id;
    private Task task;
    private User worker;
    private LocalDateTime settlementDate;
    private LocalDateTime completionDate;
}
```

### BlacklistedToken Entity (Logout)
```java
@Entity
public class BlacklistedToken {
    private String jwtId;  // Primary Key
    private Instant expirationTime;
    private Instant blacklistedAt;
    private String reason;
}
```

---

## 5. Authentication & Security

### Login Flow
- **Identifier:** `nationalId` (NOT username or email)
- **Endpoint:** `POST /api/auth/login`
- **Request:** `{ "nationalId": "123456789", "password": "..." }`
- **Response:** `{ "accessToken": "...", "refreshToken": "..." }`

### Authorization Rules
- **Public Endpoints:**
  - `POST /api/auth/login`
  - `POST /api/auth/refresh-token`
  - `GET /api/status`
  
- **ADMIN Only:**
  - `POST /api/users` (Create user - NO public registration)
  - `DELETE /api/users/**`
  - `PUT /api/users/**`
  
- **USER or ADMIN:**
  - `GET /api/users/**`
  - `GET/POST/PUT/DELETE /api/tasks/**`
  - `GET/POST /api/schedule/**`

### Security Features
- JWT tokens (stateless)
- BCrypt password hashing (strength 12)
- Token blacklist (database-persisted)
- CORS restricted to `http://localhost:5173`
- XSS protection headers
- CSRF disabled (stateless API)

---

## 6. Completed Features

### ✅ Authentication
- Login via `nationalId`
- JWT access + refresh tokens
- Token blacklist for logout
- CustomUserDetailsService

### ✅ User Management
- CRUD operations
- Admin-only creation (`@PreAuthorize`)
- Password encryption
- Role assignment

### ✅ Task Management
- CRUD operations
- Status/Priority relationships (Lookup Tables)
- Task assignment to workers

### ✅ Data Seeding
- Status values (PENDING, IN_PROGRESS, etc.)
- Priority values (LOW, MEDIUM, HIGH, CRITICAL)
- ConstraintType values
- Default admin user (`nationalId: "admin"`)

---

## 7. Coding Standards

### Layered Architecture
```
Controller → Service → Repository → Entity
     ↓          ↓
   DTO    ←  Mapper
```

### DTO Strategy
- **Separate DTOs for different operations:**
  - `CreateUserRequest` (for POST)
  - `UserDto` (for responses)
  - `TaskCreateRequest` (for POST)
  - `TaskResponseDto` (for responses)
  
- **Why:** Clean separation, different validation rules per operation

### Validation
```java
// All request DTOs must have validation
@Data
public class CreateUserRequest {
    @NotBlank(message = "National ID is required")
    private String nationalId;
    
    @Email
    private String email;
    // ...
}

// Controllers must use @Valid
@PostMapping
public ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest request) {
    // ...
}
```

### Manual Mapping (No ModelMapper)
```java
@Component
public class UserMapper {
    public UserDto toDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .nationalId(user.getNationalId())
            // ...
            .build();
    }
}
```

### Repository Pattern
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNationalId(String nationalId);
}
```

---

## 8. Development Status

### Current Mode
- **Development Mode ACTIVE:** All endpoints public for debugging
- **Production Rules:** Ready but commented out in SecurityConfig

### Seeded Users
| nationalId | Password | Role | Purpose |
|------------|----------|------|---------|
| `admin` | `admin` | ADMIN | Can create users, full access |
| `user` | `user` | USER | Regular employee |

---

## 9. NOT Implemented Yet (Scope for Future Work)

### High Priority
- [ ] TaskConstraint Controller (CRUD for task dependencies)
- [ ] Vacation Controller (CRUD for worker availability)
- [ ] Cycle detection for task constraints
- [ ] Algorithm Engine service setup
- [ ] Scheduling algorithm implementation

### Medium Priority
- [ ] Settlement Controller (schedule history)
- [ ] Task filtering (by status, priority, deadline)
- [ ] Role-based task assignment validation
- [ ] Worker availability checking

### Nice to Have
- [ ] Frontend integration
- [ ] Real-time notifications
- [ ] Audit logging
- [ ] Performance metrics

---

## 10. Key Business Rules

### User Management
- ❌ NO public registration
- ✅ Only ADMIN can create users
- ✅ `nationalId` is the unique identifier
- ✅ Passwords must be BCrypt encrypted

### Task Management
- Tasks have Status (lookup table, not enum)
- Tasks have Priority (lookup table with integer values)
- Tasks can be assigned to one User
- Tasks can require multiple Roles

### Task Constraints
- `predecessorTask` must complete before `successorTask`
- Constraint types: FINISH_TO_START, START_TO_START, etc.
- Optional lag time in minutes
- Must prevent circular dependencies (not implemented yet)

### Scheduling (Future)
- Respect task constraints (topological sort)
- Respect worker availability (vacations)
- Respect worker skills (roles)
- Minimize idle time
- Handle deadline violations

---

## 11. Database Configuration

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/main_backend_db
spring.datasource.username=root
spring.datasource.password=1234
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## 12. API Endpoint Summary

### Authentication
- `POST /api/auth/login` - Login with nationalId
- `POST /api/auth/refresh-token` - Refresh access token
- `POST /api/auth/logout` - Blacklist current token

### Users
- `POST /api/users` - Create user (ADMIN only)
- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/national-id/{nationalId}` - Get user by nationalId
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user (ADMIN only)

### Tasks
- `POST /api/tasks` - Create task
- `GET /api/tasks` - List all tasks
- `GET /api/tasks/{id}` - Get task by ID
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task

---

## 13. Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
    
    <!-- Tools -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

---

## 14. Important: What NOT to Do

### ❌ Do NOT Use
- Username for authentication (use `nationalId`)
- Email for authentication (use `nationalId`)
- Public registration endpoints
- Enums for Status/Priority (use Lookup Tables)
- ModelMapper or MapStruct (use manual mapping)
- String concatenation in queries (use JPA methods)

### ❌ Do NOT Create
- Public sign-up functionality
- Password reset without admin approval
- Direct database access from controllers
- DTOs without validation annotations

### ❌ Do NOT Assume
- Users have email addresses (it's optional)
- Status/Priority are Java enums (they're database tables)
- Frontend is already integrated (it's not)
- Algorithm Engine is implemented (it's planned)

---

## 15. Next Steps (When Requested)

1. **TaskConstraint Controller** - CRUD + cycle detection
2. **Vacation Controller** - Worker availability management
3. **Algorithm Engine Setup** - Separate Spring Boot service
4. **Scheduling Algorithm** - Greedy/heuristic implementation
5. **Frontend Integration** - React + API integration

---

**This document is the single source of truth for the Secure-Schedule project.**
**All code generation and suggestions must align with this context.**

