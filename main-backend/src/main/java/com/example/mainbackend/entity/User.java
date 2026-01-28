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
    @Column(name = "teudat_zehut", unique = true, nullable = false)
    private String teudatZehut; // Person ID

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
    private Integer dailyAvailabilityHours;
    private Integer maxTasks;

    // Relationship Roles - EAGER is acceptable for roles (usually small set)
    // Ensure that a user can have a role only once, the user_id and role_id combination must be unique
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"})
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Relationship Settlements - mappedBy establishes bidirectional relationship
    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Settlement> settlements = new ArrayList<>();

    // Relationship Vacations
    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vacation> vacations = new ArrayList<>();
}
