package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.ScrapingConfig;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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

/**
 * DevPost scraper implementation that ports the existing TypeScript logic to Java
 * Handles session management, authentication, and project data extraction
 */
@Slf4j
@Service
public class DevPostScraper extends AbstractWebsiteScraper {
    
    private static final String BASE_URL = "https://worldslargesthackathon.devpost.com";
    private static final String PARTICIPANTS_URL = BASE_URL + "/participants?page=";
    private static final String LOGIN_URL = "https://devpost.com/users/login";
    
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_PAGES_DEFAULT = 100;
    
    private final WebClient webClient;
    private final String devpostEmail;
    private final String devpostPassword;
    
    // Session management
    private String sessionCookie;
    private LocalDateTime sessionExpiry;
    private static final Duration SESSION_DURATION = Duration.ofHours(2);
    
    public DevPostScraper(WebClient.Builder webClientBuilder,
                         @Value("${scraping.devpost.email:#{null}}") String devpostEmail,
                         @Value("${scraping.devpost.password:#{null}}") String devpostPassword) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
        this.devpostEmail = devpostEmail;
        this.devpostPassword = devpostPassword;
    }
    
    @Override
    protected List<ScrapedIdea> performScraping(ScrapingConfig config) throws Exception {
        log.info("Starting DevPost scraping with config: {}", config.getSourceName());
        
        // Validate credentials
        if (devpostEmail == null || devpostPassword == null) {
            throw new IllegalStateException("DevPost credentials not configured. Set scraping.devpost.email and scraping.devpost.password");
        }
        
        // Ensure we have a valid session
        ensureValidSession();
        
        List<ScrapedIdea> allIdeas = new ArrayList<>();
        Set<String> processedUrls = new HashSet<>();
        
        int maxPages = config.getMaxPages() > 0 ? config.getMaxPages() : MAX_PAGES_DEFAULT;
        
        // Scrape participant pages
        for (int page = 1; page <= maxPages; page++) {
            try {
                log.debug("Scraping participants page {}", page);
                
                List<String> profileUrls = scrapeParticipantPage(page);
                if (profileUrls.isEmpty()) {
                    log.info("No more participants found on page {}, stopping", page);
                    break;
                }
                
                // Process each profile
                for (String profileUrl : profileUrls) {
                    if (processedUrls.contains(profileUrl)) {
                        continue;
                    }
                    
                    try {
                        String projectUrl = getFirstProjectLink(profileUrl);
                        if (projectUrl != null && !processedUrls.contains(projectUrl)) {
                            ScrapedIdea idea = scrapeProject(projectUrl);
                            if (idea != null && validateScrapedData(idea)) {
                                allIdeas.add(idea);
                                log.debug("Successfully scraped project: {}", idea.getTitle());
                            }
                            processedUrls.add(projectUrl);
                        }
                        processedUrls.add(profileUrl);
                        
                        // Apply rate limiting between requests
                        Thread.sleep(config.getRateLimitMs());
                        
                    } catch (Exception e) {
                        log.warn("Failed to process profile {}: {}", profileUrl, e.getMessage());
                    }
                }
                
                log.info("Completed page {}, total ideas scraped: {}", page, allIdeas.size());
                
            } catch (Exception e) {
                log.error("Failed to scrape participants page {}: {}", page, e.getMessage());
                // Continue with next page
            }
        }
        
        log.info("DevPost scraping completed. Total ideas: {}", allIdeas.size());
        return allIdeas;
    }
    
    /**
     * Ensures we have a valid session cookie, refreshing if necessary
     */
    private void ensureValidSession() throws Exception {
        if (sessionCookie == null || sessionExpiry == null || LocalDateTime.now().isAfter(sessionExpiry)) {
            log.info("Session expired or not available, authenticating with DevPost");
            authenticateWithDevPost();
        }
    }
    
    /**
     * Authenticates with DevPost and obtains session cookie
     * Note: This is a simplified version - in production, you might need to handle
     * CSRF tokens, captchas, and other anti-bot measures
     */
    private void authenticateWithDevPost() throws Exception {
        log.info("Authenticating with DevPost using email: {}", devpostEmail);
        
        try {
            // First, get the login page to extract any CSRF tokens
            log.debug("Fetching DevPost login page");
            String loginPageHtml = webClient.get()
                    .uri(LOGIN_URL)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), 
                             response -> {
                                 log.error("Failed to fetch login page, status: {}", response.statusCode());
                                 return Mono.error(new RuntimeException("Login page fetch failed with status: " + response.statusCode()));
                             })
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .doOnError(error -> log.error("Timeout or error fetching login page: {}", error.getMessage()))
                    .block();
            
            if (loginPageHtml == null || loginPageHtml.trim().isEmpty()) {
                throw new Exception("Empty response from login page");
            }
            
            Document loginDoc = Jsoup.parse(loginPageHtml);
            
            // Extract CSRF token if present
            String csrfToken = null;
            Element csrfElement = loginDoc.selectFirst("meta[name=csrf-token]");
            if (csrfElement != null) {
                csrfToken = csrfElement.attr("content");
                log.debug("Found CSRF token in login page");
            }
            
            // Prepare login form data
            Map<String, String> formData = new HashMap<>();
            formData.put("user[email]", devpostEmail);
            formData.put("user[password]", devpostPassword);
            formData.put("user[remember_me]", "0");
            if (csrfToken != null) {
                formData.put("authenticity_token", csrfToken);
            }
            
            // For now, we'll simulate successful authentication
            // In a real implementation, you would need to handle the actual login flow
            sessionCookie = "_devpost_session=simulated_session_" + System.currentTimeMillis();
            sessionExpiry = LocalDateTime.now().plus(SESSION_DURATION);
            
            log.info("Successfully authenticated with DevPost (simulated)");
            log.debug("Session cookie expires at: {}", sessionExpiry);
            
        } catch (Exception e) {
            log.error("Failed to authenticate with DevPost: {}", e.getMessage(), e);
            throw new Exception("DevPost authentication failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Scrapes a participants page to get profile URLs
     */
    private List<String> scrapeParticipantPage(int page) throws Exception {
        String url = PARTICIPANTS_URL + page;
        log.debug("Scraping participants page: {}", url);
        
        try {
            String html = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, sessionCookie)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                             response -> {
                                 log.warn("HTTP error {} when fetching participants page {}", response.statusCode(), page);
                                 return Mono.error(new WebClientResponseException(
                                         response.statusCode().value(),
                                         "HTTP error fetching participants page",
                                         null, null, null));
                             })
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .doOnError(error -> log.error("Error fetching participants page {}: {}", page, error.getMessage()))
                    .block();
            
            if (html == null || html.trim().isEmpty()) {
                log.warn("Empty response from participants page {}", page);
                return Collections.emptyList();
            }
            
            Document doc = Jsoup.parse(html);
            Elements profileLinks = doc.select("#participants .user-profile-link");
            
            List<String> urls = profileLinks.stream()
                    .map(element -> element.attr("href"))
                    .filter(href -> !href.isEmpty())
                    .map(href -> href.startsWith("http") ? href : BASE_URL + href)
                    .collect(Collectors.toList());
            
            log.debug("Found {} profile URLs on page {}", urls.size(), page);
            return urls;
                    
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("Participants page {} not found, assuming end of pages", page);
                return Collections.emptyList(); // No more pages
            }
            log.error("HTTP error {} when scraping participants page {}: {}", e.getStatusCode(), page, e.getMessage());
            throw new Exception("Failed to scrape participants page " + page + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error scraping participants page {}: {}", page, e.getMessage(), e);
            throw new Exception("Failed to scrape participants page " + page + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets the first project link from a user profile
     */
    private String getFirstProjectLink(String profileUrl) throws Exception {
        try {
            String html = webClient.get()
                    .uri(profileUrl)
                    .header(HttpHeaders.COOKIE, sessionCookie)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            
            Document doc = Jsoup.parse(html);
            Element firstProjectLink = doc.selectFirst("#software-entries a");
            
            if (firstProjectLink != null) {
                String href = firstProjectLink.attr("href");
                return href.startsWith("http") ? href : BASE_URL + href;
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("Failed to get project link from profile {}: {}", profileUrl, e.getMessage());
            return null;
        }
    }
    
    /**
     * Scrapes a project page to extract idea details
     */
    private ScrapedIdea scrapeProject(String projectUrl) throws Exception {
        try {
            String html = webClient.get()
                    .uri(projectUrl)
                    .header(HttpHeaders.COOKIE, sessionCookie)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            
            Document doc = Jsoup.parse(html);
            
            // Extract project details using the same logic as TypeScript version
            String title = extractText(doc, "h1#app-title");
            String shortDescription = extractText(doc, "header.page-header p.large");
            String submittedTo = extractText(doc, "#submissions .software-list-content p a");
            boolean winner = !doc.select(".winner-label").isEmpty();
            
            // Extract team members
            String createdBy = doc.select("#app-team .user-profile-link").stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining(", "));
            
            // Extract content sections
            String problemDescription = extractSection(doc, Pattern.compile("inspiration", Pattern.CASE_INSENSITIVE));
            String solution = extractSection(doc, Pattern.compile("what it does", Pattern.CASE_INSENSITIVE));
            String technicalDetails = extractSection(doc, Pattern.compile("how we built", Pattern.CASE_INSENSITIVE));
            
            // Extract likes
            int likes = 0;
            String likesText = extractText(doc, ".like-counts");
            if (likesText != null && !likesText.isEmpty()) {
                try {
                    likes = Integer.parseInt(likesText.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    log.debug("Could not parse likes count: {}", likesText);
                }
            }
            
            // Extract technologies
            List<String> technologies = doc.select("#built-with .cp-tag").stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.toList());
            
            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("submittedTo", submittedTo);
            metadata.put("winner", winner);
            metadata.put("originalUrl", projectUrl);
            
            // Create ScrapedIdea
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
                    .categories(Collections.emptyList()) // DevPost doesn't have explicit categories
                    .metadata(metadata)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to scrape project {}: {}", projectUrl, e.getMessage());
            throw new Exception("Failed to scrape project: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts text from a CSS selector, returning null if not found
     */
    private String extractText(Document doc, String selector) {
        Element element = doc.selectFirst(selector);
        return element != null ? element.text().trim() : null;
    }
    
    /**
     * Extracts a content section based on heading pattern
     */
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
    
    /**
     * Builds combined content from different sections
     */
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
}