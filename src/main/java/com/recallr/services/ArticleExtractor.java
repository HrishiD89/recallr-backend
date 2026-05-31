package com.recallr.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;


@Component
public class ArticleExtractor {

    private static final Logger log = LoggerFactory.getLogger(ArticleExtractor.class);

    public String extract(String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(5000).get();

            return doc.select("p").stream().map(Element::text).collect(Collectors.joining("\n\n"));

        } catch (Exception e) {
            log.warn("scrapeTitle failed url={} reason={}", url, e.getMessage());
            return "";
        }
    }
}
