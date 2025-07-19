package com.mfon.rmhi.repository;

import com.mfon.rmhi.model.IdeaTechnology;
import com.mfon.rmhi.model.IdeaTechnologyId;
import com.mfon.rmhi.model.Technology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

import java.util.List;

public interface IdeaTechnologyRepository extends JpaRepository<IdeaTechnology, IdeaTechnologyId> {
    @Procedure("add_technologies_to_idea")
    void addTechnologiesToIdea(Long p_idea_id, List<String> p_tags);
}
