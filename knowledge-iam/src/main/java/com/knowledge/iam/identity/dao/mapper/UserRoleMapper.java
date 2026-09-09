package com.knowledge.iam.identity.dao.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRoleMapper {

    @Select("SELECT r.code FROM iam_role r "
            + "JOIN iam_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} AND r.status = 'ENABLED' ORDER BY r.code")
    List<String> selectRoleCodesByUserId(@Param("userId") String userId);
}
