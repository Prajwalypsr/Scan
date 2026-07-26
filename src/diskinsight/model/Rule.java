package diskinsight.model;

import diskinsight.util.Fmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A cleanup rule. A file matches when it satisfies every condition that has
 * been set; conditions left empty are ignored.
 *
 * Nothing here deletes anything — a match only marks a file as worth reviewing.
 */
public class Rule {

    public int id;
    public String name;
    public boolean enabled = true;
    public List<String> extensions = new ArrayList<>(); // empty = any type
    public long minSize;        // bytes, 0 = any size
    public int olderThanDays;   // 0 = any age

    public Rule(int id, String name, boolean enabled,
                String extensions, long minSize, int olderThanDays) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.extensions = parseExtensions(extensions);
        this.minSize = minSize;
        this.olderThanDays = olderThanDays;
    }

    /** Accepts "zip, .rar, 7z" and stores ["zip","rar","7z"]. */
    public static List<String> parseExtensions(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        for (String part : raw.split(",")) {
            String e = part.trim().toLowerCase();
            while (e.startsWith(".")) e = e.substring(1);
            if (!e.isEmpty() && !out.contains(e)) out.add(e);
        }
        return out;
    }

    public boolean matches(FileRecord f) {
        if (!extensions.isEmpty() && !extensions.contains(f.extension)) return false;
        if (minSize > 0 && f.size < minSize) return false;
        if (olderThanDays > 0 && Fmt.daysOld(f.modified) < olderThanDays) return false;
        return true;
    }

    /** Plain-language summary shown under the rule name. */
    public String describe() {
        List<String> parts = new ArrayList<>();
        parts.add(extensions.isEmpty() ? "any type" : "." + String.join(", .", extensions));
        if (minSize > 0) parts.add("larger than " + Fmt.bytes(minSize));
        if (olderThanDays > 0) parts.add("older than " + olderThanDays + " days");
        return String.join("  \u00b7  ", parts);
    }

    public String extensionsAsText() {
        return String.join(", ", extensions);
    }

    /** The rules the application starts with. */
    public static List<Rule> defaults() {
        return new ArrayList<>(Arrays.asList(
            new Rule(1, "Very large files",       true,  "",                            Fmt.GB,        0),
            new Rule(2, "Stale temporary files",  true,  "tmp,part,crdownload,log",     0,             30),
            new Rule(3, "Old archives",           true,  "zip,rar,7z",                  0,             180),
            new Rule(4, "Installers you kept",    true,  "exe,msi,dmg,deb",             0,             90),
            new Rule(5, "Big videos over a year", false, "mp4,mkv,mov,avi",             500 * Fmt.MB,  365)
        ));
    }
}
