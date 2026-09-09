package com.knowledge.points.signin.converter;

import com.knowledge.points.signin.bo.SignInBO;
import com.knowledge.points.signin.dao.model.SignInRecordDO;
import com.knowledge.points.signin.vo.SignInVO;

public final class SignInConverter {

    private SignInConverter() {
    }

    public static SignInBO toBO(SignInRecordDO record, boolean duplicated) {
        return new SignInBO(record.getUserId(), record.getSignDate(), record.getContinuousDays(),
                record.getRewardPoints(), duplicated);
    }

    public static SignInVO toVO(SignInBO signIn) {
        return new SignInVO(signIn.userId(), signIn.signDate(), signIn.continuousDays(),
                signIn.rewardPoints(), signIn.duplicated());
    }
}
