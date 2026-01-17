package com.smarthire.api.dto;

// import jakarta.validation.constraints.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
         @NotBlank @Email
        String email,

         @NotBlank
        String password
) {}