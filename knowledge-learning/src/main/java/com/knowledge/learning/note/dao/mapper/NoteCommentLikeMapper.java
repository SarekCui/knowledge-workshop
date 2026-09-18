package com.knowledge.learning.note.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.note.dao.model.NoteCommentLikeDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface NoteCommentLikeMapper extends BaseMapper<NoteCommentLikeDO> {

    @Delete("DELETE FROM note_comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteByUser(@Param("commentId") String commentId, @Param("userId") String userId);

    @Delete("DELETE FROM note_comment_like WHERE comment_id = #{commentId}")
    int deleteByCommentId(@Param("commentId") String commentId);

    @Select("SELECT COUNT(*) FROM note_comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    long countByUser(@Param("commentId") String commentId, @Param("userId") String userId);

    @Select("""
            <script>
            SELECT comment_id FROM note_comment_like
             WHERE user_id = #{userId} AND comment_id IN
            <foreach collection='commentIds' item='commentId' open='(' separator=',' close=')'>
                #{commentId}
            </foreach>
            </script>
            """)
    List<String> selectLikedCommentIds(@Param("userId") String userId,
                                       @Param("commentIds") List<String> commentIds);
}
