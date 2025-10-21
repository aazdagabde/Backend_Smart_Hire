package com.smarthire.api.dto;

// import jakarta.validation.constraints.*;

public record LoginRequest(
        // @NotBlank @Email
        String email,

        // @NotBlank
        String password
) {}