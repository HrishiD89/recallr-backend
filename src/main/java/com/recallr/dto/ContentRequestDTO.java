package com.recallr.dto;

import jakarta.validation.constraints.NotBlank;

public record ContentRequestDTO(
        @NotBlank String url
) {}