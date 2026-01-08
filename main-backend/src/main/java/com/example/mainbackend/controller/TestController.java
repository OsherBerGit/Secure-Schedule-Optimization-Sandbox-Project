package com.example.mainbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/status")
    public String getStatus() {
        return "Backend is running and Secure Schedule Sandbox is active!";
    }
}
