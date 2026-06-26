package com.recallr.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class JsoupExtractionStrategy implements ExtractionStrategy {

    @Override
    public String extract(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(5000)
                .get();

        Elements paragraphs = doc.select("p");
        StringBuilder sb = new StringBuilder();
        
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (!text.isEmpty()) {
                sb.append(text).append("\n\n");
            }
        }
        
        return sb.toString().trim();
    }

    @Override
    public String name() {
        return "JSOUP";
    }
}
