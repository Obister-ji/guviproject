package shop;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class Fmt {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private Fmt() { }

    static String money(double v) {
        return String.format("Rs %,.2f", v);
    }

    static String stamp(LocalDateTime t) {
        return t.format(STAMP);
    }

    static String cut(String s, int width) {
        if (s == null) return "";
        return s.length() <= width ? s : s.substring(0, width - 1) + "~";
    }

    static String bar(int value, int max, int width) {
        int n = max <= 0 ? 0 : (int) Math.round((double) value / max * width);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append(i < n ? '#' : '.');
        }
        return sb.toString();
    }

    static void title(String text) {
        System.out.println();
        System.out.println("== " + text + " ==");
    }

    static void rule() {
        System.out.println("-".repeat(72));
    }
}
