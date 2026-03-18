package com.gymrat.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "merchandise_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchandiseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Min(0)
    private Integer quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String description;
}
