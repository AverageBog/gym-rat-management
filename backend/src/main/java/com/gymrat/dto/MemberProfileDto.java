package com.gymrat.dto;

import java.time.LocalDate;

public record MemberProfileDto(
        Long id,
        String name,
        String email,
        String phone,
        String status,
        Long membershipPlanId,
        String membershipPlanName,
        LocalDate nextPaymentDate,
        String paymentMethod,
        String cardNumber,
        String cardExpiryDate,
        String cardCvv,
        String streetAddress,
        String aptUnit,
        String city,
        String state,
        String zipCode
) {}
