package com.example.mainbackend.controller;

import com.example.mainbackend.dto.user.CreateUserRequest;
import com.example.mainbackend.dto.user.UserDto;
import com.example.mainbackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.security.Principal;

/**
 * Controller for User management.
 *
 * Business Rules:
 * - Only ADMIN users can create new employees (no public registration)
 * - ADMIN users can perform all CRUD operations
 * - Regular users can view their own profile
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * Creates a new user (employee).
     * RESTRICTED TO ADMIN ONLY - No public registration allowed.
     *
     * @param request the user creation request
     * @return ResponseEntity with the created user and HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Retrieves all users.
     * ALLOWED FOR: Any authenticated user (service will filter by department if manager).
     * @return ResponseEntity with list of all users and HTTP 200 status
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Retrieves the currently authenticated user's profile.
     * ALLOWED FOR: Any authenticated user.
     * @param principal the current authenticated principal (contains national ID)
     * @return ResponseEntity with the user and HTTP 200 if found
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUser(Principal principal) {
        return userService.getUserByNationalId(principal.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves a user by their system ID.
     * ALLOWED FOR: Admin, Manager (of the same department), and the User themselves.
     * @param id the user's system-generated ID
     * @return ResponseEntity with the user and HTTP 200 if found, HTTP 404 if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageUser(#id)")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves a user by their national ID.
     * RESTRICTED TO ADMIN ONLY (For global searches).
     * @param nationalId the user's Israeli national ID
     * @return ResponseEntity with the user and HTTP 200 if found, HTTP 404 if not found
     */
    @GetMapping("/national-id/{nationalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserByNationalId(@PathVariable String nationalId) {
        return userService.getUserByNationalId(nationalId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves a user by their email address.
     * RESTRICTED TO ADMIN ONLY (For global searches).
     * @param email the user's email address
     * @return ResponseEntity with the user and HTTP 200 if found, HTTP 404 if not found
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all users with a specific role (e.g. WORKER or ADMIN).
     *
     * @param role the role name to filter by
     * @return ResponseEntity with list of users
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    /**
     * Updates an existing user.
     *
     * @param id the user ID to update
     * @param userDto the user data with updated values
     * @return ResponseEntity with the updated user and HTTP 200 if found, HTTP 404 if not found
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @securityHelper.canManageUser(#id))")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
        return userService.updateUser(id, userDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id the user ID to delete
     * @return ResponseEntity with HTTP 204 if deleted, HTTP 404 if not found
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.deleteUser(id))
            return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves all users belonging to a specific department.
     * ALLOWED FOR: ADMIN, or the MANAGER of this department.
     */
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("@securityHelper.canManageDepartment(#departmentId)")
    public ResponseEntity<List<UserDto>> getUsersByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(userService.getUsersByDepartmentId(departmentId));
    }
}
