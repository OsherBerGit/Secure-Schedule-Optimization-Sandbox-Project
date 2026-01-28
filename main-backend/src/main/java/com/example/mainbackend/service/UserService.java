package com.example.mainbackend.service;

import com.example.mainbackend.dto.CreateUserRequest;
import com.example.mainbackend.dto.UserDto;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.mapper.UserMapper;
import com.example.mainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // Create
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        // Check if teudatZehut is already taken
        if (userRepository.findByTeudatZehut(request.getTeudatZehut()).isPresent())
            throw new IllegalArgumentException("Teudat Zehut already exists: " + request.getTeudatZehut());

        // Check if email is already taken (if provided)
        if (request.getEmail() != null && userRepository.findByEmail(request.getEmail()).isPresent())
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());

        User user = User.builder()
                .teudatZehut(request.getTeudatZehut())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .salary(request.getSalary())
                .address(request.getAddress())
                .dailyAvailabilityHours(request.getDailyAvailabilityHours())
                .maxTasks(request.getMaxTasks())
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    // Read - Get by ID
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    // Read - Get by Teudat Zehut (Business Key)
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserByTeudatZehut(String teudatZehut) {
        return userRepository.findByTeudatZehut(teudatZehut)
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

    // Update
    @Transactional
    public Optional<UserDto> updateUser(Long id, UserDto userDto) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    // Check if teudatZehut is taken by another user
                    userRepository.findByTeudatZehut(userDto.getTeudatZehut())
                            .filter(user -> !user.getId().equals(id))
                            .ifPresent(user -> {
                                throw new IllegalArgumentException("Teudat Zehut already exists: " + userDto.getTeudatZehut());
                            });

                    // Check if email is taken by another user (if provided)
                    if (userDto.getEmail() != null) {
                        userRepository.findByEmail(userDto.getEmail())
                                .filter(user -> !user.getId().equals(id))
                                .ifPresent(user -> {
                                    throw new IllegalArgumentException("Email already exists: " + userDto.getEmail());
                                });
                    }

                    existingUser.setTeudatZehut(userDto.getTeudatZehut());
                    existingUser.setFirstName(userDto.getFirstName());
                    existingUser.setLastName(userDto.getLastName());
                    existingUser.setEmail(userDto.getEmail());
                    existingUser.setPhoneNumber(userDto.getPhoneNumber());
                    existingUser.setSalary(userDto.getSalary());
                    existingUser.setAddress(userDto.getAddress());
                    existingUser.setDailyAvailabilityHours(userDto.getDailyAvailabilityHours());
                    existingUser.setMaxTasks(userDto.getMaxTasks());
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

    // Check if user exists by Teudat Zehut
    @Transactional(readOnly = true)
    public boolean existsByTeudatZehut(String teudatZehut) {
        return userRepository.findByTeudatZehut(teudatZehut).isPresent();
    }

    // Check if user exists by Email
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
