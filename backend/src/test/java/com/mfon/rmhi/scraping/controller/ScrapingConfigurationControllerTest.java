package com.mfon.rmhi.scraping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfon.rmhi.scraping.entity.ScrapingSource;
import com.mfon.rmhi.scraping.service.ScrapingConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScrapingConfigurationController.class)
@WithMockUser
class ScrapingConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScrapingConfigurationService configurationService;

    @Autowired
    private ObjectMapper objectMapper;

    private ScrapingSource validSource;

    @BeforeEach
    void setUp() {
        validSource = new ScrapingSource();
        validSource.setId(1);
        validSource.setName("DevPost");
        validSource.setBaseUrl("https://devpost.com");
        validSource.setScraperClass("com.mfon.rmhi.scraping.service.DevPostScraper");
        validSource.setEnabled(true);
        validSource.setCronExpression("0 0 2 * * ?");
        validSource.setRateLimitMs(1000);
        validSource.setMaxPages(100);
    }

    @Test
    void getAllSources_ShouldReturnAllSources() throws Exception {
        // Given
        List<ScrapingSource> sources = Arrays.asList(validSource);
        when(configurationService.getAllSources()).thenReturn(sources);

        // When & Then
        mockMvc.perform(get("/api/scraping/config/sources"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("DevPost"));

        verify(configurationService).getAllSources();
    }

    @Test
    void getEnabledSources_ShouldReturnEnabledSources() throws Exception {
        // Given
        List<ScrapingSource> sources = Arrays.asList(validSource);
        when(configurationService.getEnabledSources()).thenReturn(sources);

        // When & Then
        mockMvc.perform(get("/api/scraping/config/sources/enabled"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].enabled").value(true));

        verify(configurationService).getEnabledSources();
    }

    @Test
    void getSourceById_WhenExists_ShouldReturnSource() throws Exception {
        // Given
        when(configurationService.getSourceById(1)).thenReturn(Optional.of(validSource));

        // When & Then
        mockMvc.perform(get("/api/scraping/config/sources/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("DevPost"));

        verify(configurationService).getSourceById(1);
    }

    @Test
    void getSourceById_WhenNotExists_ShouldReturnNotFound() throws Exception {
        // Given
        when(configurationService.getSourceById(999)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/scraping/config/sources/999"))
                .andExpect(status().isNotFound());

        verify(configurationService).getSourceById(999);
    }

    @Test
    void getSourceByName_WhenExists_ShouldReturnSource() throws Exception {
        // Given
        when(configurationService.getSourceByName("DevPost")).thenReturn(Optional.of(validSource));

        // When & Then
        mockMvc.perform(get("/api/scraping/config/sources/by-name/DevPost"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("DevPost"));

        verify(configurationService).getSourceByName("DevPost");
    }

    @Test
    void createSource_WithValidSource_ShouldCreateSuccessfully() throws Exception {
        // Given
        when(configurationService.createSource(any(ScrapingSource.class))).thenReturn(validSource);

        // When & Then
        mockMvc.perform(post("/api/scraping/config/sources")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSource)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("DevPost"));

        verify(configurationService).createSource(any(ScrapingSource.class));
    }

    @Test
    void createSource_WithInvalidSource_ShouldReturnBadRequest() throws Exception {
        // Given
        when(configurationService.createSource(any(ScrapingSource.class)))
                .thenThrow(new IllegalArgumentException("Source name cannot be null or empty"));

        // When & Then
        mockMvc.perform(post("/api/scraping/config/sources")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSource)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Source name cannot be null or empty"));

        verify(configurationService).createSource(any(ScrapingSource.class));
    }

    @Test
    void updateSource_WithValidSource_ShouldUpdateSuccessfully() throws Exception {
        // Given
        when(configurationService.updateSource(eq(1), any(ScrapingSource.class))).thenReturn(validSource);

        // When & Then
        mockMvc.perform(put("/api/scraping/config/sources/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSource)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("DevPost"));

        verify(configurationService).updateSource(eq(1), any(ScrapingSource.class));
    }

    @Test
    void updateSource_WithNonExistentId_ShouldReturnBadRequest() throws Exception {
        // Given
        when(configurationService.updateSource(eq(999), any(ScrapingSource.class)))
                .thenThrow(new IllegalArgumentException("Scraping source with ID 999 not found"));

        // When & Then
        mockMvc.perform(put("/api/scraping/config/sources/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSource)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Scraping source with ID 999 not found"));

        verify(configurationService).updateSource(eq(999), any(ScrapingSource.class));
    }

    @Test
    void deleteSource_WithExistingId_ShouldDeleteSuccessfully() throws Exception {
        // Given
        doNothing().when(configurationService).deleteSource(1);

        // When & Then
        mockMvc.perform(delete("/api/scraping/config/sources/1")
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(configurationService).deleteSource(1);
    }

    @Test
    void deleteSource_WithNonExistentId_ShouldReturnBadRequest() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Scraping source with ID 999 not found"))
                .when(configurationService).deleteSource(999);

        // When & Then
        mockMvc.perform(delete("/api/scraping/config/sources/999")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Scraping source with ID 999 not found"));

        verify(configurationService).deleteSource(999);
    }

    @Test
    void toggleSourceStatus_ShouldToggleSuccessfully() throws Exception {
        // Given
        ScrapingSource disabledSource = new ScrapingSource();
        disabledSource.setId(1);
        disabledSource.setName("DevPost");
        disabledSource.setEnabled(false);
        
        when(configurationService.toggleSourceStatus(1, false)).thenReturn(disabledSource);

        // When & Then
        mockMvc.perform(patch("/api/scraping/config/sources/1/status")
                .with(csrf())
                .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.enabled").value(false));

        verify(configurationService).toggleSourceStatus(1, false);
    }

    @Test
    void validateSource_WithValidSource_ShouldReturnValid() throws Exception {
        // Given
        doNothing().when(configurationService).validateScrapingSource(any(ScrapingSource.class));

        // When & Then
        mockMvc.perform(post("/api/scraping/config/sources/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSource)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Configuration is valid"));

        verify(configurationService).validateScrapingSource(any(ScrapingSource.class));
    }

    @Test
    void validateSource_WithInvalidSource_ShouldReturnInvalid() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Source name cannot be null or empty"))
                .when(configurationService).validateScrapingSource(any(ScrapingSource.class));

        // When & Then
        mockMvc.perform(post("/api/scraping/config/sources/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSource)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Source name cannot be null or empty"));

        verify(configurationService).validateScrapingSource(any(ScrapingSource.class));
    }

    @Test
    void getConfigurationSummary_ShouldReturnSummary() throws Exception {
        // Given
        Map<String, Object> summary = Map.of(
            "totalSources", 2,
            "enabledSources", 1L,
            "disabledSources", 1L,
            "sources", Arrays.asList(validSource)
        );
        when(configurationService.getConfigurationSummary()).thenReturn(summary);

        // When & Then
        mockMvc.perform(get("/api/scraping/config/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalSources").value(2))
                .andExpect(jsonPath("$.enabledSources").value(1))
                .andExpect(jsonPath("$.disabledSources").value(1));

        verify(configurationService).getConfigurationSummary();
    }

    @Test
    void reloadConfiguration_ShouldReloadSuccessfully() throws Exception {
        // Given
        doNothing().when(configurationService).reloadConfiguration();

        // When & Then
        mockMvc.perform(post("/api/scraping/config/reload")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Configuration reloaded successfully"));

        verify(configurationService).reloadConfiguration();
    }

    @Test
    void reloadConfiguration_WhenFails_ShouldReturnError() throws Exception {
        // Given
        doThrow(new RuntimeException("Configuration reload failed"))
                .when(configurationService).reloadConfiguration();

        // When & Then
        mockMvc.perform(post("/api/scraping/config/reload")
                .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Failed to reload configuration"));

        verify(configurationService).reloadConfiguration();
    }
}