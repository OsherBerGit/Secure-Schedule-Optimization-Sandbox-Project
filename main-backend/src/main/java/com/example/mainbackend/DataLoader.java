package com.example.mainbackend;

import com.example.mainbackend.entity.Role;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.repository.RoleRepository;
import com.example.mainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // check if the database is empty
        if (userRepository.count() > 0)
            return;

        // else, populate the database with some data,create roles, admin and user, and save them to the database
        Role adminRole = new Role();
        adminRole.setRoleName("ADMIN");
        roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setRoleName("USER");
        roleRepository.save(userRole);

        // create an admin user with an admin role and save it to the database
        User adminUser = new User();
        adminUser.setTeudatZehut("admin");
        // encode the password
        adminUser.setPassword(passwordEncoder.encode("admin"));
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);
        adminUser.setRoles(roles);
        userRepository.save(adminUser);

        // create a user with a user role and save it to the database
        User user = new User();
        user.setTeudatZehut("user");
        // encode the password
        user.setPassword(passwordEncoder.encode("user"));
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        user.setRoles(userRoles);
        userRepository.save(user);
    }

}
