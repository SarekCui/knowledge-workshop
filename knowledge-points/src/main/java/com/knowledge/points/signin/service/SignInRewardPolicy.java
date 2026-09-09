package com.knowledge.points.signin.service;

import org.springframework.stereotype.Component;

@Component
public class SignInRewardPolicy {

    public int reward(int continuousDays) {
        return 10 + Math.min(Math.max(continuousDays - 1, 0), 6) * 2;
    }
}
