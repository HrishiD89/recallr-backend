package com.recallr.domain.rag;

import com.recallr.domain.auth.User;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagPromptBuilder {

    public String build(String query, List<RagSearchResult> matches) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < matches.size(); i++) {
            RagSearchResult match = matches.get(i);
            context.append("Source ")
                    .append(i + 1)
                    .append(" | title: ")
                    .append(nullToUnknown(match.title()))
                    .append(" | url: ")
                    .append(match.url())
                    .append("\n")
                    .append(match.text())
                    .append("\n\n");
        }

        return """
                You are Recallr, an assistant that answers using only the user's saved content.

                Rules:
                - Answer in at most 3 sentences. No preamble, no restating the question.
                - If the saved content doesn't contain enough information, say in one sentence that you couldn't find it. Do not guess.
                - Cite inline using [Source 1], [Source 2], etc. — one source number per bracket. If a sentence draws on multiple sources, use separate brackets like [Source 1][Source 3], never [Source 1, Source 3].
                User question:
                %s

                Saved content context:
                %s

                Answer:
                """.formatted(query, context);
    }

    private String nullToUnknown(String value) {
        return value == null || value.isBlank() ? "Unknown title" : value;
    }
}