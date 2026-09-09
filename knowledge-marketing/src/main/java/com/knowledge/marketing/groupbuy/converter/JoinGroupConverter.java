package com.knowledge.marketing.groupbuy.converter;

import com.knowledge.marketing.groupbuy.bo.JoinGroupBO;
import com.knowledge.marketing.groupbuy.dto.JoinGroupDTO;

public final class JoinGroupConverter {

    private JoinGroupConverter() {
    }

    public static JoinGroupBO toBO(JoinGroupDTO request, String authenticatedUserId) {
        return new JoinGroupBO(request.requestId(), request.activityId(), request.groupId(), authenticatedUserId);
    }
}
