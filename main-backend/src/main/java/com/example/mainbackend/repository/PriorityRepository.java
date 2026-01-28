package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, Long> {
    Optional<Priority> findById(Long aLong);
}
