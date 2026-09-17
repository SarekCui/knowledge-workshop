package com.knowledge.iam.identity.converter;

import com.knowledge.iam.identity.bo.TokenPairBO;
import com.knowledge.iam.identity.vo.TokenPairVO;
import com.knowledge.iam.identity.vo.WebAccessTokenVO;

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

    public static WebAccessTokenVO toWebVO(TokenPairBO tokenPair) {
        return new WebAccessTokenVO(tokenPair.accessToken(), "Bearer", tokenPair.accessTokenExpiresInSeconds());
    }
}
