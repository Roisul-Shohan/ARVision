package com.ARVision.dto.order;

import com.ARVision.entity.Order;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private Order.OrderStatus status;

    private String note;  // optional admin note
}