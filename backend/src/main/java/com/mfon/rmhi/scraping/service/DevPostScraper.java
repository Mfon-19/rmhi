package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.ScrapingConfig;
import com.mfon.rmhi.scraping.repository.StagedIdeaRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DevPostScraper {

    private static final String BASE_URL = "https://worldslargesthackathon.devpost.com";
    private static final String GALLERY_URL = BASE_URL + "/project-gallery?page=";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_PAGES_DEFAULT = 100;

    private final WebClient webClient;
    private final StagedIdeaRepository stagedIdeaRepository;

    public DevPostScraper(WebClient.Builder webClientBuilder, StagedIdeaRepository stagedIdeaRepository) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.stagedIdeaRepository = stagedIdeaRepository;
    }

    public List<ScrapedIdea> scrape(ScrapingConfig config) throws Exception {
        log.info("Starting DevPost scraping with config: {}", config.getSourceName());

        log.info("Loading existing project URLs from the database...");
        Set<String> existingUrls = stagedIdeaRepository.findAllSourceUrls();
        log.info("Loaded {} existing URLs.", existingUrls.size());

        List<ScrapedIdea> allIdeas = new ArrayList<>();
        int maxPages = config.getMaxPages() > 0 ? config.getMaxPages() : MAX_PAGES_DEFAULT;

        for (int page = 1; page <= maxPages; page++) {
            try {
                log.debug("Scraping gallery page {}", page);

                List<String> projectUrls = scrapeGalleryPage(page);
                if (projectUrls.isEmpty()) {
                    log.info("No more projects found on page {}, stopping.", page);
                    break;
                }

                for (String projectUrl : projectUrls) {
                    if (existingUrls.contains(projectUrl)) {
                        log.debug("Skipping already scraped project: {}", projectUrl);
                        continue;
                    }

                    try {
                        ScrapedIdea idea = scrapeProject(projectUrl);
                        if (idea != null && validateScrapedData(idea)) {
                            allIdeas.add(idea);
                            log.debug("Successfully scraped project: {}", idea.getTitle());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to process project {}: {}", projectUrl, e.getMessage());
                    } finally {
                        existingUrls.add(projectUrl);
                        Thread.sleep(config.getRateLimitMs());
                    }
                }

                log.info("Completed page {}, total ideas scraped: {}", page, allIdeas.size());

            } catch (Exception e) {
                log.error("Failed to scrape gallery page {}: {}", page, e.getMessage());
            }
        }

        log.info("DevPost scraping completed. Total ideas: {}", allIdeas.size());
        return allIdeas;
    }

    private List<String> scrapeGalleryPage(int page) throws Exception {
        String url = GALLERY_URL + page;
        log.debug("Scraping gallery page: {}", url);

        try {
            String html = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> {
                                log.warn("HTTP error {} when fetching gallery page {}", response.statusCode(), page);
                                return Mono.error(new WebClientResponseException(
                                        response.statusCode().value(),
                                        "HTTP error fetching gallery page",
                                        null, null, null));
                            })
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .doOnError(error -> log.error("Error fetching gallery page {}: {}", page, error.getMessage()))
                    .block();

            if (html == null || html.trim().isEmpty()) {
                log.warn("Empty response from gallery page {}", page);
                return Collections.emptyList();
            }

            Document doc = Jsoup.parse(html);
            Elements projectLinks = doc.select(".gallery-item a.link-to-software");

            List<String> urls = projectLinks.stream()
                    .map(element -> element.attr("href"))
                    .filter(href -> !href.isEmpty())
                    .map(href -> href.startsWith("http") ? href : BASE_URL + href)
                    .collect(Collectors.toList());

            log.debug("Found {} project URLs on page {}", urls.size(), page);
            return urls;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("Gallery page {} not found, assuming end of pages", page);
                return Collections.emptyList();
            }
            log.error("HTTP error {} when scraping gallery page {}: {}", e.getStatusCode(), page, e.getMessage());
            throw new Exception("Failed to scrape gallery page " + page + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error scraping gallery page {}: {}", page, e.getMessage(), e);
            throw new Exception("Failed to scrape gallery page " + page + ": " + e.getMessage(), e);
        }
    }

    private ScrapedIdea scrapeProject(String projectUrl) throws Exception {
        try {
            String html = webClient.get()
                    .uri(projectUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            Document doc = Jsoup.parse(html);

            String title = extractText(doc, "h1#app-title");
            String shortDescription = extractText(doc, "header.page-header p.large");
            String submittedTo = extractText(doc, "#submissions .software-list-content p a");
            boolean winner = !doc.select(".winner-label").isEmpty();

            String createdBy = doc.select("#app-team .user-profile-link").stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining(", "));

            String problemDescription = extractSection(doc, Pattern.compile("inspiration", Pattern.CASE_INSENSITIVE));
            String solution = extractSection(doc, Pattern.compile("what it does", Pattern.CASE_INSENSITIVE));
            String technicalDetails = extractSection(doc, Pattern.compile("how we built", Pattern.CASE_INSENSITIVE));

            int likes = 0;
            String likesText = extractText(doc, ".like-counts");
            if (likesText != null && !likesText.isEmpty()) {
                try {
                    likes = Integer.parseInt(likesText.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    log.debug("Could not parse likes count: {}", likesText);
                }
            }

            List<String> technologies = doc.select("#built-with .cp-tag").stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.toList());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("submittedTo", submittedTo);
            metadata.put("winner", winner);
            metadata.put("originalUrl", projectUrl);

            return ScrapedIdea.builder()
                    .sourceUrl(projectUrl)
                    .sourceWebsite("DevPost")
                    .scrapedAt(LocalDateTime.now())
                    .title(title)
                    .description(shortDescription)
                    .content(buildContent(problemDescription, solution, technicalDetails))
                    .author(createdBy)
                    .likes(likes)
                    .technologies(technologies)
                    .categories(Collections.emptyList())
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("Failed to scrape project {}: {}", projectUrl, e.getMessage());
            throw new Exception("Failed to scrape project: " + e.getMessage(), e);
        }
    }

    private String extractText(Document doc, String selector) {
        Element element = doc.selectFirst(selector);
        return element != null ? element.text().trim() : null;
    }

    private String extractSection(Document doc, Pattern titlePattern) {
        Elements headings = doc.select("h2");

        for (Element heading : headings) {
            if (titlePattern.matcher(heading.text()).find()) {
                StringBuilder content = new StringBuilder();
                Element current = heading.nextElementSibling();

                while (current != null && !current.tagName().equals("h2") && !current.tagName().equals("hr")) {
                    String text = current.text().trim();
                    if (!text.isEmpty()) {
                        content.append(text).append("\n");
                    }
                    current = current.nextElementSibling();
                }

                return content.toString().trim();
            }
        }

        return null;
    }

    private String buildContent(String problemDescription, String solution, String technicalDetails) {
        StringBuilder content = new StringBuilder();

        if (problemDescription != null && !problemDescription.isEmpty()) {
            content.append("Inspiration: ").append(problemDescription).append("\n\n");
        }

        if (solution != null && !solution.isEmpty()) {
            content.append("What it does: ").append(solution).append("\n\n");
        }

        if (technicalDetails != null && !technicalDetails.isEmpty()) {
            content.append("How we built it: ").append(technicalDetails);
        }

        return content.toString().trim();
    }

    public boolean validateScrapedData(ScrapedIdea idea) {
        if (idea == null) {
            log.warn("Scraped idea is null.");
            return false;
        }

        boolean isValid = true;
        if (idea.getTitle() == null || idea.getTitle().trim().isEmpty()) {
            log.warn("Validation failed for idea from {}: Title is missing.", idea.getSourceUrl());
            isValid = false;
        }

        if (idea.getDescription() == null || idea.getDescription().trim().isEmpty()) {
            log.warn("Validation failed for idea from {}: Description is missing.", idea.getSourceUrl());
            isValid = false;
        }

        if (idea.getSourceUrl() == null || idea.getSourceUrl().trim().isEmpty()) {
            log.warn("Validation failed for idea with title '{}': Source URL is missing.", idea.getTitle());
            isValid = false;
        }

        return isValid;
    }

}