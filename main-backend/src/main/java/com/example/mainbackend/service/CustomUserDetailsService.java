package com.example.mainbackend.service;

import com.example.mainbackend.entity.Role;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String teudatZehut) throws UsernameNotFoundException {
        User user = userRepository.findByTeudatZehut(teudatZehut).orElse(null);

        if (user != null) {
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getTeudatZehut(),  // Use teudatZehut as the principal identifier instead of username
                    user.getPassword(),
                    mapRolesToAuthorities(user.getRoles()));
            if (!userDetails.isEnabled())
                throw new DisabledException("User account is disabled");

            if (!userDetails.isAccountNonLocked())
                throw new LockedException("User account is locked");

            return userDetails;

        } else {
            throw new UsernameNotFoundException("Invalid teudat zehut: " + teudatZehut);
        }
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Role> roles) {
        return roles.stream()
                // add the prefix "ROLE_" to the role name, it is required by Spring Security
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))
                .collect(Collectors.toList());
    }
}
