package com.devanshbhosale.channelhub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devanshbhosale.channelhub.domain.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByProductId(Long productId);

    Optional<Inventory> findByProductIdAndWarehouse(Long productId, String warehouse);

    List<Inventory> findByQuantityLessThanEqual(int quantity);
}
