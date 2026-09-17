package com.knowledge.learning.note.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.enums.NoteStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface NoteMapper extends BaseMapper<NoteDO> {

    @Select("""
            SELECT * FROM note
             WHERE user_id = #{userId} AND client_request_id = #{clientRequestId}
             LIMIT 1
            """)
    NoteDO findByRequest(@Param("userId") String userId, @Param("clientRequestId") String clientRequestId);

    @Update("""
            UPDATE note
               SET title = #{title}, content = #{content}, video_position_ms = #{videoPositionMs},
                   version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0 AND status != 'PUBLIC'
            """)
    int updateContent(@Param("id") String id, @Param("userId") String userId,
                      @Param("title") String title, @Param("content") String content,
                      @Param("videoPositionMs") Long videoPositionMs, @Param("version") int version,
                      @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE note
               SET title = #{title}, version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0 AND status != 'PUBLIC'
            """)
    int rename(@Param("id") String id, @Param("userId") String userId, @Param("title") String title,
               @Param("version") int version, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE note
               SET deleted = 1, version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0
            """)
    int softDelete(@Param("id") String id, @Param("userId") String userId,
                   @Param("version") int version, @Param("updatedAt") LocalDateTime updatedAt);

    @Select("SELECT COUNT(*) FROM note WHERE chapter_id = #{chapterId} AND deleted = 0")
    long countActiveByChapter(@Param("chapterId") String chapterId);

    @Update("""
            UPDATE note SET status = #{status}, published_at = #{publishedAt},
                   version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0
            """)
    int changeStatus(@Param("id") String id, @Param("userId") String userId,
                     @Param("status") NoteStatus status, @Param("version") int version,
                     @Param("publishedAt") LocalDateTime publishedAt, @Param("updatedAt") LocalDateTime updatedAt);

    @Select("SELECT * FROM note WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    NoteDO selectActiveForUpdate(@Param("id") String id);

    @Update("""
            UPDATE note SET like_count = GREATEST(0, like_count + #{delta})
             WHERE id = #{id} AND deleted = 0
            """)
    int adjustLikeCount(@Param("id") String id, @Param("delta") int delta);

    @Update("""
            UPDATE note SET favorite_count = GREATEST(0, favorite_count + #{delta})
             WHERE id = #{id} AND deleted = 0
            """)
    int adjustFavoriteCount(@Param("id") String id, @Param("delta") int delta);

    @Update("""
            UPDATE note SET comment_count = GREATEST(0, comment_count + #{delta})
             WHERE id = #{id} AND deleted = 0
            """)
    int adjustCommentCount(@Param("id") String id, @Param("delta") int delta);

    @Select("""
            SELECT n.* FROM note n
            JOIN note_like l ON l.note_id = n.id
             WHERE l.user_id = #{userId} AND n.status = 'PUBLIC' AND n.deleted = 0
             ORDER BY l.created_at DESC, n.id DESC
             LIMIT #{limit} OFFSET #{offset}
            """)
    List<NoteDO> selectLiked(@Param("userId") String userId, @Param("offset") long offset,
                             @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*) FROM note n
            JOIN note_like l ON l.note_id = n.id
             WHERE l.user_id = #{userId} AND n.status = 'PUBLIC' AND n.deleted = 0
            """)
    long countLiked(@Param("userId") String userId);

    @Select("""
            SELECT n.* FROM note n
            JOIN note_favorite f ON f.note_id = n.id
             WHERE f.user_id = #{userId} AND n.status = 'PUBLIC' AND n.deleted = 0
             ORDER BY f.created_at DESC, n.id DESC
             LIMIT #{limit} OFFSET #{offset}
            """)
    List<NoteDO> selectFavorited(@Param("userId") String userId, @Param("offset") long offset,
                                 @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*) FROM note n
            JOIN note_favorite f ON f.note_id = n.id
             WHERE f.user_id = #{userId} AND n.status = 'PUBLIC' AND n.deleted = 0
            """)
    long countFavorited(@Param("userId") String userId);
}
