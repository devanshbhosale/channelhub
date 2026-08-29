package com.devanshbhosale.channelhub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devanshbhosale.channelhub.domain.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByOrderId(Long orderId);
}
