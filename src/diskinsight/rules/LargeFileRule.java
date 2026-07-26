package diskinsight.rules;

import diskinsight.model.FileRecord;

/**
 * Flags files that exceed a certain size threshold.
 */
public class LargeFileRule implements AnalysisRule {
    private final long minSize;

    public LargeFileRule(long minSize) {
        this.minSize = minSize;
    }

    @Override
    public boolean matches(FileRecord f) {
        return minSize > 0 && f.size >= minSize;
    }
}
