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
    List<User> findByRole_RoleName(String roleName); // Updated to single Role

    List<User> findAllByDepartmentId(Long departmentId);

    /**
     * Loads ALL users with their skills eagerly in a single query.
     * Used by SchedulingService (ADMIN scope) to avoid N+1 when building AlgoUserRequests.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.skills")
    List<User> findAllWithSkills();

    /**
     * Loads users belonging to a specific department with their skills eagerly.
     * Used by SchedulingService (MANAGER scope).
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.skills WHERE u.department.id = :departmentId")
    List<User> findAllWithSkillsByDepartment(@Param("departmentId") Long departmentId);
}
