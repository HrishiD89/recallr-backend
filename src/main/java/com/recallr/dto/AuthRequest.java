package com.recallr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record AuthRequest(
        @Size(min = 3, max = 10, message = "Username must be 3-10 characters")
        @NotBlank
        String username,

        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
                message = "Password must be 8-20 chars, include upper, lower, number, and special char"
        )
        String password
) {}