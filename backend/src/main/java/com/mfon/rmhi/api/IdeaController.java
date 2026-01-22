package com.mfon.rmhi.api;

import com.mfon.rmhi.api.dto.IdeaResponseDTO;
import com.mfon.rmhi.api.dto.TransformedIdeaResponseDTO;
import com.mfon.rmhi.api.request.CreateIdeaRequest;
import com.mfon.rmhi.api.response.IdeaIdResponse;
import com.mfon.rmhi.application.IdeaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IdeaController {
    private static final int MAX_LIMIT = 50;

    private final IdeaService ideaService;

    public IdeaController(IdeaService ideaService) {
        this.ideaService = ideaService;
    }

    @PostMapping("/create-idea")
    public ResponseEntity<IdeaIdResponse> createScrapedIdea(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateIdeaRequest request
    ) {
        Long id = ideaService.createIdea(request.idea());
        return ResponseEntity.status(HttpStatus.OK).body(new IdeaIdResponse(id));
    }

    @GetMapping("/get-ideas")
    public ResponseEntity<List<IdeaResponseDTO>> getIdeas(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.OK).body(ideaService.getIdeas());
    }

    @GetMapping("/get-transformed-ideas")
    public ResponseEntity<List<TransformedIdeaResponseDTO>> getTransformedIdeas(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "cursor", defaultValue = "0") int cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        int safeCursor = Math.max(cursor, 0);
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ideaService.getTransformedIdeas(safeCursor, safeLimit));
    }
}
