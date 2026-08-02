package com.recallr.domain.content;

public interface ExtractionStrategy {

    String extract(String url) throws Exception;

    String name();
}