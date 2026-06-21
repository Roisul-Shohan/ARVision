package com.ARVision.dto.cart;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private Long cartId;
    private Long customerId;
    private List<CartItemResponse> items;
    private int totalItems;        // total number of products
    private int totalQuantity;     // total quantity of all items
    private Float totalAmount;

    @Data
    @Builder
    public static class CartItemResponse {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private String productImage;
        private String category;
        private Float unitPrice;
        private Integer quantity;
        private Float subtotal;
        private Integer availableStock;  // so frontend can show max quantity
        private boolean hasArModel;      // show AR button in cart
        private String arModelUrl;
    }
}