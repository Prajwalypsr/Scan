package diskinsight.model;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * The groups a file can belong to. Each group owns one colour, used
 * consistently everywhere it appears: the storage tape, the legend chips,
 * the extension badge in the file table and the mini size bars.
 */
public enum Category {

    VIDEO("Videos", new Color(0x4C6FE7),
            "mp4", "mkv", "mov", "avi", "wmv", "flv", "webm"),

    ARCHIVE("Archives", new Color(0x7B5EC7),
            "zip", "rar", "7z", "tar", "gz", "bz2"),

    INSTALLER("Installers", new Color(0xC97A0A),
            "exe", "msi", "dmg", "deb", "rpm", "apk", "pkg"),

    DOCUMENT("Documents", new Color(0x12938F),
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "odt"),

    IMAGE("Images", new Color(0xCB5F84),
            "jpg", "jpeg", "png", "gif", "bmp", "heic", "svg", "psd", "webp"),

    AUDIO("Audio", new Color(0x6F9A4C),
            "mp3", "wav", "m4a", "flac", "aac", "ogg"),

    TEMP("Temporary files", new Color(0x94A2B3),
            "tmp", "temp", "part", "crdownload", "log", "bak", "old"),

    OTHER("Everything else", new Color(0xB9C2CE));

    private static final Map<String, Category> BY_EXT = new HashMap<>();

    static {
        for (Category c : values()) {
            for (String e : c.extensions) BY_EXT.put(e, c);
        }
    }

    public final String label;
    public final Color color;
    public final String[] extensions;

    Category(String label, Color color, String... extensions) {
        this.label = label;
        this.color = color;
        this.extensions = extensions;
    }

    /** Looks up the group for a file extension; unknown types fall into OTHER. */
    public static Category of(String extension) {
        if (extension == null) return OTHER;
        Category c = BY_EXT.get(extension.toLowerCase());
        return c == null ? OTHER : c;
    }
}
