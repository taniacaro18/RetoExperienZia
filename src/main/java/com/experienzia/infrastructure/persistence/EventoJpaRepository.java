package com.experienzia.infrastructure.persistence;

import com.experienzia.infrastructure.persistence.entity.EventoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoJpaRepository extends JpaRepository<EventoEntity, Long> {
}
