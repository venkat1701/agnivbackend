package io.github.venkat1701.agnivbackend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtProvider {

    private static final int EXPIRATION = 846000000;
    private static final String TOKEN_PREFIX = "Bearer ";

    public String generateToken(Authentication authentication, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("email", authentication.getName())
                .claim("userId", userId)
                .signWith(Keys.hmacShaKeyFor(JwtConstants.SECRET_KEY.getBytes()))
                .compact();
    }
    public String getEmailFromToken(String jwt) {
        try {
            if (jwt.startsWith(TOKEN_PREFIX)) {
                jwt = jwt.substring(TOKEN_PREFIX.length());
            }

            SecretKey key = Keys.hmacShaKeyFor(JwtConstants.SECRET_KEY.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

            return claims.get("email", String.class); // Extract email from claims
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
    public Long getUserIdFromToken(String jwt) {
        try {
            if (jwt.startsWith(TOKEN_PREFIX)) {
                jwt = jwt.substring(TOKEN_PREFIX.length());
            }
            SecretKey key = Keys.hmacShaKeyFor(JwtConstants.SECRET_KEY.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

            return claims.get("userId", Long.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
}
