package diskinsight.rules;

import diskinsight.model.FileRecord;

/**
 * Common interface for all rules that analyze a FileRecord.
 */
public interface AnalysisRule {
    /**
     * @param f the file record to evaluate
     * @return true if the file matches the rule's criteria for being flagged
     */
    boolean matches(FileRecord f);
}
