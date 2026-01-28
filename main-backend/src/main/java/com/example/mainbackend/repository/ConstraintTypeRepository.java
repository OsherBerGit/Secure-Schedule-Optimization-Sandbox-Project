package com.example.mainbackend.repository;

import com.example.mainbackend.entity.ConstraintType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConstraintTypeRepository extends JpaRepository<ConstraintType, Long> {
    Optional<ConstraintType> findByName(String name);
}
