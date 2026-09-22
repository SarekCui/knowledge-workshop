package com.knowledge.security.config;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Transitional decoder: existing user sessions use HS256 while workload tokens use IAM's RS256 JWKS.
 *
 * <p>Only IAM owns the RSA private key. Resource services receive and cache its public JWKs.</p>
 */
final class HybridJwtDecoder implements JwtDecoder {

    private final JwtDecoder userTokenDecoder;
    private final JwtDecoder workloadTokenDecoder;

    HybridJwtDecoder(JwtDecoder userTokenDecoder, JwtDecoder workloadTokenDecoder) {
        this.userTokenDecoder = userTokenDecoder;
        this.workloadTokenDecoder = workloadTokenDecoder;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            String algorithm = SignedJWT.parse(token).getHeader().getAlgorithm().getName();
            if ("RS256".equals(algorithm)) {
                return workloadTokenDecoder.decode(token);
            }
            if ("HS256".equals(algorithm)) {
                return userTokenDecoder.decode(token);
            }
            throw new JwtException("Unsupported JWT signing algorithm");
        } catch (java.text.ParseException exception) {
            throw new JwtException("Malformed JWT", exception);
        }
    }
}
