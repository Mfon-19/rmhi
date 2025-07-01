package com.mfon.rmhi.dto;

import java.util.List;

public record ScrapedIdeaRequest(
        String project_name,
        Integer likes,
        String submitted_to,
        Boolean winner,
        String created_by,
        String description,
        List<String> technologies
) {}
