package com.knowledge.iam.profile.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.iam.profile.dao.mapper.UserProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileTransactionService {

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Transactional
    public void updateDetails(String userId, String nickname, String bio, int version) {
        if (userProfileMapper.updateDetails(userId, nickname, bio, version) != 1) {
            throw BusinessException.conflict("个人资料已在其他设备更新，请刷新后重试");
        }
    }

    @Transactional
    public void updateAvatar(String userId, String objectKey, int version) {
        if (userProfileMapper.updateAvatar(userId, objectKey, version) != 1) {
            throw BusinessException.conflict("头像已在其他设备更新，请刷新后重试");
        }
    }
}
