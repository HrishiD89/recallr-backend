package com.recallr.dto.response;

import java.util.List;

public record RagQueryResponse(
        String answer,
        List<RagSourceDTO> sources
) {
}