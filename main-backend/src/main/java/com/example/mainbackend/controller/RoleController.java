package com.example.mainbackend.controller;

        import com.example.mainbackend.entity.Role;
        import com.example.mainbackend.repository.RoleRepository;
        import lombok.RequiredArgsConstructor;
        import org.springframework.http.ResponseEntity;
        import org.springframework.security.access.prepost.PreAuthorize;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RestController;

        import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }
}

