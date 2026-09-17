package com.knowledge.iam.profile.converter;

import com.knowledge.iam.profile.bo.UserProfileBO;
import com.knowledge.iam.profile.vo.PublicUserProfileVO;
import com.knowledge.iam.profile.vo.UserProfileVO;

public final class UserProfileConverter {

    private UserProfileConverter() {
    }

    public static UserProfileVO toVO(UserProfileBO profile) {
        return new UserProfileVO(profile.userId(), profile.username(), profile.nickname(),
                profile.avatarUrl(), profile.bio(), profile.version());
    }

    public static PublicUserProfileVO toPublicVO(UserProfileBO profile) {
        return new PublicUserProfileVO(profile.userId(), profile.nickname(), profile.avatarUrl());
    }
}
