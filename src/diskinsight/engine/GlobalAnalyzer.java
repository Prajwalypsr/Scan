package diskinsight.engine;

import diskinsight.model.FileRecord;
import diskinsight.model.Rule;
import diskinsight.rules.AnalysisRule;
import diskinsight.rules.DuplicateNameRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrator that evaluates files against all active DB rules
 * and global heuristic rules.
 */
public class GlobalAnalyzer {
    private final List<RuleEngine> activeEngines = new ArrayList<>();
    private final List<AnalysisRule> globalRules = new ArrayList<>();
    private final Rule duplicateSourceRule;

    public GlobalAnalyzer(List<Rule> activeDbRules) {
        // Convert active DB rules to polymorphic engines
        for (Rule r : activeDbRules) {
            if (r.enabled) {
                activeEngines.add(new RuleEngine(r));
            }
        }
        
        // Add global heuristic rules that aren't configured in DB
        globalRules.add(new DuplicateNameRule());
        
        // Create a dummy source rule to attach to files flagged by the duplicate heuristic
        duplicateSourceRule = new Rule(-1, "Possible Duplicate", true, "", 0, 0);
    }

    /**
     * Runs all rules against a file and populates its hits.
     */
    public void analyze(FileRecord f) {
        f.hits.clear();
        
        // 1. Evaluate against configured rule engines
        for (RuleEngine engine : activeEngines) {
            if (engine.evaluate(f)) {
                f.hits.add(engine.getSourceRule());
            }
        }
        
        // 2. Evaluate against global heuristic rules
        for (AnalysisRule globalRule : globalRules) {
            if (globalRule.matches(f)) {
                if (globalRule instanceof DuplicateNameRule) {
                    f.hits.add(duplicateSourceRule);
                }
                // (Extend here if adding more global rules that need dummy DB sources)
            }
        }
        
        f.flagged = !f.hits.isEmpty();
    }
}
