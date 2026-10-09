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
}
