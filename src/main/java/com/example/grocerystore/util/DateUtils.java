package com.example.grocerystore.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class DateUtils {

    private static final String[] MY_MONTHS = new String[]{
            "ဇန်နဝါရီ", "ဖေဖော်ဝါရီ", "မတ်", "ဧပြီ", "မေ", "ဇွန်",
            "ဇူလိုင်", "ဩဂုတ်", "စက်တင်ဘာ", "အောက်တိုဘာ", "နိုဝင်ဘာ", "ဒီဇင်ဘာ"
    };

    public String format(LocalDateTime dt, String pattern, Locale locale) {
        if (dt == null) return "";
        if (locale != null && "my".equals(locale.getLanguage())) {
            // Provide Myanmar formatted output for patterns containing day, month name and year/time
            // Common patterns used: "dd MMM yyyy, HH:mm" and "dd MMM" and "yyyy-MM-dd"
            if (pattern.contains("yyyy-MM-dd")) {
                DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return f.format(dt);
            }
            int day = dt.getDayOfMonth();
            int month = dt.getMonthValue();
            int year = dt.getYear();
            int hour = dt.getHour();
            int minute = dt.getMinute();
            String monthName = MY_MONTHS[month - 1];
            if (pattern.contains("HH") || pattern.contains("mm")) {
                // include time
                return String.format("%02d %s %04d, %02d:%02d", day, monthName, year, hour, minute);
            }
            // short date
            return String.format("%02d %s %04d", day, monthName, year);
        }
        // fallback: use standard Java formatter with the provided locale
        DateTimeFormatter fmt;
        if (locale != null) {
            fmt = DateTimeFormatter.ofPattern(pattern, locale);
        } else {
            fmt = DateTimeFormatter.ofPattern(pattern);
        }
        return fmt.format(dt);
    }

    public String format(LocalDate dt, String pattern, Locale locale) {
        if (dt == null) return "";
        if (locale != null && "my".equals(locale.getLanguage())) {
            int day = dt.getDayOfMonth();
            int month = dt.getMonthValue();
            int year = dt.getYear();
            String monthName = MY_MONTHS[month - 1];
            if (pattern.contains("yyyy-MM-dd")) {
                return String.format("%04d-%02d-%02d", year, month, day);
            }
            return String.format("%02d %s %04d", day, monthName, year);
        }
        DateTimeFormatter fmt;
        if (locale != null) {
            fmt = DateTimeFormatter.ofPattern(pattern, locale);
        } else {
            fmt = DateTimeFormatter.ofPattern(pattern);
        }
        return fmt.format(dt);
    }
}
