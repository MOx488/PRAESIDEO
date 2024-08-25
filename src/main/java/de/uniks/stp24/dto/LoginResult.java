package de.uniks.stp24.dto;

public record LoginResult(
        String createdAt,
        String updatedAt,
        String _id,
        String name,
        String avatar,
        String accessToken,
        String refreshToken
) {
}
