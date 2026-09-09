package com.knowledge.points.signin.bo;

import java.time.LocalDate;

public record SignInBO(
        String userId,
        LocalDate signDate,
        int continuousDays,
        int rewardPoints,
        boolean duplicated) {
}
