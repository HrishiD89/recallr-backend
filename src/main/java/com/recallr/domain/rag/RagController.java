package com.recallr.domain.rag;

import com.recallr.domain.auth.User;
import com.recallr.domain.auth.UserRepository;

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

    private final RagQueryService ragService;
    private final UserRepository userRepository;

    public RagController(RagQueryService ragService, UserRepository userRepository) {
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
