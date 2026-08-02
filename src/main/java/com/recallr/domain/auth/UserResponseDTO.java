package com.recallr.domain.auth;

import com.recallr.domain.quota.UserTier;

public record UserResponseDTO(
        Long id,
        String username,
        UserTier tier,
        String shareToken,
        int bookmarkSavesUsed,
        int bookmarkLimit,
        int ragQueriesUsed,
        int ragLimit,
        long totalBookmarks,
        long totalRagQueries
) {}
