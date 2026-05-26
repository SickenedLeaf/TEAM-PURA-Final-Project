package com.gamecheck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeSingleResponse {
    private int nintendoResultsCount;
    private int matchedDbGamesCount;
    private int newGamesCreatedCount;
    private List<String> matchedGameTitles;
    private List<String> createdGameTitles;
}
