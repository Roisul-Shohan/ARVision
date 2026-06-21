package com.ARVision.dto.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressCheckResponse {
    private boolean hasAddress;
    private AddressDto savedAddress;   // null if no address saved
    private String message;            // message for frontend to show user
}