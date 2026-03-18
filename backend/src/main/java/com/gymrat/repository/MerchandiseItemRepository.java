package com.gymrat.repository;

import com.gymrat.entity.MerchandiseItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchandiseItemRepository extends JpaRepository<MerchandiseItem, Long> {
}
