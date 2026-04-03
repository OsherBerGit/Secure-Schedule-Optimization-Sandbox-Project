package com.example.mainbackend.service;

import com.example.mainbackend.dto.user.CreateUserRequest;
import com.example.mainbackend.dto.user.UserDto;
import com.example.mainbackend.dto.user.WorkerAvailabilityDto;
import com.example.mainbackend.entity.Department;
import com.example.mainbackend.entity.Skill;
import com.example.mainbackend.entity.Role;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.entity.WorkerAvailability;
import com.example.mainbackend.mapper.UserMapper;
import com.example.mainbackend.repository.DepartmentRepository;
import com.example.mainbackend.repository.SkillRepository;
import com.example.mainbackend.repository.RoleRepository;
import com.example.mainbackend.repository.UserRepository;
import com.example.mainbackend.security.SecurityHelper;
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
    private final DepartmentRepository departmentRepository;
    private final SecurityHelper securityHelper;
    private final SkillRepository skillRepository;

    // Create
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.findByNationalId(request.getNationalId()).isPresent())
            throw new IllegalArgumentException("National ID already exists: " + request.getNationalId());

        if (request.getEmail() != null && userRepository.findByEmail(request.getEmail()).isPresent())
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = userMapper.fromCreateRequest(request, encodedPassword);

        // Assign role â€” default to WORKER if not specified
        String roleName = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole().toUpperCase()
                : "WORKER";

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        user.setRole(role);

        // Assign skills (Multi-profession support)
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            Set<Skill> skills = new HashSet<>();
            for (Long skillId : request.getSkillIds())
                skillRepository.findById(skillId).ifPresent(skills::add);
            user.setSkills(skills);
        }

        // Set department by name if provided
        if (request.getDepartmentName() != null && !request.getDepartmentName().isBlank())
            departmentRepository.findByName(request.getDepartmentName())
                    .ifPresent(user::setDepartment);

        // Set availabilities if provided
        if (request.getAvailabilities() != null)
            for (WorkerAvailabilityDto dto : request.getAvailabilities()) {
                WorkerAvailability av = WorkerAvailability.builder()
                        .dayOfWeek(dto.getDayOfWeek())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .user(user)
                        .build();
                user.getAvailabilities().add(av);
            }

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
        if (securityHelper.isManager()) {
            Long departmentId = securityHelper.getCurrentUserDepartmentId();
            return userRepository.findAllByDepartmentId(departmentId).stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
        }
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    // Read - Get all users by role (e.g. "WORKER" or "ADMIN")
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(String roleName) {
        if (securityHelper.isManager()) {
            Long departmentId = securityHelper.getCurrentUserDepartmentId();
            return userRepository.findByRole_RoleName(roleName).stream()
                    .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(departmentId))
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
        }
        return userRepository.findByRole_RoleName(roleName).stream() // Updated Method Name
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    // Update
    @Transactional
    public Optional<UserDto> updateUser(Long id, UserDto userDto) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    // Check if nationalId is taken by another user
                    if (userDto.getNationalId() != null)
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

                    // Update basic entity fields manually to avoid overwriting non-mutable or missing DTO fields
                    if (userDto.getFirstName() != null) existingUser.setFirstName(userDto.getFirstName());
                    if (userDto.getLastName() != null) existingUser.setLastName(userDto.getLastName());
                    if (userDto.getEmail() != null) existingUser.setEmail(userDto.getEmail());
                    if (userDto.getPhoneNumber() != null) existingUser.setPhoneNumber(userDto.getPhoneNumber());
                    if (userDto.getSalary() != null) existingUser.setSalary(userDto.getSalary());
                    if (userDto.getAddress() != null) existingUser.setAddress(userDto.getAddress());
                    if (userDto.getMaxTasks() != null) existingUser.setMaxTasks(userDto.getMaxTasks());

                    // Update department by name
                    if (userDto.getDepartmentName() != null && !userDto.getDepartmentName().isBlank()) {
                        Department dept = departmentRepository.findByName(userDto.getDepartmentName())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Department not found: " + userDto.getDepartmentName()));
                        existingUser.setDepartment(dept);
                    } else
                        existingUser.setDepartment(null);

                    // Update role if provided
                    if (userDto.getRole() != null && !userDto.getRole().isEmpty()) {
                        String roleName = userDto.getRole().toUpperCase();
                        Role role = roleRepository.findByRoleName(roleName)
                                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                        existingUser.setRole(role);
                    }

                    // Update skills if provided
                    if (userDto.getSkillIds() != null) {
                        Set<Skill> skills = new HashSet<>();
                        for (Long skillId : userDto.getSkillIds())
                            skillRepository.findById(skillId).ifPresent(skills::add);

                        if (existingUser.getSkills() == null)
                            existingUser.setSkills(new HashSet<>());

                        existingUser.getSkills().clear();
                        existingUser.getSkills().addAll(skills);
                    }

                    // Replace availabilities
                    if (userDto.getAvailabilities() != null) {
                        if (existingUser.getAvailabilities() == null)
                            existingUser.setAvailabilities(new java.util.ArrayList<>());

                        existingUser.getAvailabilities().clear();
                        for (WorkerAvailabilityDto dto : userDto.getAvailabilities()) {
                            WorkerAvailability av = WorkerAvailability.builder()
                                    .dayOfWeek(dto.getDayOfWeek())
                                    .startTime(dto.getStartTime())
                                    .endTime(dto.getEndTime())
                                    .user(existingUser)
                                    .build();
                            existingUser.getAvailabilities().add(av);
                        }
                    }

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

    @Transactional
    public List<UserDto> getUsersByDepartmentId(Long departmentId) {
        return userRepository.findAllByDepartmentId(departmentId).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
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
