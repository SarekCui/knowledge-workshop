package com.knowledge.iam.identity.bo;

public record RefreshTokenRotationBO(boolean accepted, RefreshTokenBO token) {

    public static RefreshTokenRotationBO accepted(RefreshTokenBO token) {
        return new RefreshTokenRotationBO(true, token);
    }

    public static RefreshTokenRotationBO rejected() {
        return new RefreshTokenRotationBO(false, null);
    }
}
