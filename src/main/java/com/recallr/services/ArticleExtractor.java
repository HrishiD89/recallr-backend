package com.recallr.services;

import com.recallr.dto.ExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ArticleExtractor {

    private static final Logger log = LoggerFactory.getLogger(ArticleExtractor.class);

    private final List<ExtractionStrategy> strategies;

    public ArticleExtractor(JsoupExtractionStrategy jsoup,
                            JinaExtractionStrategy jina,
                            TinyfishExtractionStrategy tinyfish) {
        this.strategies = List.of(jsoup, jina, tinyfish);
    }

    public ExtractionResult extract(String url) {
        for(ExtractionStrategy strategy : strategies) {
            try{
                String text = strategy.extract(url);
                if (text != null && text.trim().length() > 50) {
                    log.info("Successfully extracted content using: {}", strategy.name());
                    return new ExtractionResult(text, strategy.name());
                }
            }catch (Exception e){
                // If a strategy fails, log it and let the loop proceed to the next fallback
                log.warn("Scraper {} failed for url={}. Error: {}", strategy.name(), url, e.getMessage());
            }
        }
        log.error("All extraction strategies failed for url={}", url);
        return new ExtractionResult("", "NONE");
    }
}
