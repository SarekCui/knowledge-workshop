package com.knowledge.api.iam.client;

import com.knowledge.api.iam.dto.OAuth2AccessTokenDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/** Standard OAuth 2.0 token endpoint contract exposed by IAM for internal workloads. */
@FeignClient(name = "knowledge-iam", contextId = "iamOAuthFeignClient", path = "/oauth2")
public interface IamOAuthFeignClient {

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    OAuth2AccessTokenDTO clientCredentials(
            @RequestHeader("Authorization") String authorization,
            @RequestBody String formBody);
}
