package com.gamecheck.controller;

import com.gamecheck.dto.AddWishlistRequest;
import com.gamecheck.dto.WishlistItemDto;
import com.gamecheck.service.WishlistService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemDto>> getWishlist(Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    @PostMapping("/{gameId}")
    public ResponseEntity<WishlistItemDto> addToWishlist(
            Authentication authentication,
            @PathVariable Integer gameId,
            @RequestBody(required = false) AddWishlistRequest request) {
        Integer userId = (Integer) authentication.getPrincipal();
        var threshold = request != null ? request.getPriceAlertThreshold() : null;
        WishlistItemDto created = wishlistService.addToWishlist(userId, gameId, threshold);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> removeFromWishlist(
            Authentication authentication, @PathVariable Integer gameId) {
        Integer userId = (Integer) authentication.getPrincipal();
        wishlistService.removeFromWishlist(userId, gameId);
        return ResponseEntity.noContent().build();
    }
}
