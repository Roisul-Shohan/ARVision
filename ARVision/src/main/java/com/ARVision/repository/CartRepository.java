package com.ARVision.repository;

import com.ARVision.entity.Cart;
import com.ARVision.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomer(Customer customer);

    Optional<Cart> findByCustomerUserId(Long customerId);

    // Check if product already in cart
    @Query("""
        SELECT COUNT(ci) > 0 FROM CartItem ci
        WHERE ci.cart.customer.userId = :customerId
        AND ci.product.productId = :productId
    """)
    boolean existsProductInCart(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId
    );
    @Query("""
        SELECT DISTINCT c FROM Cart c
        LEFT JOIN FETCH c.cartItems ci
        LEFT JOIN FETCH ci.product p
        LEFT JOIN FETCH p.arModel
        WHERE c.customer.userId = :customerId
    """)
    Optional<Cart> findByCustomerWithItems(@Param("customerId") Long customerId);
}