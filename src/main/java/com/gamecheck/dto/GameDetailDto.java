package com.gamecheck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameDetailDto {

    private Integer gameId;
    private String title;
    private String platform;
    private Integer metacriticScore;
    private String trailerUrl;
    private String coverImageUrl;
}
