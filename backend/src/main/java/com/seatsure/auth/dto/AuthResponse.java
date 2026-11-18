package com.seatsure.auth.dto;

import com.seatsure.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        UserResponse user) {
}
