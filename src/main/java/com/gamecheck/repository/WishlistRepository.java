package com.gamecheck.repository;

import com.gamecheck.model.Wishlist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

    List<Wishlist> findByUser_UserId(Integer userId);

    @Query("SELECT w FROM Wishlist w JOIN FETCH w.game WHERE w.user.userId = :userId ORDER BY w.addedAt DESC")
    List<Wishlist> findByUserIdWithGame(@Param("userId") Integer userId);

    Optional<Wishlist> findByUser_UserIdAndGame_GameId(Integer userId, Integer gameId);

    @Query(
            "SELECT w FROM Wishlist w JOIN FETCH w.user JOIN FETCH w.game WHERE w.priceAlertThreshold IS NOT NULL")
    List<Wishlist> findAllWithPriceAlertThreshold();

    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.user.userId = :userId AND w.game.gameId = :gameId")
    int deleteByUser_UserIdAndGame_GameId(@Param("userId") Integer userId, @Param("gameId") Integer gameId);
}
