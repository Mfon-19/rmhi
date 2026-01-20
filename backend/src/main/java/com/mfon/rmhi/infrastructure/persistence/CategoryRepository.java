package com.mfon.rmhi.infrastructure.persistence;

import com.mfon.rmhi.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
