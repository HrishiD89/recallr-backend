package com.recallr.domain.auth;

import java.util.Map;

public record AuthResponse(
        String token,
        String message
) {}