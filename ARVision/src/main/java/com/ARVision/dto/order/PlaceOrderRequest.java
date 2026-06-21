package com.ARVision.dto.order;

import lombok.Data;

@Data
public class PlaceOrderRequest {
    private AddressDto shippingAddress;  // null = use saved address
    private boolean updateSavedAddress;  // true = save new address to profile
    private String contactNumber;
}