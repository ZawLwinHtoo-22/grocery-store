package com.example.grocerystore.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component("currency")
public class CurrencyFormatter {

    private static final char[] MY_DIGITS = new char[]{'\u1040','\u1041','\u1042','\u1043','\u1044','\u1045','\u1046','\u1047','\u1048','\u1049'};

    public String format(BigDecimal price, Locale locale) {
        if (price == null) return "";
        // Use integer MMK (no decimals) with grouping
        BigDecimal rounded = price.setScale(0, RoundingMode.HALF_UP);
        // DecimalFormat to add grouping separators
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        String formatted = df.format(rounded);

        if (locale != null && "my".equals(locale.getLanguage())) {
            // convert western digits to Myanmar digits, keep commas
            StringBuilder sb = new StringBuilder();
            for (char c : formatted.toCharArray()) {
                if (c >= '0' && c <= '9') {
                    sb.append(MY_DIGITS[c - '0']);
                } else {
                    sb.append(c);
                }
            }
            // Append Myanmar suffix with a space
            sb.append(" ").append('\u1000').append('\u103B').append('\u1015').append('\u103A');
            return sb.toString();
        }

        // Default English mode
        return formatted + " MMK";
    }
}