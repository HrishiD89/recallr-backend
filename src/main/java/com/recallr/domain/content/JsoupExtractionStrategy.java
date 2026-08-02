package com.recallr.domain.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JsoupExtractionStrategy implements ExtractionStrategy {

    private static final List<String> PAYWALL_SIGNALS = List.of(
            "member-only", "not a medium member", "sign up", "sign in",
            "create an account", "subscribe", "unlock full access",
            "you've read all your free stories", "to continue reading",
            "unlock this story", "become a member", "this is a subscriber-only story",
            "log in", "log-in", "join medium"
    );

    @Override
    public String extract(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        Elements paragraphs = doc.select("article p, main p, div[itemprop*=article] p, [role=article] p, p");
        StringBuilder sb = new StringBuilder();

        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (!text.isEmpty()) {
                sb.append(text).append("\n\n");
            }
        }

        String extracted = sb.toString().trim();

        if (isPaywallContent(extracted)) {
            return "";
        }

        if (isBoilerplate(extracted)) {
            return "";
        }

        return extracted;
    }

    private boolean isPaywallContent(String text) {
        if (text.length() < 100) return false;
        String lower = text.substring(0, Math.min(text.length(), 1000)).toLowerCase();
        long matches = PAYWALL_SIGNALS.stream().filter(lower::contains).count();
        return matches >= 3;
    }

    private boolean isBoilerplate(String text) {
        if (text.length() < 200) return false;
        String[] lines = text.split("\n");
        int shortLines = 0;
        for (String line : lines) {
            if (line.trim().length() < 40) shortLines++;
        }
        return (double) shortLines / lines.length > 0.6;
    }

    @Override
    public String name() {
        return "JSOUP";
    }
}