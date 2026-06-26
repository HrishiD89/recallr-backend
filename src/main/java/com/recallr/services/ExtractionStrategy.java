package com.recallr.services;

public interface ExtractionStrategy {
    
    /**
     * Attempts to extract the article text from the given URL.
     * Returns the raw text or empty string/null on failure.
     */
    String extract(String url) throws Exception;

    /**
     * Returns the identifier of this extraction mode (e.g. JSOUP, JINA, TINYFISH).
     */
    String name();
}
