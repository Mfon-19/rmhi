package com.mfon.rmhi.repository;

import com.mfon.rmhi.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
