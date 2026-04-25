package com.gymrat.dto;

public record PaymentUpdateDto(
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
