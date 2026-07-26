package diskinsight.rules;

import diskinsight.model.FileRecord;
import diskinsight.util.Fmt;

/**
 * Flags files that are older than a specific number of days.
 */
public class OldFileRule implements AnalysisRule {
    private final int olderThanDays;

    public OldFileRule(int olderThanDays) {
        this.olderThanDays = olderThanDays;
    }

    @Override
    public boolean matches(FileRecord f) {
        return olderThanDays > 0 && Fmt.daysOld(f.modified) >= olderThanDays;
    }
}
