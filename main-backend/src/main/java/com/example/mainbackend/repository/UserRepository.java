package com.example.mainbackend.repository;

import com.example.mainbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNationalId(String nationalId); // Queries teudat_zehut column
    Optional<User> findByEmail(String email);
    List<User> findByRoles_RoleName(String roleName); // Find all users with a specific role
}
