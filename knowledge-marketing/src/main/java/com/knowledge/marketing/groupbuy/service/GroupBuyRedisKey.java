package com.knowledge.marketing.groupbuy.service;

import java.util.regex.Pattern;

final class GroupBuyRedisKey {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final String PREFIX = "kw:marketing:group:";
    static final String EXPIRY_GROUP_REGISTRY = PREFIX + "reservation-expiry-groups";

    private GroupBuyRedisKey() {
    }

    static String occupied(String groupId) {
        return groupPrefix(groupId) + ":occupied";
    }

    static String reservation(String groupId, String userId) {
        return groupPrefix(groupId) + ":reservation:" + safe(userId, "userId");
    }

    static String reservationExpiry(String groupId) {
        return groupPrefix(groupId) + ":reservation-expiry";
    }

    private static String groupPrefix(String groupId) {
        String safeGroupId = safe(groupId, "groupId");
        return PREFIX + '{' + safeGroupId + '}';
    }

    private static String safe(String id, String name) {
        if (id == null || !SAFE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException(name + " 只能包含字母、数字、点、下划线或连字符，长度为 1-64");
        }
        return id;
    }
}
