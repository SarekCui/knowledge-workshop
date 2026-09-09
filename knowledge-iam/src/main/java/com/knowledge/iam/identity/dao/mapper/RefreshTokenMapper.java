package com.knowledge.iam.identity.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.iam.identity.dao.model.RefreshTokenDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshTokenDO> {

    @Select("SELECT id, user_id, family_id, token_hash, status, expires_at, created_at, "
            + "rotated_at, revoked_at, replaced_by_id FROM refresh_token "
            + "WHERE token_hash = #{tokenHash} LIMIT 1 FOR UPDATE")
    RefreshTokenDO selectByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Update("UPDATE refresh_token SET status = 'ROTATED', rotated_at = #{rotatedAt}, "
            + "replaced_by_id = #{replacedById} WHERE id = #{id} AND status = 'ACTIVE'")
    int markRotated(
            @Param("id") String id,
            @Param("replacedById") String replacedById,
            @Param("rotatedAt") LocalDateTime rotatedAt);

    @Update("UPDATE refresh_token SET status = 'REVOKED', revoked_at = #{revokedAt} "
            + "WHERE family_id = #{familyId} AND status IN ('ACTIVE', 'ROTATED')")
    int revokeFamily(
            @Param("familyId") String familyId,
            @Param("revokedAt") LocalDateTime revokedAt);
}
