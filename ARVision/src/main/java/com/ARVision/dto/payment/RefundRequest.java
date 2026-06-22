package com.ARVision.dto.payment;

import lombok.Data;

@Data
public class RefundRequest {
    private String reason;  // optional reason for refund
}