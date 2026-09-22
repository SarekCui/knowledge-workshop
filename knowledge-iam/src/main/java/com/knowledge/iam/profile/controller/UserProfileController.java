package com.knowledge.iam.profile.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.iam.profile.converter.UserProfileConverter;
import com.knowledge.iam.profile.dto.UpdateUserProfileDTO;
import com.knowledge.iam.profile.service.UserProfileService;
import com.knowledge.iam.profile.vo.PublicUserProfileVO;
import com.knowledge.iam.profile.vo.UserProfileVO;
import com.knowledge.security.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/iam")
@Tag(name = "用户资料", description = "当前用户资料与公开作者信息")
public class UserProfileController {

    @Resource
    private UserProfileService userProfileService;

    @GetMapping("/profile")
    @Operation(summary = "查询当前用户资料")
    public Result<UserProfileVO> get(@Parameter(hidden = true) HttpServletRequest request) {
        return Result.ok(UserProfileConverter.toVO(userProfileService.get(UserContext.getUserId())), requestId(request));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新当前用户资料")
    public Result<UserProfileVO> update(@Valid @RequestBody UpdateUserProfileDTO profile,
                                        @Parameter(hidden = true) HttpServletRequest request) {
        return Result.ok(UserProfileConverter.toVO(userProfileService.update(UserContext.getUserId(),
                profile.nickname(), profile.bio(), profile.version())), requestId(request));
    }

    @PutMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传并替换当前用户头像")
    public Result<UserProfileVO> updateAvatar(
            @RequestPart("file") MultipartFile file,
            @RequestParam @Min(value = 0, message = "版本号不能小于0")
            @Max(value = Integer.MAX_VALUE, message = "版本号不合法") int version,
            @Parameter(hidden = true) HttpServletRequest request) {
        return Result.ok(UserProfileConverter.toVO(
                userProfileService.updateAvatar(UserContext.getUserId(), file, version)), requestId(request));
    }

    @GetMapping("/users/public")
    @Operation(summary = "批量查询公开用户资料")
    public Result<List<PublicUserProfileVO>> listPublic(
            @RequestParam @Size(max = 100, message = "单次最多查询100个用户") List<String> userIds,
            @Parameter(hidden = true) HttpServletRequest request) {
        return Result.ok(userProfileService.listPublic(userIds).stream()
                .map(UserProfileConverter::toPublicVO).toList(), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
