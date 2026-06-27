package com.recallr.controller;

import com.recallr.dto.request.ContentRequestDTO;
import com.recallr.dto.response.ContentResponseDTO;
import com.recallr.model.User;
import com.recallr.repository.UserRepository;
import com.recallr.service.content.ContentManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    private final ContentManagementService contentService;
    private final UserRepository userRepository;

    public ContentController(ContentManagementService contentService, UserRepository userRepository) {
        this.contentService = contentService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ContentResponseDTO> save(
            @Valid @RequestBody ContentRequestDTO request,
            Authentication auth
    ) {
        User user = currentUser(auth);
        return ResponseEntity.ok(contentService.save(request, user));
    }

    @GetMapping
    public ResponseEntity<List<ContentResponseDTO>> getAll(Authentication auth) {
        User user = currentUser(auth);
        return ResponseEntity.ok(contentService.findAll(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentResponseDTO> getById(@PathVariable Long id, Authentication auth) {
        User user = currentUser(auth);
        return ResponseEntity.ok(contentService.findById(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        User user = currentUser(auth);
        contentService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ContentResponseDTO> toggleRead(@PathVariable Long id, Authentication auth) {
        User user = currentUser(auth);
        return ResponseEntity.ok(contentService.toggleRead(id, user));
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
