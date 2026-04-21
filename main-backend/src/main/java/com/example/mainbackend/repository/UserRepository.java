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
    Optional<User> findByNationalId(String nationalId);
    Optional<User> findByEmail(String email);
    List<User> findByRole_Name(String roleName);

    List<User> findAllByDepartmentId(Long departmentId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.skills")
    List<User> findAllWithSkills();

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.skills WHERE u.department.id = :departmentId")
    List<User> findAllWithSkillsByDepartment(@Param("departmentId") Long departmentId);
}
