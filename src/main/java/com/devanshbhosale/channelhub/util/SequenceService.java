package com.devanshbhosale.channelhub.util;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.time.Year;

/**
 * Human-friendly document numbers from database sequences (created by bootstrap,
 * works on H2 in PostgreSQL mode and on Postgres): CH-2026-00001 / INV-2026-00001.
 */
@Service
public class SequenceService {

    private final EntityManager entityManager;

    public SequenceService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public String nextOrderNumber() {
        return "CH-" + Year.now().getValue() + "-" + String.format("%05d", nextval("order_number_seq"));
    }

    public String nextInvoiceNumber() {
        return "INV-" + Year.now().getValue() + "-" + String.format("%05d", nextval("invoice_number_seq"));
    }

    private long nextval(String sequence) {
        Object result = entityManager
            .createNativeQuery("SELECT nextval('" + sequence + "')")
            .getSingleResult();
        return ((Number) result).longValue();
    }
}
