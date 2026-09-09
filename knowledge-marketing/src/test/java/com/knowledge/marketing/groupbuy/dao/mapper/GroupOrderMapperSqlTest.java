package com.knowledge.marketing.groupbuy.dao.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

class GroupOrderMapperSqlTest {

    @Test
    void computesFormedStateBeforeIncrementForMysqlLeftToRightAssignments() throws Exception {
        Method method = GroupOrderMapper.class.getMethod("confirmOne", String.class, java.time.LocalDateTime.class);
        String sql = String.join(" ", method.getAnnotation(Update.class).value());

        assertThat(sql.indexOf("status = CASE")).isLessThan(sql.indexOf("confirmed_count = confirmed_count + 1"));
    }
}
