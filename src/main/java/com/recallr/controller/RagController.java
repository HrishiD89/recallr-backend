package com.recallr.controller;

import com.recallr.dto.RagQueryRequest;
import com.recallr.dto.RagQueryResponse;
import com.recallr.model.User;
import com.recallr.repository.UserRepository;
import com.recallr.services.RagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final RagService ragService;
    private final UserRepository userRepository;

    public RagController(RagService ragService, UserRepository userRepository) {
        this.ragService = ragService;
        this.userRepository = userRepository;
    }

    @PostMapping("/query")
    public ResponseEntity<RagQueryResponse> query(
            @Valid @RequestBody RagQueryRequest request,
            Authentication auth
    ) {
        User user = currentUser(auth);
        return ResponseEntity.ok(ragService.query(request, user));
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
