package diskinsight.service;

import diskinsight.model.FileRecord;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/**
 * Reads a real folder on a background thread.
 *
 * Swing rule: the scan must not run on the Event Dispatch Thread or the window
 * would freeze. SwingWorker does the walking in doInBackground() and hands the
 * result back on the EDT in done(), so the UI stays responsive throughout.
 */
public class FolderScanner extends SwingWorker<List<FileRecord>, String> {

    private final Path root;
    private final boolean includeSubfolders;
    private final Consumer<String> onProgress;
    private final Consumer<List<FileRecord>> onFinished;
    private final Consumer<String> onFailed;

    private int found = 0;
    private int skipped = 0;

    public FolderScanner(Path root,
                         boolean includeSubfolders,
                         Consumer<String> onProgress,
                         Consumer<List<FileRecord>> onFinished,
                         Consumer<String> onFailed) {
        this.root = root;
        this.includeSubfolders = includeSubfolders;
        this.onProgress = onProgress;
        this.onFinished = onFinished;
        this.onFailed = onFailed;
    }

    @Override
    protected List<FileRecord> doInBackground() throws Exception {
        List<FileRecord> files = new ArrayList<>();
        int maxDepth = includeSubfolders ? Integer.MAX_VALUE : 1;

        Files.walkFileTree(root, java.util.EnumSet.noneOf(FileVisitOption.class), maxDepth,
            new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    if (isCancelled()) return FileVisitResult.TERMINATE;
                    if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;

                    Path parent = path.getParent();
                    files.add(new FileRecord(
                            ++found,
                            path.getFileName().toString(),
                            attrs.size(),
                            attrs.lastModifiedTime().toMillis(),
                            parent == null ? root.toString() : parent.toString()));

                    if (found % 25 == 0) publish(path.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) {
                    // permission denied, locked file, broken link — count and move on
                    skipped++;
                    return FileVisitResult.CONTINUE;
                }
            });

        return files;
    }

    @Override
    protected void process(List<String> chunks) {
        if (!chunks.isEmpty()) onProgress.accept(chunks.get(chunks.size() - 1));
    }

    @Override
    protected void done() {
        if (isCancelled()) return;
        try {
            onFinished.accept(get());
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            String message = cause.getMessage() == null
                    ? cause.getClass().getSimpleName()
                    : cause.getMessage();
            onFailed.accept("That folder could not be read: " + message);
        }
    }

    /** Files the scan could not open, usually because of permissions. */
    public int getSkipped() { return skipped; }
}
