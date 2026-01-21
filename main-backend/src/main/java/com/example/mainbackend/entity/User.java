package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Worker ID

    // Authentication Details

    @Column(name = "teudat_zehut", unique = true, nullable = false)
    private String teudatZehut; // Person ID

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String phoneNumber;
    private Double salary;
    private String address;

    private Integer dailyAvailabilityHours;

    private Integer maxTasks;

    // Relationship Roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"})
    )
    private Set<Role> roles;

    // Relationship Tasks
    @OneToMany(mappedBy = "assignedEmployee", fetch = FetchType.LAZY)
    private List<Task> tasks;
}
