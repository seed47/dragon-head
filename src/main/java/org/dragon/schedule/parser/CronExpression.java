package org.dragon.schedule.parser;

import lombok.Getter;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Cron 表达式解析器
 * 支持标准 Unix Cron 格式: 秒 分 时 日 月 周 年(可选)
 */
@Getter
public class CronExpression {

    // Cron 字段
    private final CronField seconds;
    private final CronField minutes;
    private final CronField hours;
    private final CronField daysOfMonth;
    private final CronField months;
    private final CronField daysOfWeek;
    private final CronField years;  // 可选

    private final String expression;

    // 默认值
    private static final int DEFAULT_SECONDS = 0;

    /**
     * 构造函数
     */
    private CronExpression(String expression,
                          CronField seconds, CronField minutes, CronField hours,
                          CronField daysOfMonth, CronField months, CronField daysOfWeek,
                          CronField years) {
        this.expression = expression;
        this.seconds = seconds;
        this.minutes = minutes;
        this.hours = hours;
        this.daysOfMonth = daysOfMonth;
        this.months = months;
        this.daysOfWeek = daysOfWeek;
        this.years = years;
    }

    /**
     * 解析 Cron 表达式
     *
     * @param expression Cron 表达式
     * @return CronExpression
     */
    public static CronExpression parse(String expression) {
        return parse(expression, ZoneId.systemDefault());
    }

    /**
     * 解析 Cron 表达式（带时区）
     *
     * @param expression Cron 表达式
     * @param zoneId 时区
     * @return CronExpression
     */
    public static CronExpression parse(String expression, ZoneId zoneId) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty");
        }

        String[] parts = expression.trim().split("\\s+");

        CronField seconds, minutes, hours, daysOfMonth, months, daysOfWeek, years = null;

        if (parts.length == 5) {
            // 标准 Unix Cron: 分 时 日 月 周
            seconds = new CronField(CronFieldType.SECOND, String.valueOf(DEFAULT_SECONDS));
            minutes = new CronField(CronFieldType.MINUTE, parts[0]);
            hours = new CronField(CronFieldType.HOUR, parts[1]);
            daysOfMonth = new CronField(CronFieldType.DAY_OF_MONTH, parts[2]);
            months = new CronField(CronFieldType.MONTH, parts[3]);
            daysOfWeek = new CronField(CronFieldType.DAY_OF_WEEK, parts[4]);
        } else if (parts.length == 6) {
            // 带秒的 Cron: 秒 分 时 日 月 周
            seconds = new CronField(CronFieldType.SECOND, parts[0]);
            minutes = new CronField(CronFieldType.MINUTE, parts[1]);
            hours = new CronField(CronFieldType.HOUR, parts[2]);
            daysOfMonth = new CronField(CronFieldType.DAY_OF_MONTH, parts[3]);
            months = new CronField(CronFieldType.MONTH, parts[4]);
            daysOfWeek = new CronField(CronFieldType.DAY_OF_WEEK, parts[5]);
        } else if (parts.length == 7) {
            // 带年和秒的 Cron: 秒 分 时 日 月 周 年
            seconds = new CronField(CronFieldType.SECOND, parts[0]);
            minutes = new CronField(CronFieldType.MINUTE, parts[1]);
            hours = new CronField(CronFieldType.HOUR, parts[2]);
            daysOfMonth = new CronField(CronFieldType.DAY_OF_MONTH, parts[3]);
            months = new CronField(CronFieldType.MONTH, parts[4]);
            daysOfWeek = new CronField(CronFieldType.DAY_OF_WEEK, parts[5]);
            years = new CronField(CronFieldType.YEAR, parts[6]);
        } else {
            throw new IllegalArgumentException("Invalid cron expression: " + expression + 
                ". Expected 5, 6, or 7 fields, but got " + parts.length);
        }

        return new CronExpression(expression,
                seconds, minutes, hours, daysOfMonth, months, daysOfWeek, years);
    }

    /**
     * 计算下次有效时间
     *
     * @param afterTime 从此时间之后开始计算
     * @return 下次有效时间戳，如果没有则返回 null
     */
    public Long getNextValidTimeAfter(long afterTime) {
        ZonedDateTime afterDateTime = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(afterTime), ZoneId.systemDefault());
        
        // 从下一秒开始计算
        ZonedDateTime nextTime = afterDateTime.plusSeconds(1).withNano(0);
        
        // 最大查找年数，防止无限循环
        int maxYears = 10;
        ZonedDateTime maxTime = nextTime.plusYears(maxYears);
        
        while (nextTime.isBefore(maxTime)) {
            if (matches(nextTime)) {
                return nextTime.toInstant().toEpochMilli();
            }
            nextTime = nextTime.plusSeconds(1);
        }
        
        return null;
    }

    /**
     * 计算多个未来的触发时间
     *
     * @param afterTime 从此时间之后开始计算
     * @param count 数量
     * @return 时间戳列表
     */
    public java.util.List<Long> getNextValidTimes(long afterTime, int count) {
        java.util.List<Long> times = new java.util.ArrayList<>();
        long currentTime = afterTime;
        
        for (int i = 0; i < count; i++) {
            Long nextTime = getNextValidTimeAfter(currentTime);
            if (nextTime == null) {
                break;
            }
            times.add(nextTime);
            currentTime = nextTime;
        }
        
        return times;
    }

    /**
     * 检查时间是否匹配 Cron 表达式
     *
     * @param dateTime 日期时间
     * @return 是否匹配
     */
    public boolean matches(ZonedDateTime dateTime) {
        // 检查年
        if (years != null && !years.matches(dateTime.getYear())) {
            return false;
        }
        
        // 检查月
        if (!months.matches(dateTime.getMonthValue())) {
            return false;
        }
        
        // 检查日（需要同时考虑日期的日和星期）
        boolean dayOfMonthMatches = daysOfMonth.matches(dateTime.getDayOfMonth());
        boolean dayOfWeekMatches = daysOfWeek.matches(dateTime.getDayOfWeek().getValue() % 7);
        
        // 日和星期都是 * 的特殊情况
        if (daysOfMonth.isAll() && daysOfWeek.isAll()) {
            // 都匹配
        } else if (daysOfMonth.isAll()) {
            // 只有星期有效
            dayOfMonthMatches = true;
        } else if (daysOfWeek.isAll()) {
            // 只有日有效
            dayOfWeekMatches = true;
        }
        
        if (!dayOfMonthMatches && !dayOfWeekMatches) {
            return false;
        }
        
        // 检查时
        if (!hours.matches(dateTime.getHour())) {
            return false;
        }
        
        // 检查分
        if (!minutes.matches(dateTime.getMinute())) {
            return false;
        }
        
        // 检查秒
        if (!seconds.matches(dateTime.getSecond())) {
            return false;
        }
        
        return true;
    }

    @Override
    public String toString() {
        return expression;
    }
}
