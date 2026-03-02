package com.example.mainbackend.service;

import com.example.mainbackend.dto.user.CreateUserRequest;
import com.example.mainbackend.dto.user.UserDto;
import com.example.mainbackend.entity.Role;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.mapper.UserMapper;
import com.example.mainbackend.repository.RoleRepository;
import com.example.mainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    // Create
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.findByNationalId(request.getNationalId()).isPresent())
            throw new IllegalArgumentException("National ID already exists: " + request.getNationalId());

        if (request.getEmail() != null && userRepository.findByEmail(request.getEmail()).isPresent())
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = userMapper.fromCreateRequest(request, encodedPassword);

        // Assign role — default to WORKER if not specified
        String roleName = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole().toUpperCase()
                : "WORKER";

        Set<Role> roles = new HashSet<>();
        roleRepository.findByRoleName(roleName).ifPresent(roles::add);
        // ADMIN also gets WORKER role
        if ("ADMIN".equals(roleName))
            roleRepository.findByRoleName("WORKER").ifPresent(roles::add);

        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    // Read - Get by ID
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    // Read - Get by National ID (Business Key)
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserByNationalId(String nationalId) {
        return userRepository.findByNationalId(nationalId)
                .map(userMapper::toDto);
    }

    // Read - Get by Email
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto);
    }

    // Read - Get all users
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    // Read - Get all users by role (e.g. "WORKER" or "ADMIN")
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(String roleName) {
        return userRepository.findByRoles_RoleName(roleName).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    // Update
    @Transactional
    public Optional<UserDto> updateUser(Long id, UserDto userDto) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    // Check if nationalId is taken by another user
                    userRepository.findByNationalId(userDto.getNationalId())
                            .filter(user -> !user.getId().equals(id))
                            .ifPresent(user -> {
                                throw new IllegalArgumentException("National ID already exists: " + userDto.getNationalId());
                            });

                    // Check if email is taken by another user (if provided)
                    if (userDto.getEmail() != null)
                        userRepository.findByEmail(userDto.getEmail())
                                .filter(user -> !user.getId().equals(id))
                                .ifPresent(user -> {
                                    throw new IllegalArgumentException("Email already exists: " + userDto.getEmail());
                                });

                    // Use mapper to update entity fields
                    userMapper.updateEntityFromDto(existingUser, userDto);

                    User updatedUser = userRepository.save(existingUser);
                    return userMapper.toDto(updatedUser);
                });
    }

    // Delete
    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Check if user exists by National ID
    @Transactional(readOnly = true)
    public boolean existsByNationalId(String nationalId) {
        return userRepository.findByNationalId(nationalId).isPresent();
    }

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
