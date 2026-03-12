package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents an organisational department (e.g. "Engineering", "Operations").
 *
 * <p>Users and Tasks are scoped to a department so that MANAGER-role users
 * can only schedule work within their own department.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}

