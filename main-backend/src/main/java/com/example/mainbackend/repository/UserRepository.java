package com.example.mainbackend.repository;

import com.example.mainbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNationalId(String nationalId); // Queries teudat_zehut column
    Optional<User> findByEmail(String email);
    List<User> findByRoles_RoleName(String roleName); // Find all users with a specific role

    /**
     * Loads all users with their roles eagerly in a single query.
     * Used by SchedulingService to avoid N+1 when building AlgoUserRequests.
     * Vacations are loaded separately per-user via VacationRepository to avoid Cartesian product.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles")
    List<User> findAllWithRoles();
}
