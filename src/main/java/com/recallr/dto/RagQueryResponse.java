package com.recallr.dto;

import java.util.List;

public record RagQueryResponse(
        String answer,
        List<RagSourceDTO> sources
) {
}
