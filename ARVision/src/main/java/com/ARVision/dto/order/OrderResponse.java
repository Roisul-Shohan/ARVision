package com.ARVision.dto.order;

import com.ARVision.entity.Order;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Order.OrderStatus status;
    private boolean canCancel;           // false when status is SHIPPED or beyond
    private Float totalAmount;
    private AddressDto shippingAddress;
    private String contactNumber;
    private LocalDateTime orderDate;
    private LocalDateTime estimatedDelivery;
    private List<OrderItemResponse> items;

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long orderItemId;
        private Long productId;
        private String productName;
        private String productImage;
        private String category;
        private Integer quantity;
        private Float unitPrice;
        private Float subtotal;
        private boolean hasArModel;
        private String arModelUrl;
    }
}