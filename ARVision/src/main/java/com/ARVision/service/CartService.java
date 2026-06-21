package com.ARVision.service;

import com.ARVision.dto.cart.AddToCartRequest;
import com.ARVision.dto.cart.CartResponse;
import com.ARVision.dto.cart.UpdateCartItemRequest;
import com.ARVision.entity.*;
import com.ARVision.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ── Get customer from email ────────────────────────────────
    private Customer getCustomer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    // ── Get or create cart for customer ───────────────────────
    private Cart getOrCreateCart(Customer customer) {
        return cartRepository.findByCustomer(customer)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomer(customer);
                    newCart.setTotalAmount(0f);
                    return cartRepository.save(newCart);
                });
    }

    // ── Map cart to response ───────────────────────────────────
    private CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getCartItems()
                .stream()
                .map(item -> CartResponse.CartItemResponse.builder()
                        .cartItemId(item.getCartItemId())
                        .productId(item.getProduct().getProductId())
                        .productName(item.getProduct().getName())
                        .productImage(item.getProduct().getImageUrl())
                        .category(item.getProduct().getCategory())
                        .unitPrice(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .availableStock(item.getProduct().getStockQuantity())
                        .hasArModel(item.getProduct().getArModel() != null)
                        .arModelUrl(item.getProduct().getArModel() != null
                                ? item.getProduct().getArModel().getFileUrl()
                                : null)
                        .build())
                .toList();

        int totalQuantity = itemResponses.stream()
                .mapToInt(CartResponse.CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .customerId(cart.getCustomer().getUserId())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .totalQuantity(totalQuantity)
                .totalAmount(cart.getTotalAmount())
                .build();
    }

    // ── Recalculate cart total ─────────────────────────────────
    private void recalculateTotal(Cart cart) {
        float total = cart.getCartItems()
                .stream()
                .map(CartItem::getSubtotal)
                .reduce(0f, Float::sum);
        cart.setTotalAmount(total);
    }

    // ── Get cart
    @Transactional
    public CartResponse getCart(String email) {
        Customer customer = getCustomer(email);
        Cart cart = getOrCreateCart(customer);
        return toResponse(cart);
    }

    // ── Add item to cart ───────────────────────────────────────
    @Transactional
    public CartResponse addToCart(String email, AddToCartRequest request) {
        Customer customer = getCustomer(email);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check stock availability
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException(
                    "Only " + product.getStockQuantity() + " items available in stock"
            );
        }

        Cart cart = getOrCreateCart(customer);

        // Check if product already in cart → update quantity instead
        CartItem cartItem = cartItemRepository
                .findByCartCartIdAndProductProductId(
                        cart.getCartId(),
                        product.getProductId())
                .orElse(null);

        if (cartItem != null) {
            // Product exists in cart → increase quantity
            int newQuantity = cartItem.getQuantity() + request.getQuantity();

            // Check new total doesn't exceed stock
            if (newQuantity > product.getStockQuantity()) {
                throw new RuntimeException(
                        "Cannot add more. Only "
                                + product.getStockQuantity()
                                + " items available in stock"
                );
            }

            cartItem.setQuantity(newQuantity);
            cartItem.setSubtotal(cartItem.getPrice() * newQuantity);
        } else {
            // New item → create CartItem
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(product.getPrice());
            cartItem.setSubtotal(product.getPrice() * request.getQuantity());
            cart.getCartItems().add(cartItem);
        }

        cartItemRepository.save(cartItem);
        recalculateTotal(cart);
        cartRepository.save(cart);

        return toResponse(cart);
    }

    // ── Update cart item quantity ──────────────────────────────
    @Transactional
    public CartResponse updateCartItem(
            String email,
            Long cartItemId,
            UpdateCartItemRequest request) {

        Customer customer = getCustomer(email);
        Cart cart = getOrCreateCart(customer);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Security check — make sure item belongs to this customer's cart
        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized access to cart item");
        }

        // If quantity = 0 → remove item
        if (request.getQuantity() == 0) {
            cart.getCartItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            // Check stock
            if (request.getQuantity() > cartItem.getProduct().getStockQuantity()) {
                throw new RuntimeException(
                        "Only " + cartItem.getProduct().getStockQuantity()
                                + " items available in stock"
                );
            }

            cartItem.setQuantity(request.getQuantity());
            cartItem.setSubtotal(cartItem.getPrice() * request.getQuantity());
            cartItemRepository.save(cartItem);
        }

        recalculateTotal(cart);
        cartRepository.save(cart);

        return toResponse(cart);
    }

    // ── Remove specific item from cart ─────────────────────────
    @Transactional
    public CartResponse removeFromCart(String email, Long cartItemId) {
        Customer customer = getCustomer(email);
        Cart cart = getOrCreateCart(customer);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Security check
        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized access to cart item");
        }

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        recalculateTotal(cart);
        cartRepository.save(cart);

        return toResponse(cart);
    }

    // ── Clear entire cart ──────────────────────────────────────
    @Transactional
    public void clearCart(String email) {
        Customer customer = getCustomer(email);
        Cart cart = getOrCreateCart(customer);

        cartItemRepository.deleteByCartCartId(cart.getCartId());
        cart.getCartItems().clear();
        cart.setTotalAmount(0f);
        cartRepository.save(cart);
    }
}