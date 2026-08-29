package com.devanshbhosale.channelhub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devanshbhosale.channelhub.domain.Reseller;

public interface ResellerRepository extends JpaRepository<Reseller, Long> {

    Optional<Reseller> findByUserId(Long userId);
}
