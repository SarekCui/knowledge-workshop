package com.knowledge.points.signin.bo;

import java.time.LocalDate;
import java.util.List;

public record SignInMonthBO(String month, LocalDate today, List<LocalDate> signedDates) {
}
