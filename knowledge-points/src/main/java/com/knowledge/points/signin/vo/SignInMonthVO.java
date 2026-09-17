package com.knowledge.points.signin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record SignInMonthVO(
        @Schema(description = "查询月份，YYYY-MM") String month,
        @Schema(description = "服务端当前 UTC 日期") LocalDate today,
        @Schema(description = "本人该月已签到日期，按日期升序") List<LocalDate> signedDates) {
}
