package com.recallr.domain.content;

import jakarta.validation.constraints.NotBlank;

public record ContentRequestDTO(
        @NotBlank String url
) {}