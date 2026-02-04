package com.example.mainbackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {
    private String nationalId; // Israeli National ID (Teudat Zehut)
    private String password;
    private String ip;  // field to store the IP address of the client
}
