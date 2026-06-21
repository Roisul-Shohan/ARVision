package com.ARVision.repository;

import com.ARVision.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartCartIdAndProductProductId(
            Long cartId, Long productId);

    void deleteByCartCartId(Long cartId);
}