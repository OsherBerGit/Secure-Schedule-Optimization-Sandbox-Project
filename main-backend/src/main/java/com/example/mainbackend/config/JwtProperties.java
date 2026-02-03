package com.example.mainbackend.config;

public class JwtProperties {
    // The EXPIRATION_TIME constant is used to set the expiration time of the JWT

    public static final int ACCESS_TOKEN_EXPIRATION_TIME = 5 * 60 * 1000; // 5 minutes

    public static final int REFRESH_TOKEN_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000; // 7 days

    // The TOKEN_PREFIX constant is used to prefix the JWT in the Authorization header
    public static final String TOKEN_PREFIX = "Bearer ";

    // The HEADER_STRING constant is used to set the key of the Authorization header
    public static final String HEADER_STRING = "Authorization";
}
