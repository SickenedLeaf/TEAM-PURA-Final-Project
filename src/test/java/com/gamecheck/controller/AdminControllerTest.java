package com.gamecheck.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamecheck.dto.ScrapeSingleRequest;
import com.gamecheck.dto.ScrapeSingleResponse;
import com.gamecheck.service.AggregationService;
import com.gamecheck.service.NintendoAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.liquibase.enabled=false"
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AggregationService aggregationService;

    @MockBean
    private NintendoAggregationService nintendoAggregationService;

    @Test
    void testTriggerAggregation_Success() throws Exception {
        // Arrange
        doNothing().when(aggregationService).runFullUpdate();
        doNothing().when(nintendoAggregationService).aggregateEShopPrices();

        // Act & Assert
        mockMvc.perform(post("/api/admin/trigger-aggregation")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Aggregation triggered"));

        verify(aggregationService, times(1)).runFullUpdate();
        verify(nintendoAggregationService, times(1)).aggregateEShopPrices();
    }

    @Test
    void testScrapeSingle_Success() throws Exception {
        // Arrange
        ScrapeSingleRequest request = new ScrapeSingleRequest();
        request.setGameTitle("Bendy and the Ink Machine");

        ScrapeSingleResponse response = ScrapeSingleResponse.builder()
                .nintendoResultsCount(1)
                .matchedDbGamesCount(1)
                .newGamesCreatedCount(0)
                .matchedGameTitles(Collections.singletonList("Bendy and the Ink Machine"))
                .createdGameTitles(Collections.emptyList())
                .build();

        when(nintendoAggregationService.aggregateForTitle("Bendy and the Ink Machine"))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/admin/scrape-single")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nintendoResultsCount").value(1))
                .andExpect(jsonPath("$.matchedDbGamesCount").value(1))
                .andExpect(jsonPath("$.newGamesCreatedCount").value(0))
                .andExpect(jsonPath("$.matchedGameTitles[0]").value("Bendy and the Ink Machine"));

        verify(nintendoAggregationService, times(1)).aggregateForTitle("Bendy and the Ink Machine");
    }

    @Test
    void testScrapeSingle_CreateNewGame() throws Exception {
        // Arrange
        ScrapeSingleRequest request = new ScrapeSingleRequest();
        request.setGameTitle("New Digital Game");

        ScrapeSingleResponse response = ScrapeSingleResponse.builder()
                .nintendoResultsCount(1)
                .matchedDbGamesCount(0)
                .newGamesCreatedCount(1)
                .matchedGameTitles(Collections.emptyList())
                .createdGameTitles(Collections.singletonList("New Digital Game"))
                .build();

        when(nintendoAggregationService.aggregateForTitle("New Digital Game"))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/admin/scrape-single")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nintendoResultsCount").value(1))
                .andExpect(jsonPath("$.matchedDbGamesCount").value(0))
                .andExpect(jsonPath("$.newGamesCreatedCount").value(1))
                .andExpect(jsonPath("$.createdGameTitles[0]").value("New Digital Game"));

        verify(nintendoAggregationService, times(1)).aggregateForTitle("New Digital Game");
    }

    @Test
    void testScrapeSingle_ServiceThrowsException() throws Exception {
        // Arrange
        ScrapeSingleRequest request = new ScrapeSingleRequest();
        request.setGameTitle("Test Game");

        when(nintendoAggregationService.aggregateForTitle("Test Game"))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        mockMvc.perform(post("/api/admin/scrape-single")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(nintendoAggregationService, times(1)).aggregateForTitle("Test Game");
    }
}
