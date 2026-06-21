package com.ARVision.repository;

import com.ARVision.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartCartIdAndProductProductId(
            Long cartId, Long productId);

    @Modifying                          // ← add this
    @Transactional                      // ← add this
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cartId")
    void deleteByCartCartId(@Param("cartId") Long cartId);
}