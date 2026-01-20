package com.mfon.rmhi.infrastructure.persistence;

import com.mfon.rmhi.domain.Technology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

public interface IdeaTechnologyRepository extends JpaRepository<Technology, Long> {
    @Procedure("add_technologies_to_idea")
    void addTechnologiesToIdea(Long p_idea_id, String[] p_tags);
}
