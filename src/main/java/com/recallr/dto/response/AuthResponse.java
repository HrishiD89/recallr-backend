package com.recallr.dto.response;

import java.util.Map;

public record AuthResponse(
        String token,
        String message
) {}