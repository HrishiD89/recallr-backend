package com.recallr.service.extraction.strategy;

public interface ExtractionStrategy {

    String extract(String url) throws Exception;

    String name();
}