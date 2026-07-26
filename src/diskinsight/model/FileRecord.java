package diskinsight.model;

import java.util.ArrayList;
import java.util.List;

/** One file found during a scan, plus the rules it currently matches. */
public class FileRecord {

    public int id;
    public String name;
    public String extension;
    public Category category;
    public long size;         // bytes
    public long modified;     // epoch millis
    public String folder;     // parent folder, e.g. C:\Users\me\Downloads\Invoices

    /** Rules this file matched on the last evaluation. */
    public final List<Rule> hits = new ArrayList<>();
    public boolean flagged;
    public boolean selected;

    public FileRecord(int id, String name, long size, long modified, String folder) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.modified = modified;
        this.folder = folder;
        this.extension = extensionOf(name);
        this.category = Category.of(this.extension);
    }

    public static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase();
    }

    public String fullPath() {
        return folder + java.io.File.separator + name;
    }

    /** Short label for the coloured badge in the file table. */
    public String badge() {
        if (extension.isEmpty()) return "\u2014";
        return extension.length() > 4
                ? extension.substring(0, 4).toUpperCase()
                : extension.toUpperCase();
    }

    /** Names of the matched rules, for the "why is this flagged" line. */
    public String reason() {
        if (hits.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Rule r : hits) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(r.name);
        }
        return sb.toString();
    }
}
