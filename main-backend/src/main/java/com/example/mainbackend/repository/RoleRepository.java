package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String roleName);

    @Modifying
    @Query(value = "DELETE FROM user_role WHERE role_id = :roleId", nativeQuery = true)
    void deleteUserRoleAssociations(@Param("roleId") Long roleId);
}
