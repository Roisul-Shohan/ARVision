package com.ARVision.controller;

import com.ARVision.dto.cart.AddToCartRequest;
import com.ARVision.dto.cart.CartResponse;
import com.ARVision.dto.cart.UpdateCartItemRequest;
import com.ARVision.dto.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.getCart(email),
                "Cart fetched successfully"));
    }

    // POST /api/customer/cart
    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.addToCart(email, request),
                "Item added to cart"));
    }

    // PATCH /api/customer/cart/items/3
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal String email,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.updateCartItem(email, cartItemId, request),
                "Cart item updated"));
    }

    // DELETE /api/customer/cart/items/3
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @AuthenticationPrincipal String email,
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.removeFromCart(email, cartItemId),
                "Item removed from cart"));
    }

    // DELETE /api/customer/cart
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal String email) {
        cartService.clearCart(email);
        return ResponseEntity.ok(ApiResponse.success(null,
                "Cart cleared successfully"));
    }
}