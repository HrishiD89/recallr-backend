package com.recallr.domain.content;

import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextExtractionService {

    private final ArticleExtractionStrategy articleExtractionStrategy;
    private final YoutubeTranscriptStrategy youtubeTranscriptStrategy;

    public TextExtractionService(ArticleExtractionStrategy articleExtractionStrategy,
                                 YoutubeTranscriptStrategy youtubeTranscriptStrategy) {
        this.articleExtractionStrategy = articleExtractionStrategy;
        this.youtubeTranscriptStrategy = youtubeTranscriptStrategy;
    }

    private static final Pattern YOUTUBE_WATCH = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    public ExtractionResult extract(String url, ContentType type) {
        return switch (type) {
            case YOUTUBE -> {
                Matcher m = YOUTUBE_WATCH.matcher(url);
                String text = "";
                if (m.find()) {
                    try {
                        text = youtubeTranscriptStrategy.extract(m.group(1));
                    } catch (Exception e) {
                        text = "";
                    }
                }
                yield new ExtractionResult(text, "YOUTUBE_TRANSCRIPT");
            }
            case ARTICLE -> articleExtractionStrategy.extractWithSource(url);
            default -> new ExtractionResult("", "NONE");
        };
    }
}