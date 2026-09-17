package com.knowledge.learning.note.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.note.dao.model.NoteImageDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface NoteImageMapper extends BaseMapper<NoteImageDO> {

    @Select("SELECT COUNT(*) FROM note_image WHERE user_id = #{userId} AND status = 'TEMP'")
    long countTemporary(@Param("userId") String userId);

    @Update("""
            UPDATE note_image
               SET note_id = #{noteId}, status = 'BOUND', expires_at = NULL, cleanup_token = NULL,
                   version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND user_id = #{userId}
               AND status IN ('TEMP', 'BOUND') AND (note_id IS NULL OR note_id = #{noteId})
            """)
    int bind(@Param("id") String id, @Param("userId") String userId, @Param("noteId") String noteId,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            <script>
            UPDATE note_image
               SET note_id = NULL, status = 'TEMP', expires_at = #{expiresAt}, cleanup_token = NULL,
                   version = version + 1, updated_at = #{updatedAt}
             WHERE note_id = #{noteId} AND status = 'BOUND'
            <if test="keptIds != null and !keptIds.isEmpty()">
               AND id NOT IN
               <foreach collection="keptIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </if>
            </script>
            """)
    int releaseMissing(@Param("noteId") String noteId, @Param("keptIds") List<String> keptIds,
            @Param("expiresAt") LocalDateTime expiresAt, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE note_image
               SET status = 'DELETING', cleanup_token = #{token}, version = version + 1, updated_at = #{now}
             WHERE status = 'TEMP' AND expires_at <= #{now}
             ORDER BY expires_at, id LIMIT #{limit}
            """)
    int claimExpired(@Param("token") String token, @Param("now") LocalDateTime now, @Param("limit") int limit);

    @Select("SELECT * FROM note_image WHERE cleanup_token = #{token} AND status = 'DELETING' ORDER BY id")
    List<NoteImageDO> selectClaimed(@Param("token") String token);

    @Delete("DELETE FROM note_image WHERE id = #{id} AND cleanup_token = #{token} AND status = 'DELETING'")
    int deleteClaimed(@Param("id") String id, @Param("token") String token);

    @Update("""
            UPDATE note_image
               SET status = 'TEMP', cleanup_token = NULL, expires_at = #{retryAt},
                   version = version + 1, updated_at = #{now}
             WHERE id = #{id} AND cleanup_token = #{token} AND status = 'DELETING'
            """)
    int retryClaimed(@Param("id") String id, @Param("token") String token,
            @Param("retryAt") LocalDateTime retryAt, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE note_image
               SET status = 'TEMP', cleanup_token = NULL, expires_at = #{now},
                   version = version + 1, updated_at = #{now}
             WHERE status = 'DELETING' AND updated_at <= #{staleBefore}
            """)
    int recoverStaleClaims(@Param("staleBefore") LocalDateTime staleBefore, @Param("now") LocalDateTime now);
}
