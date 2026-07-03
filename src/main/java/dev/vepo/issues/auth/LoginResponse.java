package dev.vepo.issues.auth;

public record LoginResponse(String token, String refreshToken, long expiresIn) {

}
