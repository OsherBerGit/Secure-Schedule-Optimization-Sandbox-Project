package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id; // Worker ID

    // Authentication Details

    @Column(unique = true, nullable = false)
    private String nationalId; // Person ID

    @Column(nullable = false)
    private String password;

    // Personal Information
    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String phoneNumber;
    private Double salary;
    private String address;
    private Integer maxTasks;

    /**
     * The department this user belongs to.
     * Nullable — users not yet assigned to a department (e.g. a global admin) have no department.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Weekly availability windows (shifts) that define when this worker can be scheduled. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkerAvailability> availabilities = new ArrayList<>();

    // Access Level: Each user has exactly one security Role (ADMIN, MANAGER, WORKER)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Functional Skills: A user can have multiple Skill Titles (many-to-many)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_skills",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill_id"})
    )
    @Builder.Default
    private Set<Skill> skills = new HashSet<>();

    // Relationship Settlements - mappedBy establishes bidirectional relationship
    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Settlement> settlements = new ArrayList<>();

    // Relationship Vacations
    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vacation> vacations = new ArrayList<>();
}
