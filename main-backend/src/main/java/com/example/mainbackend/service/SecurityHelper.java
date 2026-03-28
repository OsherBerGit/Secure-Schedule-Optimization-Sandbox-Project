package com.example.mainbackend.service;

import com.example.mainbackend.entity.User;
import com.example.mainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityHelper {

    private final UserRepository userRepository;

    /**
     * Retrieves the currently authenticated user from the database.
     * Throws an exception if the user is not found.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal()))
            throw new IllegalStateException("No authenticated user found");

        String nationalId;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails)
            nationalId = ((UserDetails) principal).getUsername();
        else if (principal instanceof String)
            nationalId = (String) principal;
        else
            throw new IllegalStateException("Unknown principal type: " + principal.getClass());

        return userRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new IllegalStateException("User not found in database: " + nationalId));
    }

    /**
     * Checks if the current user has the given role.
     * The role name should be provided without the "ROLE_" prefix (e.g., "ADMIN").
     */
    public boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;

        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName));
    }

    public boolean isAdmin() { return hasRole("ADMIN"); }

    public boolean isManager() { return hasRole("MANAGER"); }

    /**
     * Helper to enforce department-level access control.
     * Returns the department ID for MANAGERs.
     * Returns null for ADMINs (global access).
     */
    public Long getCurrentUserDepartmentId() {
        if (isAdmin()) { return null; } // Global access

        User user = getCurrentUser();
        if (user.getDepartment() == null)
            throw new IllegalStateException("Manager user has no department assigned");
        return user.getDepartment().getId();
    }
}

