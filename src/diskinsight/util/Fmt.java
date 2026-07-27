package diskinsight.util;

import java.text.NumberFormat;

/** Formatting used across every screen, so numbers always read the same way. */
public final class Fmt {

    private Fmt() { }

    public static final long KB = 1024L;
    public static final long MB = KB * 1024L;
    public static final long GB = MB * 1024L;
    public static final long DAY = 86_400_000L;

    /** 18.62 GB / 940 MB / 12 KB / 512 B */
    public static String bytes(long b) {
        if (b >= GB) return trim(b / (double) GB, b >= 10 * GB ? 1 : 2) + " GB";
        if (b >= MB) return trim(b / (double) MB, b >= 10 * MB ? 0 : 1) + " MB";
        if (b >= KB) return Math.round(b / (double) KB) + " KB";
        return b + " B";
    }

    private static String trim(double v, int decimals) {
        return String.format("%." + decimals + "f", v);
    }

    /** 842 -> "842", 1106 -> "1,106" */
    public static String count(long n) {
        return NumberFormat.getIntegerInstance().format(n);
    }

    /** "1 file" / "23 files" — never "1 files". */
    public static String files(long n) {
        return count(n) + (n == 1 ? " file" : " files");
    }

    /** "today" / "yesterday" / "12 days ago" / "3 months ago" / "2 years ago" */
    public static String ago(long epochMillis) {
        long elapsed = System.currentTimeMillis() - epochMillis;
        if (elapsed < 60 * 60 * 1000L) return "just now";
        long days = elapsed / DAY;
        if (days < 1) return "today";
        if (days == 1) return "yesterday";
        if (days < 30) return days + " days ago";
        if (days < 365) {
            long m = Math.round(days / 30.0);
            return m <= 1 ? "1 month ago" : m + " months ago";
        }
        long y = Math.round(days / 365.0);
        return y <= 1 ? "1 year ago" : y + " years ago";
    }

    /** Whole days between the timestamp and now, used by the rule engine. */
    public static double daysOld(long epochMillis) {
        return (System.currentTimeMillis() - epochMillis) / (double) DAY;
    }

    /** Percentage of a total, guarding against a zero total. */
    public static String percent(long part, long total) {
        if (total <= 0) return "0%";
        return String.format("%.1f%%", part * 100.0 / total);
    }

    /** Formats milliseconds into a readable duration, e.g., "300ms", "2.4s", "1m 12s". */
    public static String duration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        long mins = ms / 60000;
        long secs = (ms % 60000) / 1000;
        return mins + "m " + secs + "s";
    }
}
