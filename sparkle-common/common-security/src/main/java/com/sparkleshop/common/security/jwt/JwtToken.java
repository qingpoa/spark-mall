package com.sparkleshop.common.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtToken {

    private final String token;
    private final String tokenId;
    private final long expiresIn;
}
