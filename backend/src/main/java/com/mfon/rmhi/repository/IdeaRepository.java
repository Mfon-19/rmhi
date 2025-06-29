package com.mfon.rmhi.repository;

import com.mfon.rmhi.model.Idea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaRepository extends JpaRepository<Idea, Long> {
}
