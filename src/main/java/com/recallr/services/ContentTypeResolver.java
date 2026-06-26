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

    public ContentMetadata resolve(String url) {
        if (url == null || url.isBlank()) {
            return new ContentMetadata(ContentType.OTHER, url, "", null, "", "");
        }

        Matcher ytMatcher = YOUTUBE_WATCH.matcher(url);
        if (ytMatcher.find()) {
            String videoId = ytMatcher.group(1);
            ScrapedPageInfo pageInfo = scrapePage(url);
            return new ContentMetadata(
                    ContentType.YOUTUBE,
                    "https://www.youtube.com/embed/" + videoId,
                    pageInfo.title.isEmpty() ? "YouTube Video" : pageInfo.title,
                    "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg",
                    pageInfo.author,
                    pageInfo.description
            );
        }

        if (url.contains("twitter.com") || url.contains("x.com")) {
            String authorName = fetchOEmbedField(
                    "https://publish.twitter.com/oembed?url=" + encode(url));
            return new ContentMetadata(
                    ContentType.TWITTER,
                    url,
                    authorName != null ? "Tweet by " + authorName : "Tweet",
                    null,
                    authorName != null ? authorName : "",
                    ""
            );
        }

        if (url.contains("instagram.com")) {
            ScrapedPageInfo pageInfo = scrapePage(url);
            return new ContentMetadata(
                    ContentType.INSTAGRAM,
                    url,
                    pageInfo.title.isEmpty() ? "Instagram Post" : pageInfo.title,
                    null,
                    pageInfo.author,
                    pageInfo.description
            );
        }

        // Default: Article scrape
        ScrapedPageInfo pageInfo = scrapePage(url);
        return new ContentMetadata(
                ContentType.ARTICLE,
                url,
                pageInfo.title.isEmpty() ? "Article" : pageInfo.title,
                null,
                pageInfo.author,
                pageInfo.description
        );
    }

    private String fetchOEmbedField(String oEmbedUrl) {
        try {
            Map<?, ?> body = restTemplate.getForObject(oEmbedUrl, Map.class);
            if (body != null) {
                return (String) body.get("author_name");
            }
            return null;
        } catch (Exception e) {
            log.warn("oEmbed fetch failed url={} reason={}", oEmbedUrl, e.getMessage());
            return null;
        }
    }

    private ScrapedPageInfo scrapePage(String url) {
        ScrapedPageInfo info = new ScrapedPageInfo();
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

            // 1. Scrape Title
            String ogTitle = doc.select("meta[property=og:title]").attr("content").trim();
            if (!ogTitle.isEmpty()) {
                info.title = ogTitle;
            } else {
                String twTitle = doc.select("meta[name=twitter:title]").attr("content").trim();
                if (!twTitle.isEmpty()) {
                    info.title = twTitle;
                } else {
                    String docTitle = doc.title().trim();
                    info.title = docTitle;
                }
            }

            // 2. Scrape Description
            String ogDesc = doc.select("meta[property=og:description]").attr("content").trim();
            if (!ogDesc.isEmpty()) {
                info.description = ogDesc;
            } else {
                String twDesc = doc.select("meta[name=twitter:description]").attr("content").trim();
                if (!twDesc.isEmpty()) {
                    info.description = twDesc;
                } else {
                    String metaDesc = doc.select("meta[name=description]").attr("content").trim();
                    info.description = metaDesc;
                }
            }

            // 3. Scrape Author
            String authorMeta = doc.select("meta[name=author]").attr("content").trim();
            if (!authorMeta.isEmpty()) {
                info.author = authorMeta;
            } else {
                String articleAuthor = doc.select("meta[property=article:author]").attr("content").trim();
                if (!articleAuthor.isEmpty()) {
                    info.author = articleAuthor;
                }
            }

        } catch (Exception e) {
            log.warn("scrapePage failed url={} reason={}", url, e.getMessage());
        }
        return info;
    }

    private String encode(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    // A simple tuple class to hold scraped results
    private static class ScrapedPageInfo {
        String title = "";
        String author = "";
        String description = "";
    }
}