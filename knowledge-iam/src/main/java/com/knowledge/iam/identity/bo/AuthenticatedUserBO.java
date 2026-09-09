package com.knowledge.iam.identity.bo;

import java.util.List;

public record AuthenticatedUserBO(String userId, String username, List<String> roles) {

    public AuthenticatedUserBO {
        roles = List.copyOf(roles);
    }
}
