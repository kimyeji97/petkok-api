package com.petkok.global.security;

import java.util.UUID;

/** 인증된 사용자 주체. SecurityContext 의 principal 로 사용된다. */
public record AuthPrincipal(UUID userId) {}
