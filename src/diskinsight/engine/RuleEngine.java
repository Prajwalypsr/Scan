package diskinsight.engine;

import diskinsight.model.FileRecord;
import diskinsight.model.Rule;
import diskinsight.rules.AnalysisRule;
import diskinsight.rules.ExtensionRule;
import diskinsight.rules.LargeFileRule;
import diskinsight.rules.OldFileRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a database Rule into a list of polymorphic AnalysisRules,
 * evaluating a file against them without hardcoded if/else chains.
 */
public class RuleEngine {
    private final Rule sourceRule;
    private final List<AnalysisRule> conditions = new ArrayList<>();

    public RuleEngine(Rule sourceRule) {
        this.sourceRule = sourceRule;
        
        if (sourceRule.minSize > 0) {
            conditions.add(new LargeFileRule(sourceRule.minSize));
        }
        if (sourceRule.olderThanDays > 0) {
            conditions.add(new OldFileRule(sourceRule.olderThanDays));
        }
        if (sourceRule.extensions != null && !sourceRule.extensions.isEmpty()) {
            conditions.add(new ExtensionRule(sourceRule.extensions));
        }
    }

    /**
     * @return the original database rule this engine represents.
     */
    public Rule getSourceRule() {
        return sourceRule;
    }

    /**
     * Evaluates a file against all composed criteria.
     * @param f the file record
     * @return true if it matches ALL criteria of this engine.
     */
    public boolean evaluate(FileRecord f) {
        // If there are no conditions configured, it shouldn't match any files 
        // to prevent an empty rule from flagging the whole hard drive.
        if (conditions.isEmpty()) return false;
        
        for (AnalysisRule condition : conditions) {
            if (!condition.matches(f)) {
                return false;
            }
        }
        return true;
    }
}
