package com.recallr.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ContentRequestDTO(
        @NotBlank String url
) {}