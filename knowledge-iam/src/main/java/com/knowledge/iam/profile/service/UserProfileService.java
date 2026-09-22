package com.knowledge.iam.profile.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.iam.identity.dao.mapper.UserAccountMapper;
import com.knowledge.iam.identity.dao.model.UserAccountDO;
import com.knowledge.iam.profile.bo.AvatarImageBO;
import com.knowledge.iam.profile.bo.UserProfileBO;
import com.knowledge.iam.profile.dao.mapper.UserProfileMapper;
import com.knowledge.iam.profile.dao.model.UserProfileDO;
import com.knowledge.iam.profile.storage.AvatarObjectStorage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfileService {

    @Resource
    private UserProfileMapper userProfileMapper;
    @Resource
    private UserAccountMapper userAccountMapper;
    @Resource
    private UserProfileTransactionService transactionService;
    @Resource
    private AvatarImageService avatarImageService;
    @Resource
    private AvatarObjectStorage avatarObjectStorage;

    public UserProfileBO get(String userId) {
        ensureProfile(userId);
        UserProfileDO profile = userProfileMapper.selectById(userId);
        UserAccountDO account = userAccountMapper.selectById(userId);
        if (profile == null || account == null) throw BusinessException.notFound("用户不存在");
        return toBO(profile, account.getUsername());
    }

    public List<UserProfileBO> listPublic(List<String> userIds) {
        LinkedHashSet<String> ids = userIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) return List.of();
        if (ids.size() > 100) throw BusinessException.badRequest("单次最多查询100个用户");
        List<UserProfileDO> profiles = userProfileMapper.selectByIds(ids);
        Map<String, UserProfileDO> profileById = profiles.stream()
                .collect(Collectors.toMap(UserProfileDO::getUserId, Function.identity()));
        Map<String, UserAccountDO> accountById = userAccountMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(UserAccountDO::getId, Function.identity()));
        return ids.stream().map(id -> {
            UserAccountDO account = accountById.get(id);
            if (account == null) return null;
            UserProfileDO profile = profileById.get(id);
            if (profile == null) {
                profile = new UserProfileDO();
                profile.setUserId(id);
                profile.setNickname(account.getUsername());
                profile.setVersion(0);
            }
            return toBO(profile, account.getUsername());
        }).filter(profile -> profile != null).toList();
    }

    public UserProfileBO update(String userId, String nickname, String bio, int version) {
        ensureProfile(userId);
        String normalizedNickname = nickname.trim();
        String normalizedBio = bio == null || bio.isBlank() ? null : bio.trim();
        transactionService.updateDetails(userId, normalizedNickname, normalizedBio, version);
        return get(userId);
    }

    public UserProfileBO updateAvatar(String userId, MultipartFile file, int version) {
        ensureProfile(userId);
        UserProfileDO before = userProfileMapper.selectById(userId);
        AvatarImageBO image = avatarImageService.normalize(file);
        String newObjectKey = avatarObjectStorage.put(image);
        try {
            transactionService.updateAvatar(userId, newObjectKey, version);
        } catch (RuntimeException exception) {
            avatarObjectStorage.deleteQuietly(newObjectKey);
            throw exception;
        }
        avatarObjectStorage.deleteQuietly(before.getAvatarObjectKey());
        return get(userId);
    }

    private void ensureProfile(String userId) {
        if (userProfileMapper.selectById(userId) != null) return;
        if (userAccountMapper.selectById(userId) == null) throw BusinessException.notFound("用户不存在");
        userProfileMapper.insertDefault(userId);
    }

    private UserProfileBO toBO(UserProfileDO profile, String username) {
        return new UserProfileBO(profile.getUserId(), username, profile.getNickname(),
                avatarObjectStorage.temporaryUrl(profile.getAvatarObjectKey()), profile.getBio(), profile.getVersion());
    }
}
