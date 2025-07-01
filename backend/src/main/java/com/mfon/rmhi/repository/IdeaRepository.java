package com.mfon.rmhi.repository;

import com.mfon.rmhi.model.Idea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

public interface IdeaRepository extends JpaRepository<Idea, Long> {
    @Procedure("add_tags_to_idea")
    void addTagsToIdea(Long p_idea_id, String[] p_tags);
}
