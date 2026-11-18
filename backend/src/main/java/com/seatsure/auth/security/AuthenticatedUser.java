package com.seatsure.auth.security;

import com.seatsure.user.Role;

/** The identity extracted from a verified JWT; available in controllers via @AuthenticationPrincipal. */
public record AuthenticatedUser(Long userId, Role role) {
}
