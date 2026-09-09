package com.knowledge.points.signin.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SignInRewardPolicyTest {

    private final SignInRewardPolicy policy = new SignInRewardPolicy();

    @Test
    void rewardsIncreaseForSevenDaysThenCap() {
        assertThat(policy.reward(1)).isEqualTo(10);
        assertThat(policy.reward(7)).isEqualTo(22);
        assertThat(policy.reward(30)).isEqualTo(22);
    }
}
