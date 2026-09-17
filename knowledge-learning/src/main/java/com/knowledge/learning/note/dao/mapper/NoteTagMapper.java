package com.knowledge.learning.note.dao.mapper;

import com.knowledge.learning.note.dao.model.NoteTagDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface NoteTagMapper {

    @Insert("""
            INSERT INTO note_tag(note_id, normalized_name, display_name, sort_order, created_at)
            VALUES(#{noteId}, #{normalizedName}, #{displayName}, #{sortOrder}, #{createdAt})
            """)
    int insert(@Param("noteId") String noteId, @Param("normalizedName") String normalizedName,
            @Param("displayName") String displayName, @Param("sortOrder") int sortOrder,
            @Param("createdAt") LocalDateTime createdAt);

    @Delete("DELETE FROM note_tag WHERE note_id = #{noteId}")
    int deleteByNoteId(@Param("noteId") String noteId);

    @Select("""
            <script>
            SELECT note_id, normalized_name, display_name, sort_order, created_at
              FROM note_tag
             WHERE note_id IN
             <foreach collection="noteIds" item="noteId" open="(" separator="," close=")">
               #{noteId}
             </foreach>
             ORDER BY note_id, sort_order
            </script>
            """)
    List<NoteTagDO> selectByNoteIds(@Param("noteIds") List<String> noteIds);
}
