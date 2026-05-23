package com.recallr.services;

import com.recallr.dto.ContentMetadata;
import com.recallr.model.ContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContentTypeResolver {

    private static final Logger log = LoggerFactory.getLogger(ContentTypeResolver.class);


    private final RestTemplate restTemplate;

    public ContentTypeResolver(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    private static final Pattern YOUTUBE_WATCH = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    public ContentMetadata resolve(String url){
        if (url == null || url.isBlank()) {
            return new ContentMetadata(ContentType.OTHER, url,"", null);
        }

        Matcher ytMatcher = YOUTUBE_WATCH.matcher(url);
        if (ytMatcher.find()) {
            String videoId = ytMatcher.group(1);
            return new ContentMetadata(
                    ContentType.YOUTUBE,
                    "https://www.youtube.com/embed/" + videoId,
                    scrapeTitle(url),   // called here, not before
                    "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg"
            );
        }

        if (url.contains("twitter.com") || url.contains("x.com")) {
            String title = fetchOEmbedField(
                    "https://publish.twitter.com/oembed?url=" + encode(url));
            return new ContentMetadata(ContentType.TWITTER, url,
                    title != null ? title : "", null);
        }

        if (url.contains("instagram.com")) {
            return new ContentMetadata(ContentType.INSTAGRAM, url, scrapeTitle(url), null);
        }

        return new ContentMetadata(ContentType.ARTICLE, url,  scrapeTitle(url),null);
    }


    private String fetchOEmbedField(String oEmbedUrl) {
        try {
            Map<?, ?> body = restTemplate.getForObject(oEmbedUrl, Map.class);
            return body != null ? (String) body.get("author_name") : null;
        } catch (Exception e) {
            log.warn("oEmbed fetch failed url={} reason={}", oEmbedUrl, e.getMessage());
            return null;
        }
    }

    private String scrapeTitle(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .referrer("https://www.google.com")
                    .timeout(5000)
                    .followRedirects(true)
                    .get();

            String og = doc.select("meta[property=og:title]").attr("content");
            if (!og.isBlank()) return og;

            String tw = doc.select("meta[name=twitter:title]").attr("content");
            if (!tw.isBlank()) return tw;

            String title = doc.title();
            return title.isBlank() ? "" : title;

        } catch (Exception e) {
            log.warn("scrapeTitle failed url={} reason={}", url, e.getMessage());
            return "";  // never null
        }
    }

    private String encode(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

}