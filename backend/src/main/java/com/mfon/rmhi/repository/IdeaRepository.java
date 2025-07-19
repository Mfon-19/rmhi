package com.mfon.rmhi.repository;

import com.mfon.rmhi.model.Idea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IdeaRepository extends JpaRepository<Idea, Long> {
    @Procedure("add_tags_to_idea")
    void addTagsToIdea(Long p_idea_id, List<String> p_tags);

    /**
     * Check if an idea with the same project name and created by already exists
     */
    @Query("SELECT COUNT(i) > 0 FROM Idea i WHERE LOWER(i.projectName) = LOWER(:projectName) AND LOWER(i.createdBy) = LOWER(:createdBy)")
    boolean existsByProjectNameAndCreatedByIgnoreCase(@Param("projectName") String projectName, @Param("createdBy") String createdBy);
}
