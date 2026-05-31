package com.recallr.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.stream.Collectors;

@Component
public class YoutubeTranscriptExtractor {

    private static final Logger log = LoggerFactory.getLogger(YoutubeTranscriptExtractor.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    public YoutubeTranscriptExtractor(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public String extract(String videoId) {
        try {
            YoutubeTranscriptApi api = TranscriptApiFactory.createDefault();
            TranscriptList transcripts = api.listTranscripts(videoId);
            Transcript transcript = transcripts.findTranscript("en");

            return transcript.fetch()
                    .getContent()
                    .stream()
                    .map(TranscriptContent.Fragment::getText)
                    .collect(Collectors.joining(" "));

        } catch (Exception e) {
            log.warn("YoutubeTranscriptExtractor failed videoId={} reason={}", videoId, e.getMessage());
            return "";
        }
    }
}