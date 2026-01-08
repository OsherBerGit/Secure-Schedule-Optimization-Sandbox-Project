package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String roleName;
}
