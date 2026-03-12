package com.example.mainbackend.repository;

import com.example.mainbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNationalId(String nationalId); // Queries teudat_zehut column
    Optional<User> findByEmail(String email);
    List<User> findByRoles_RoleName(String roleName); // Find all users with a specific role

    /** All users scoped to a specific department. */
    List<User> findAllByDepartmentId(Long departmentId);

    /**
     * Loads ALL users with their roles eagerly in a single query.
     * Used by SchedulingService (ADMIN scope) to avoid N+1 when building AlgoUserRequests.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles")
    List<User> findAllWithRoles();

    /**
     * Loads users belonging to a specific department with their roles eagerly.
     * Used by SchedulingService (MANAGER scope).
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.department.id = :departmentId")
    List<User> findByDepartmentIdWithRoles(@Param("departmentId") Long departmentId);
}
