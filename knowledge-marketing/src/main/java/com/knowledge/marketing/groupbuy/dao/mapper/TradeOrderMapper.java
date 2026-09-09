package com.knowledge.marketing.groupbuy.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.marketing.groupbuy.dao.model.TradeOrderDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface TradeOrderMapper extends BaseMapper<TradeOrderDO> {

    @Update("""
            UPDATE trade_order
               SET status = 'PAID', payment_trade_no = #{paymentTradeNo}, paid_at = #{now}, updated_at = #{now}
             WHERE id = #{orderId} AND status = 'PENDING_PAYMENT'
            """)
    int markPaid(@Param("orderId") String orderId, @Param("paymentTradeNo") String paymentTradeNo,
                 @Param("now") LocalDateTime now);

    @Update("""
            UPDATE trade_order SET status = 'CLOSED', updated_at = #{now}
             WHERE group_id = #{groupId} AND user_id = #{userId} AND status = 'PENDING_PAYMENT'
            """)
    int closePendingByGroupAndUser(@Param("groupId") String groupId, @Param("userId") String userId,
                                   @Param("now") LocalDateTime now);
}
