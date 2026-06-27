package com.recallr.service.extraction.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class YoutubeTranscriptStrategy implements ExtractionStrategy {

    private static final Logger log = LoggerFactory.getLogger(YoutubeTranscriptStrategy.class);

    private final ObjectMapper objectMapper;

    public YoutubeTranscriptStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String extract(String videoId) throws Exception {
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
            log.warn("YoutubeTranscriptStrategy failed videoId={} reason={}", videoId, e.getMessage());
            return "";
        }
    }

    @Override
    public String name() {
        return "YOUTUBE_TRANSCRIPT";
    }
}