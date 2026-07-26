package diskinsight;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sample data, so the screens can be demonstrated without scanning a real
 * folder. The seed is fixed, so the sample folder looks the same every run.
 *
 * Replace this with FolderScanner (real files) or Database.loadFiles (a saved
 * scan) — the rest of the application does not care where the list came from.
 */
public final class DemoData {

    private DemoData() { }

    private static final String[] WORDS = {
        "report", "invoice", "backup", "setup", "draft", "final", "holiday",
        "budget", "screen", "recording", "presentation", "photo", "scan",
        "export", "statement", "recipe", "resume", "notes", "contract", "demo",
        "logo", "meeting", "tutorial", "wallpaper", "receipt", "album",
        "archive", "update", "installer", "sample", "render", "clip"
    };

    private static final String[] SUFFIX = {
        "", "_final", "_v2", "_v3", "(1)", "(2)", "_old", "_copy",
        "_2024", "_2025", "_draft", "-compressed"
    };

    private static final String[] SUBFOLDERS = {
        "", "Invoices", "Chrome", "Unpacked", "2024 archive"
    };

    /**
     * How the 842 sample files are made up: how many of each group, the
     * smallest and largest a file of that group can be, and how strongly the
     * sizes lean small. A high skew means most files are near the low end with
     * a few large outliers, which is how a real Downloads folder behaves.
     */
    private static final Object[][] MIX = {
        //  category            count   smallest        largest          skew
        { Category.VIDEO,        84,    25L * Fmt.MB,  2000L * Fmt.MB,  24.0 },
        { Category.ARCHIVE,      67,     3L * Fmt.MB,   900L * Fmt.MB,  18.0 },
        { Category.INSTALLER,    50,     8L * Fmt.MB,   700L * Fmt.MB,  14.0 },
        { Category.DOCUMENT,    185,    40L * Fmt.KB,    60L * Fmt.MB,   7.0 },
        { Category.IMAGE,       236,   180L * Fmt.KB,    25L * Fmt.MB,   4.5 },
        { Category.AUDIO,        59,     2L * Fmt.MB,   120L * Fmt.MB,   7.5 },
        { Category.TEMP,         76,     1L * Fmt.KB,   250L * Fmt.MB,  22.0 },
        { Category.OTHER,        85,     2L * Fmt.KB,   180L * Fmt.MB,  24.0 }
    };

    /** Extensions that deliberately fall outside every named group. */
    private static final String[] OTHER_EXTENSIONS = {"json", "dll", "iso", "ttf", "dat"};

    public static final String SAMPLE_FOLDER = "~" + java.io.File.separator + "Downloads";

    public static List<FileRecord> sampleFolder() {
        return generate(842, 7);
    }

    public static List<FileRecord> generate(int fileCount, long seed) {
        Random r = new Random(seed);
        List<FileRecord> files = new ArrayList<>();
        int id = 0;

        for (Object[] spec : MIX) {
            Category cat = (Category) spec[0];
            int count = (Integer) spec[1];
            long lo = (Long) spec[2];
            long hi = (Long) spec[3];
            double skew = (Double) spec[4];

            for (int i = 0; i < count; i++) {
                String ext = pickExtension(cat, r);

                // most files sit near the low end, a few run large
                long size = Math.max(512L, lo + Math.round(Math.pow(r.nextDouble(), skew) * (hi - lo)));

                int ageDays = (int) Math.round(Math.pow(r.nextDouble(), 1.7) * 900);
                long modified = System.currentTimeMillis()
                        - ageDays * Fmt.DAY
                        - (long) (r.nextDouble() * Fmt.DAY);

                String name = WORDS[r.nextInt(WORDS.length)]
                        + (r.nextBoolean() ? "-" + WORDS[r.nextInt(WORDS.length)] : "")
                        + SUFFIX[r.nextInt(SUFFIX.length)]
                        + "." + ext;

                String sub = SUBFOLDERS[r.nextInt(SUBFOLDERS.length)];
                String folder = SAMPLE_FOLDER
                        + (sub.isEmpty() ? "" : java.io.File.separator + sub);

                files.add(new FileRecord(++id, name, size, modified, folder));
            }
        }
        return files;
    }

    /**
     * Picks an extension, leaning towards the first in the list. Real folders
     * are not evenly split: there are far more .pdf than .odt, and far more
     * .zip than .tar.
     */
    private static String pickExtension(Category cat, Random r) {
        String[] pool = cat.extensions.length == 0 ? OTHER_EXTENSIONS : cat.extensions;
        int usable = Math.min(5, pool.length);
        int index = (int) (Math.pow(r.nextDouble(), 1.3) * usable);
        return pool[Math.min(index, usable - 1)];
    }
}
