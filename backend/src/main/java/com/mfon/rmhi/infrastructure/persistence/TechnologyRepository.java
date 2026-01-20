package com.mfon.rmhi.infrastructure.persistence;

import com.mfon.rmhi.domain.Technology;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnologyRepository extends JpaRepository<Technology, Long> {
}
