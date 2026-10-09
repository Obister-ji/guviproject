package shop;

import java.util.Scanner;

/** Thin wrapper over Scanner so every menu re-asks on bad input instead of crashing. */
final class In {
    private static final Scanner SC = new Scanner(System.in);

    private In() { }

    static String line(String prompt) {
        System.out.print(prompt + ": ");
        if (!SC.hasNextLine()) {
            System.out.println();
            System.exit(0);
        }
        return SC.nextLine().trim();
    }

    static String required(String prompt) {
        while (true) {
            String s = line(prompt);
            if (!s.isEmpty()) return s;
            System.out.println("  This field can't be empty.");
        }
    }

    static int integer(String prompt, int min, int max) {
        while (true) {
            String s = line(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Enter a whole number between " + min + " and " + max + ".");
        }
    }

    static double price(String prompt) {
        while (true) {
            String s = line(prompt);
            try {
                double v = Double.parseDouble(s);
                if (v > 0 && v < 10_000_000) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Enter a positive amount.");
        }
    }

    static boolean yes(String prompt) {
        return line(prompt + " (y/n)").toLowerCase().startsWith("y");
    }

    static void pause() {
        line("Press Enter to continue");
    }
}
