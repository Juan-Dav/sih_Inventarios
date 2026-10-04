package com.sih_inventarios_backend.repository;

import com.sih_inventarios_backend.entity.Sala;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaRepository extends JpaRepository<Sala, Long> {
}