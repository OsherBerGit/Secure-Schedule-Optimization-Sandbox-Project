package com.example.mainbackend.repository;

import com.example.mainbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTeudatZehut(String teudatZehut);
    Optional<User> findByEmail(String email);
}
