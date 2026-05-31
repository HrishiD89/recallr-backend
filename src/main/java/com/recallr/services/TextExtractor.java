package com.recallr.services;

import com.recallr.dto.ContentMetadata;
import com.recallr.model.ContentType;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextExtractor {

    private final ArticleExtractor articleExtractor;
    private final YoutubeTranscriptExtractor youtubeTranscriptExtractor;

    public TextExtractor(ArticleExtractor articleExtractor, YoutubeTranscriptExtractor youtubeTranscriptExtractor) {
        this.articleExtractor = articleExtractor;
        this.youtubeTranscriptExtractor = youtubeTranscriptExtractor;
    }

    private static final Pattern YOUTUBE_WATCH = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    public String extract(String url, ContentType type) {

        return switch (type) {
            case YOUTUBE -> {
                Matcher m = YOUTUBE_WATCH.matcher(url);
                yield m.find() ? youtubeTranscriptExtractor.extract(m.group(1)) : "";
            }
            case ARTICLE -> articleExtractor.extract(url);
            case TWITTER, INSTAGRAM -> "";
            default -> "";
        };

    }

}
