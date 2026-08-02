package com.recallr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads RSA keys from either:
 *   - Environment variable content (RSA_PUBLIC_KEY / RSA_PRIVATE_KEY) — used on Render
 *   - Classpath PEM files (rsa.public-key / rsa.private-key) — used locally
 *
 * The env-var path takes precedence when the variables are present and non-empty.
 */
@Configuration
public class RsaKeyConfig {

    /** Inline PEM content injected from env var RSA_PUBLIC_KEY (Render). May be empty. */
    @Value("${rsa.public-key-content:}")
    private String publicKeyContent;

    /** Inline PEM content injected from env var RSA_PRIVATE_KEY (Render). May be empty. */
    @Value("${rsa.private-key-content:}")
    private String privateKeyContent;

    /**
     * Provides the {@link RsaKeyProperties} record used by {@link com.recallr.config.security.JwtConfig}.
     * When RSA_PUBLIC_KEY / RSA_PRIVATE_KEY env vars are set (Render), parse them directly.
     * Otherwise, fall back to the {@link RsaKeyProperties} auto-configured from classpath files.
     */
    @Bean
    public RsaKeyProperties rsaKeyProperties(
            // Spring auto-converts classpath PEM → RSAPublicKey/RSAPrivateKey via @ConfigurationProperties
            @Value("${rsa.public-key:#{null}}") org.springframework.core.io.Resource publicKeyResource,
            @Value("${rsa.private-key:#{null}}") org.springframework.core.io.Resource privateKeyResource
    ) throws Exception {

        if (publicKeyContent != null && !publicKeyContent.isBlank()) {
            // ── Render / env-var path ─────────────────────────────────────────
            RSAPublicKey publicKey = parsePublicKey(publicKeyContent);
            RSAPrivateKey privateKey = parsePrivateKey(privateKeyContent);
            return new RsaKeyProperties(publicKey, privateKey);
        }

        // ── Local dev path — load from classpath PEM files ────────────────────
        if (publicKeyResource == null || privateKeyResource == null) {
            throw new IllegalStateException(
                    "No RSA keys configured. Set RSA_PUBLIC_KEY + RSA_PRIVATE_KEY env vars " +
                    "(Render) or rsa.public-key + rsa.private-key properties (local).");
        }

        // Re-use Spring's built-in PEM resource parsing
        String pubPem = new String(publicKeyResource.getInputStream().readAllBytes());
        String privPem = new String(privateKeyResource.getInputStream().readAllBytes());
        return new RsaKeyProperties(parsePublicKey(pubPem), parsePrivateKey(privPem));
    }

    // ── PEM parsing helpers ───────────────────────────────────────────────────

    private RSAPublicKey parsePublicKey(String pem) throws Exception {
        String b64 = stripPem(pem, "PUBLIC KEY");
        byte[] decoded = Base64.getDecoder().decode(b64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String b64 = stripPem(pem, "PRIVATE KEY");
        byte[] decoded = Base64.getDecoder().decode(b64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    /** Strips PEM header/footer and all whitespace to get raw Base64. */
    private String stripPem(String pem, String type) {
        return pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");
    }
}
