package com.sparkleshop.common.security.jwt;

import cn.hutool.core.util.IdUtil;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.redis.key.RedisKeys;
import com.sparkleshop.common.security.constant.SecurityConstants;
import com.sparkleshop.common.security.constant.SecurityErrorCodes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    public JwtToken generateToken(Long userId, Integer userType) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpirationSeconds());
        String tokenId = IdUtil.getSnowflakeNextIdStr();
        String token = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .id(tokenId)
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .claim(SecurityConstants.CLAIM_USER_TYPE, userType)
                .signWith(getSigningKey())
                .compact();
        return new JwtToken(token, tokenId, jwtProperties.getExpirationSeconds());
    }

    public TokenUser authenticate(String authorization) {
        String token = resolveToken(authorization);
        Claims claims = parseClaims(token);
        String tokenId = claims.getId();
        if (isBlacklisted(tokenId)) {
            throw new BusinessException(SecurityErrorCodes.TOKEN_REVOKED, "登录态已失效");
        }
        TokenUser tokenUser = new TokenUser();
        tokenUser.setUserId(claims.get(SecurityConstants.CLAIM_USER_ID, Long.class));
        tokenUser.setUserType(claims.get(SecurityConstants.CLAIM_USER_TYPE, Integer.class));
        tokenUser.setTokenId(tokenId);
        tokenUser.setExpiresAtEpochMilli(claims.getExpiration().getTime());
        return tokenUser;
    }

    public void blacklist(TokenUser tokenUser) {
        if (tokenUser == null || tokenUser.getTokenId() == null) {
            return;
        }
        long ttlMillis = tokenUser.getExpiresAtEpochMilli() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(getBlacklistKey(tokenUser.getTokenId()), "1", Duration.ofMillis(ttlMillis));
    }

    public String resolveToken(String authorization) {
        if (authorization == null || authorization.isBlank() || !authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
            throw new BusinessException(SecurityErrorCodes.UNAUTHORIZED, "未登录或登录态无效");
        }
        String token = authorization.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(SecurityErrorCodes.UNAUTHORIZED, "未登录或登录态无效");
        }
        return token;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw new BusinessException(SecurityErrorCodes.TOKEN_EXPIRED, "登录态已过期");
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(SecurityErrorCodes.UNAUTHORIZED, "未登录或登录态无效");
        }
    }

    private boolean isBlacklisted(String tokenId) {
        Boolean exists = stringRedisTemplate.hasKey(getBlacklistKey(tokenId));
        return Boolean.TRUE.equals(exists);
    }

    private String getBlacklistKey(String tokenId) {
        return RedisKeys.JWT_BLACKLIST + tokenId;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
