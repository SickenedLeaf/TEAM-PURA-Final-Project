package com.gamecheck.controller;

import com.gamecheck.dto.GameDetailDto;
import com.gamecheck.dto.GameSummaryDto;
import com.gamecheck.dto.PriceDto;
import com.gamecheck.service.GameService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/search")
    public ResponseEntity<List<GameSummaryDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String platform) {
        return ResponseEntity.ok(gameService.searchGames(query, platform));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDetailDto> getById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(gameService.getGameById(id));
    }

    /**
     * Price comparison for one game. Query params: {@code sort} = asc|desc (default asc), optional {@code sourceType}
     * = physical|digital.
     */
    @GetMapping("/{id}/prices")
    public ResponseEntity<List<PriceDto>> prices(
            @PathVariable("id") Integer id,
            @RequestParam(required = false, defaultValue = "asc") String sort,
            @RequestParam(required = false) String sourceType) {
        return ResponseEntity.ok(gameService.getPricesForGame(id, sort, sourceType));
    }
}
