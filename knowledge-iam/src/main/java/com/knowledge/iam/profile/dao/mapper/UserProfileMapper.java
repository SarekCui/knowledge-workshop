package com.knowledge.iam.profile.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.iam.profile.dao.model.UserProfileDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfileDO> {

    @Insert("INSERT IGNORE INTO user_profile "
            + "(user_id, nickname, avatar_object_key, bio, version, created_at, updated_at) "
            + "SELECT id, username, NULL, NULL, 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3) "
            + "FROM user_account WHERE id = #{userId}")
    int insertDefault(@Param("userId") String userId);

    @Update("UPDATE user_profile SET nickname = #{nickname}, bio = #{bio}, "
            + "version = version + 1, updated_at = UTC_TIMESTAMP(3) "
            + "WHERE user_id = #{userId} AND version = #{version}")
    int updateDetails(@Param("userId") String userId, @Param("nickname") String nickname,
                      @Param("bio") String bio, @Param("version") int version);

    @Update("UPDATE user_profile SET avatar_object_key = #{objectKey}, "
            + "version = version + 1, updated_at = UTC_TIMESTAMP(3) "
            + "WHERE user_id = #{userId} AND version = #{version}")
    int updateAvatar(@Param("userId") String userId, @Param("objectKey") String objectKey,
                     @Param("version") int version);
}
