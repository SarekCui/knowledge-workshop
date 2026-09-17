package com.knowledge.learning.note.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.note.dao.model.NoteLikeDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface NoteLikeMapper extends BaseMapper<NoteLikeDO> {

    @Delete("DELETE FROM note_like WHERE note_id = #{noteId} AND user_id = #{userId}")
    int deleteByUser(@Param("noteId") String noteId, @Param("userId") String userId);

    @Select("SELECT COUNT(*) FROM note_like WHERE note_id = #{noteId} AND user_id = #{userId}")
    long countByUser(@Param("noteId") String noteId, @Param("userId") String userId);
}
