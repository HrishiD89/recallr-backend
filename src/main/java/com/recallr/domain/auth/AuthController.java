package com.recallr.domain.auth;

import com.recallr.domain.content.ContentRepository;
import com.recallr.domain.quota.QuotaService;
import com.recallr.domain.quota.UserQuota;
import com.recallr.domain.quota.UserQuotaRepository;

import com.recallr.domain.quota.UserTier;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AuthenticationManager authenticationManager;
    private final com.recallr.domain.quota.QuotaService quotaService;
    private final com.recallr.domain.content.ContentRepository contentRepository;
    private final com.recallr.domain.quota.UserQuotaRepository userQuotaRepository;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtEncoder jwtEncoder,
                          AuthenticationManager authenticationManager,
                          com.recallr.domain.quota.QuotaService quotaService,
                          com.recallr.domain.content.ContentRepository contentRepository,
                          com.recallr.domain.quota.UserQuotaRepository userQuotaRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.authenticationManager = authenticationManager;
        this.quotaService = quotaService;
        this.contentRepository = contentRepository;
        this.userQuotaRepository = userQuotaRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody AuthRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(403).body(new AuthResponse(null, "User already exists"));
        }

        User user = createUser(request);
        String token = generateToken(user);

        return ResponseEntity.ok(new AuthResponse(token, "Signed up"));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@Valid @RequestBody AuthRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = generateToken(user);

        return ResponseEntity.ok(new AuthResponse(token, null));
    }

    @GetMapping("/greeting")
    public ResponseEntity<String> greeting(){
        return ResponseEntity.ok("Hello");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        com.recallr.domain.quota.UserQuota quota = quotaService.getOrCreateTodayQuota(user);
        
        int bookmarkLimit = user.getTier() == UserTier.PRO ? Integer.MAX_VALUE : 20;
        int ragLimit = user.getTier() == UserTier.PRO ? Integer.MAX_VALUE : 10;

        long totalBookmarks = contentRepository.countByUser(user);
        long totalRagQueries = userQuotaRepository.sumRagQueriesByUser(user);

        return ResponseEntity.ok(new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getTier(),
                user.getShareToken(),
                quota.getBookmarkSavesUsed(),
                bookmarkLimit,
                quota.getRagQueriesUsed(),
                ragLimit,
                totalBookmarks,
                totalRagQueries
        ));
    }

    private String generateToken(User user){
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(24, ChronoUnit.HOURS))
                .subject(user.getUsername())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private User createUser(AuthRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        return userRepository.save(user);
    }

}
