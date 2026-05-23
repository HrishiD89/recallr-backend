package com.recallr.controller;

import com.recallr.dto.ContentResponseDTO;
import com.recallr.model.User;
import com.recallr.repository.UserRepository;
import com.recallr.services.ContentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/share")
public class ShareController {

    private final UserRepository userRepository;
    private final ContentService contentService;

    public ShareController(UserRepository userRepository, ContentService contentService) {
        this.userRepository = userRepository;
        this.contentService = contentService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        String token = UUID.randomUUID().toString();
        user.setShareToken(token);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("shareUrl", "/share/" + token));
    }

    @GetMapping("/{token}")
    public ResponseEntity<List<ContentResponseDTO>> view(@PathVariable String token) {
        User user = userRepository.findByShareToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid link"));
        return ResponseEntity.ok(contentService.findAll(user));
    }

    @DeleteMapping("/revoke")
    public ResponseEntity<?> revoke(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        user.setShareToken(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Share link revoked"));
    }
}