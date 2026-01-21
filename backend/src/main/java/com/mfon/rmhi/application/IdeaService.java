package com.mfon.rmhi.application;

import com.mfon.rmhi.api.dto.IdeaResponseDTO;
import com.mfon.rmhi.api.dto.TransformedIdeaResponseDTO;
import com.mfon.rmhi.application.mapper.IdeaMapper;
import com.mfon.rmhi.domain.Category;
import com.mfon.rmhi.domain.Comment;
import com.mfon.rmhi.domain.Idea;
import com.mfon.rmhi.domain.Technology;
import com.mfon.rmhi.infrastructure.persistence.CommentRepository;
import com.mfon.rmhi.infrastructure.persistence.IdeaRepository;
import com.mfon.rmhi.infrastructure.persistence.IdeaTechnologyRepository;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Array;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class IdeaService {
    private final IdeaRepository ideaRepository;
    private final IdeaTechnologyRepository ideaTechnologyRepository;
    private final IdeaMapper ideaMapper;
    private final CommentRepository commentRepository;
    private final JdbcTemplate jdbcTemplate;

    public IdeaService(
            IdeaRepository ideaRepository,
            IdeaTechnologyRepository ideaTechnologyRepository,
            IdeaMapper ideaMapper,
            CommentRepository commentRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.ideaRepository = ideaRepository;
        this.ideaTechnologyRepository = ideaTechnologyRepository;
        this.ideaMapper = ideaMapper;
        this.commentRepository = commentRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Long createIdea(Idea ideaToSave) {
        List<String> tags = ideaToSave.getTechnologies()
                .stream()
                .map(Technology::getName)
                .toList();

        List<String> categories = ideaToSave.getCategories()
                .stream()
                .map(Category::getName)
                .toList();

        ideaToSave.setTechnologies(new java.util.HashSet<>());
        ideaToSave.setCategories(new java.util.HashSet<>());

        Idea idea = ideaRepository.save(ideaToSave);

        ideaTechnologyRepository.addTechnologiesToIdea(
                idea.getId(),
                tags.toArray(new String[0])
        );

        ideaRepository.addTagsToIdea(idea.getId(), categories.toArray(new String[0]));

        return idea.getId();
    }

    public List<IdeaResponseDTO> getIdeas() {
        List<Idea> ideas = ideaRepository.findAll();
        List<Comment> comments = commentRepository.findAll();

        Map<Long, List<Comment>> commentsByIdeaId = comments.stream()
                .collect(Collectors.groupingBy(Comment::getIdeaId));

        return ideas.stream()
                .map(ideaMapper::toDto)
                .map(responseDto -> {
                    responseDto.setComments(
                            commentsByIdeaId.getOrDefault(responseDto.getId(), List.of())
                    );
                    return responseDto;
                })
                .toList();
    }

    public List<TransformedIdeaResponseDTO> getTransformedIdeas() {
        String sql = """
                SELECT id,
                       project_name,
                       short_description,
                       created_by,
                       problem_description,
                       solution,
                       technical_details,
                       likes,
                       technologies,
                       categories,
                       rating
                FROM ingest.transformed_project
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TransformedIdeaResponseDTO dto = new TransformedIdeaResponseDTO();
            dto.setId(rs.getLong("id"));
            dto.setProjectName(rs.getString("project_name"));
            dto.setShortDescription(rs.getString("short_description"));
            dto.setCreatedBy(defaultIfBlank(rs.getString("created_by"), "anonymous"));
            dto.setProblemDescription(rs.getString("problem_description"));
            dto.setSolution(rs.getString("solution"));
            dto.setTechnicalDetails(rs.getString("technical_details"));

            Integer likes = (Integer) rs.getObject("likes");
            dto.setLikes(likes == null ? 0 : likes);

            dto.setTechnologies(arrayToList(rs.getArray("technologies")));
            dto.setCategories(arrayToList(rs.getArray("categories")));

            BigDecimal rating = rs.getBigDecimal("rating");
            dto.setRating(rating == null ? 0.0 : rating.doubleValue());
            return dto;
        });
    }

    private static List<String> arrayToList(Array array) {
        if (array == null) {
            return List.of();
        }
        try {
            String[] values = (String[]) array.getArray();
            if (values == null) {
                return List.of();
            }
            return Arrays.stream(values)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
