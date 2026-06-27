package com.recallr.service.extraction.strategy;

import com.recallr.dto.ExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleExtractionStrategy implements ExtractionStrategy {

    private static final Logger log = LoggerFactory.getLogger(ArticleExtractionStrategy.class);

    private final List<ExtractionStrategy> strategies;

    public ArticleExtractionStrategy(JsoupExtractionStrategy jsoup,
                                     JinaExtractionStrategy jina,
                                     TinyfishExtractionStrategy tinyfish) {
        this.strategies = List.of(jsoup, jina, tinyfish);
    }

    @Override
    public String extract(String url) throws Exception {
        for (ExtractionStrategy strategy : strategies) {
            try {
                String text = strategy.extract(url);
                if (text != null && text.trim().length() > 50) {
                    log.info("Successfully extracted content using: {}", strategy.name());
                    return text;
                }
            } catch (Exception e) {
                // If a strategy fails, log it and let the loop proceed to the next fallback
                log.warn("Scraper {} failed for url={}. Error: {}", strategy.name(), url, e.getMessage());
            }
        }
        log.error("All extraction strategies failed for url={}", url);
        return "";
    }

    @Override
    public String name() {
        return "ARTICLE_FALLBACK_CHAIN";
    }

    public ExtractionResult extractWithSource(String url) {
        for (ExtractionStrategy strategy : strategies) {
            try {
                String text = strategy.extract(url);
                if (text != null && text.trim().length() > 50) {
                    log.info("Successfully extracted content using: {}", strategy.name());
                    return new ExtractionResult(text, strategy.name());
                }
            } catch (Exception e) {
                log.warn("Scraper {} failed for url={}. Error: {}", strategy.name(), url, e.getMessage());
            }
        }
        log.error("All extraction strategies failed for url={}", url);
        return new ExtractionResult("", "NONE");
    }
}