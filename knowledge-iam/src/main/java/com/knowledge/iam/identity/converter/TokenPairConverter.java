package com.knowledge.iam.identity.converter;

import com.knowledge.iam.identity.bo.TokenPairBO;
import com.knowledge.iam.identity.vo.TokenPairVO;

public final class TokenPairConverter {

    private TokenPairConverter() {
    }

    public static TokenPairVO toVO(TokenPairBO tokenPair) {
        return new TokenPairVO(
                tokenPair.accessToken(),
                "Bearer",
                tokenPair.accessTokenExpiresInSeconds(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresInSeconds());
    }
}
