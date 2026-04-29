package com.gymrat.dto;

import java.time.LocalDate;

public record AdminMemberDto(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate joinDate,
        String status,
        Long membershipPlanId,
        String membershipPlanName,
        String paymentMethod,
        String cardNumber,
        String cardExpiryDate,
        String streetAddress,
        String aptUnit,
        String city,
        String state,
        String zipCode,
        LocalDate nextPaymentDate
) {}
