package com.ARVision.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDto {

    @NotBlank(message = "Division is required")
    private String division;

    @NotBlank(message = "Zilla is required")
    private String zilla;

    @NotBlank(message = "Upazilla is required")
    private String upazilla;

    private String detailAddress; // house, road, area etc

    // Convert to single string for DB storage
    public String toFormattedString() {
        return detailAddress + ", " + upazilla + ", " + zilla + ", " + division;
    }

    // Parse from DB string back to DTO
    public static AddressDto fromString(String address) {
        if (address == null) return null;
        String[] parts = address.split(", ");
        AddressDto dto = new AddressDto();
        if (parts.length >= 4) {
            dto.setDetailAddress(parts[0]);
            dto.setUpazilla(parts[1]);
            dto.setZilla(parts[2]);
            dto.setDivision(parts[3]);
        } else {
            dto.setDetailAddress(address);
        }
        return dto;
    }
}