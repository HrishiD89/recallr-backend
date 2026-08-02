package com.recallr.domain.rag;

import java.util.List;

public record RagQueryResponse(
        String answer,
        List<RagSourceDTO> sources
) {
}