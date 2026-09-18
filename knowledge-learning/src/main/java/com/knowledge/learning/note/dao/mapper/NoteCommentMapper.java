package com.knowledge.learning.note.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.note.dao.model.NoteCommentDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface NoteCommentMapper extends BaseMapper<NoteCommentDO> {

    @Select("""
            SELECT * FROM note_comment
             WHERE user_id = #{userId} AND client_request_id = #{clientRequestId}
             LIMIT 1
            """)
    NoteCommentDO findByRequest(@Param("userId") String userId,
                                @Param("clientRequestId") String clientRequestId);

    @Select("SELECT * FROM note_comment WHERE source_comment_id = #{sourceCommentId} LIMIT 1")
    NoteCommentDO findAgentReplyBySourceCommentId(@Param("sourceCommentId") String sourceCommentId);

    @Select("SELECT * FROM note_comment WHERE id = #{id} FOR UPDATE")
    NoteCommentDO selectForUpdate(@Param("id") String id);

    @Update("""
            UPDATE note_comment
               SET deleted = 1, version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0
            """)
    int softDelete(@Param("id") String id, @Param("userId") String userId,
                   @Param("version") int version, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE note_comment SET like_count = GREATEST(0, like_count + #{delta})
             WHERE id = #{id} AND deleted = 0
            """)
    int adjustLikeCount(@Param("id") String id, @Param("delta") int delta);

    @Select("SELECT COUNT(*) FROM note_comment WHERE parent_comment_id = #{parentId} AND deleted = 0")
    long countActiveReplies(@Param("parentId") String parentId);

}
