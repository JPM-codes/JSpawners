package com.github.jpmcodes.spawner.utils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberFormatter {
    private static final Pattern PATTERN = Pattern.compile("^(\\d+\\.?\\d*)(\\D+)");

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.##");

    public static String format(double value) {
        if (isInvalid(value))
            return "0";

        int index = 0;
        List<String> format = getCurrencyFormat();

        double tmp;
        while ((tmp = value / 1000.0D) >= 1.0D &&
                index + 1 != format.size()) {
            value = tmp;
            index++;
        }

        return NUMBER_FORMAT.format(value) + (String) format.get(index);
    }

    public static double parse(String string) {
        try {
            double value = Double.parseDouble(string);
            return isInvalid(value) ? 0.0D : value;
        } catch (Exception exception) {

            Matcher matcher = PATTERN.matcher(string);
            if (!matcher.find())
                return 0.0D;

            double amount = Double.parseDouble(matcher.group(1));
            String suffix = matcher.group(2);
            String fixedSuffix = suffix.equalsIgnoreCase("k") ? suffix.toLowerCase() : suffix.toUpperCase();

            int index = getCurrencyFormat().indexOf(fixedSuffix);

            double value = amount * Math.pow(1000.0D, index);
            return isInvalid(value) ? 0.0D : value;
        }
    }

    public static boolean isInvalid(double value) {
        return (value < 0.0D || Double.isNaN(value) || Double.isInfinite(value));
    }

    public static String priceWithoutDecimal(Double price) {
        DecimalFormat formatter = new DecimalFormat("###,###,###.##");
        return formatter.format(price);
    }

    public static String priceToString(Double price) {
        return priceWithoutDecimal(price);
    }

    private static List<String> getCurrencyFormat() {
        return Arrays.asList(new String[] { "", "k", "M", "B", "T", "Q", "QQ", "S", "SS", "OC", "N", "D", "UN", "DD",
                "TR", "QT", "QN", "SD", "SPD", "OD", "ND", "VG", "UVG", "DVG", "TVG", "QTV", "QNV", "SEV", "SPV", "OVG",
                "NVG", "TG" });
    }
}