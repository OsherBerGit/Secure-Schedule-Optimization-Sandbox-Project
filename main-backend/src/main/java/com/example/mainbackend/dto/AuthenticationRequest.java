package com.example.mainbackend.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {
    private String teudatZehut;
    private String password;
    private String ip;  // field to store the IP address of the client
}
