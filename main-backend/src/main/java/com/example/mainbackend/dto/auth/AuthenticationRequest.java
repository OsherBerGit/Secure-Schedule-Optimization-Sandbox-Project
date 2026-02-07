package com.example.mainbackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {

    @NotBlank(message = "National ID is required")
    private String nationalId; // Israeli National ID (Teudat Zehut)

    @NotBlank(message = "Password is required")
    private String password;

    private String ip;  // field to store the IP address of the client (set by server)
}
