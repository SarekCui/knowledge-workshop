package com.knowledge.iam.identity.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.iam.identity.dao.model.UserAccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountDO> {

    @Select("SELECT id, username, password_hash, status, created_at, updated_at "
            + "FROM iam_user_account WHERE username = #{username} LIMIT 1")
    UserAccountDO selectByUsername(@Param("username") String username);
}
