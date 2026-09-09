package com.knowledge.learning.entitlement.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.entitlement.dao.model.MessageInboxDO;
import org.apache.ibatis.annotations.Insert;

public interface MessageInboxMapper extends BaseMapper<MessageInboxDO> {

    @Insert("""
            INSERT IGNORE INTO lr_message_inbox (id, consumer_name, event_id, processed_at)
            VALUES (#{id}, #{consumerName}, #{eventId}, #{processedAt})
            """)
    int insertIgnore(MessageInboxDO inbox);
}
