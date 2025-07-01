package com.mfon.rmhi.repository;

import com.mfon.rmhi.model.Technology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

public interface IdeaTechnologyRepository extends JpaRepository<Technology, Long> {
    @Procedure(name = "add_technologies_to_idea")
    void addTechnologiesToIdea(Long p_idea_id, String[] p_tags);
}
