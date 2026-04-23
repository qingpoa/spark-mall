package com.sparkleshop.common.security.jwt;

import lombok.Data;

@Data
public class TokenUser {

    private Long userId;
    private Integer userType;
    private String tokenId;
    private long issuedAtEpochMilli;
    private long expiresAtEpochMilli;
}
