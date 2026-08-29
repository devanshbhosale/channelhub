package com.devanshbhosale.channelhub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devanshbhosale.channelhub.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
