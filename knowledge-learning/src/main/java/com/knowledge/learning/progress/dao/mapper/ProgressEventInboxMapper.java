package com.knowledge.learning.progress.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.progress.dao.model.ProgressEventInboxDO;
import org.apache.ibatis.annotations.Insert;

public interface ProgressEventInboxMapper extends BaseMapper<ProgressEventInboxDO> {

    @Insert("""
            INSERT IGNORE INTO lr_progress_event_inbox (id, consumer_name, event_id, processed_at)
            VALUES (#{id}, #{consumerName}, #{eventId}, #{processedAt})
            """)
    int insertIgnore(ProgressEventInboxDO inbox);
}
