package com.gamecheck.service;

import com.gamecheck.dto.WishlistItemDto;
import com.gamecheck.exception.ResourceNotFoundException;
import com.gamecheck.exception.WishlistAlreadyExistsException;
import com.gamecheck.model.Game;
import com.gamecheck.model.User;
import com.gamecheck.model.Wishlist;
import com.gamecheck.repository.GameRepository;
import com.gamecheck.repository.PriceRepository;
import com.gamecheck.repository.UserRepository;
import com.gamecheck.repository.WishlistRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final PriceRepository priceRepository;

    @Transactional(readOnly = true)
    public List<WishlistItemDto> getWishlist(Integer userId) {
        assertActiveUser(userId);
        List<Wishlist> entries = wishlistRepository.findByUserIdWithGame(userId);
        List<WishlistItemDto> out = new ArrayList<>(entries.size());
        for (Wishlist w : entries) {
            out.add(toDto(w));
        }
        return out;
    }

    @Transactional
    public WishlistItemDto addToWishlist(Integer userId, Integer gameId, BigDecimal alertThreshold) {
        assertActiveUser(userId);
        validateAlertThreshold(alertThreshold);

        if (wishlistRepository.findByUser_UserIdAndGame_GameId(userId, gameId).isPresent()) {
            throw new WishlistAlreadyExistsException("This game is already on your wishlist.");
        }

        Game game =
                gameRepository
                        .findById(gameId)
                        .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + gameId));

        User user =
                userRepository
                        .findById(userId)
                        .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Wishlist entry =
                Wishlist.builder()
                        .user(user)
                        .game(game)
                        .priceAlertThreshold(alertThreshold)
                        .addedAt(LocalDateTime.now())
                        .build();
        return toDto(wishlistRepository.save(entry));
    }

    @Transactional
    public void removeFromWishlist(Integer userId, Integer gameId) {
        assertActiveUser(userId);
        if (!gameRepository.existsById(gameId)) {
            throw new ResourceNotFoundException("Game not found: " + gameId);
        }
        int deleted = wishlistRepository.deleteByUser_UserIdAndGame_GameId(userId, gameId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Wishlist entry not found for game: " + gameId);
        }
    }

    private void assertActiveUser(Integer userId) {
        userRepository
                .findById(userId)
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private static void validateAlertThreshold(BigDecimal alertThreshold) {
        if (alertThreshold == null) {
            return;
        }
        if (alertThreshold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("priceAlertThreshold must be greater than zero");
        }
    }

    private WishlistItemDto toDto(Wishlist w) {
        Integer gameId = w.getGame().getGameId();
        return WishlistItemDto.builder()
                .wishlistId(w.getWishlistId())
                .gameId(gameId)
                .title(w.getGame().getGameTitle())
                .platform(w.getGame().getPlatform())
                .priceAlertThreshold(w.getPriceAlertThreshold())
                .addedAt(w.getAddedAt())
                .bestPricePhp(priceRepository.findMinPricePhpByGame_GameId(gameId).orElse(null))
                .build();
    }
}
