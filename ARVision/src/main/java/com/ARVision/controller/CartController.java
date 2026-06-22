package com.ARVision.controller;

import com.ARVision.dto.cart.AddToCartRequest;
import com.ARVision.dto.cart.CartResponse;
import com.ARVision.dto.cart.UpdateCartItemRequest;
import com.ARVision.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")  // entire controller needs login
public class CartController {

    private final CartService cartService;

    // GET /api/customer/cart
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(cartService.getCart(email));
    }

    // POST /api/customer/cart
    @PostMapping
    public ResponseEntity<CartResponse> addToCart(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(email, request));
    }

    // PATCH /api/customer/cart/items/3
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @AuthenticationPrincipal String email,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(
                cartService.updateCartItem(email, cartItemId, request));
    }

    // DELETE /api/customer/cart/items/3
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @AuthenticationPrincipal String email,
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(cartService.removeFromCart(email, cartItemId));
    }

    // DELETE /api/customer/cart
    @DeleteMapping
    public ResponseEntity<String> clearCart(
            @AuthenticationPrincipal String email) {
        cartService.clearCart(email);
        return ResponseEntity.ok("Cart cleared successfully");
    }
}