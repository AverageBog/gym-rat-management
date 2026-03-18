package com.gymrat.service;

import com.gymrat.dto.MerchandiseItemDto;
import com.gymrat.entity.MerchandiseItem;
import com.gymrat.repository.MerchandiseItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MerchandiseItemService {

    private final MerchandiseItemRepository repository;

    public List<MerchandiseItemDto> getAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public MerchandiseItemDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    public MerchandiseItemDto create(MerchandiseItemDto dto) {
        MerchandiseItem item = MerchandiseItem.builder()
                .name(dto.name())
                .quantity(dto.quantity())
                .price(dto.price())
                .description(dto.description())
                .build();
        return toDto(repository.save(item));
    }

    public MerchandiseItemDto update(Long id, MerchandiseItemDto dto) {
        MerchandiseItem item = findOrThrow(id);
        item.setName(dto.name());
        item.setQuantity(dto.quantity());
        item.setPrice(dto.price());
        item.setDescription(dto.description());
        return toDto(repository.save(item));
    }

    public MerchandiseItemDto adjustQuantity(Long id, int delta) {
        MerchandiseItem item = findOrThrow(id);
        int newQty = item.getQuantity() + delta;
        if (newQty < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity cannot go below zero");
        }
        item.setQuantity(newQty);
        return toDto(repository.save(item));
    }

    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private MerchandiseItem findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchandise item not found: " + id));
    }

    private MerchandiseItemDto toDto(MerchandiseItem item) {
        return new MerchandiseItemDto(item.getId(), item.getName(), item.getQuantity(), item.getPrice(), item.getDescription());
    }
}
