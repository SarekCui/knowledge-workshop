package com.knowledge.learning.progress.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.progress.dao.model.WatchedSegmentDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WatchedSegmentMapper extends BaseMapper<WatchedSegmentDO> {

    @Insert("""
            INSERT IGNORE INTO watched_segment
              (id, user_id, video_id, video_version, segment_index, created_at)
            VALUES
              (#{id}, #{userId}, #{videoId}, #{videoVersion}, #{segmentIndex}, #{createdAt})
            """)
    int insertIgnore(WatchedSegmentDO segment);

    @Select("""
            SELECT COUNT(*) FROM watched_segment
             WHERE user_id = #{userId} AND video_id = #{videoId} AND video_version = #{videoVersion}
            """)
    long countSegments(@Param("userId") String userId, @Param("videoId") String videoId,
                       @Param("videoVersion") int videoVersion);
}
