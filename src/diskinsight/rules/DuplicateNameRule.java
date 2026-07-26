package diskinsight.rules;

import diskinsight.model.FileRecord;

/**
 * Global rule heuristic that flags files appearing to be duplicates
 * based on their naming pattern (e.g. "resume (1).pdf", "backup_copy.zip").
 */
public class DuplicateNameRule implements AnalysisRule {

    @Override
    public boolean matches(FileRecord f) {
        if (f.name == null) return false;
        String lowerName = f.name.toLowerCase();
        
        // Match files containing "_copy", "-copy", or ending in " (1)", "(2)" etc before extension
        if (lowerName.contains("_copy") || lowerName.contains("-copy")) {
            return true;
        }
        
        // Regex: any chars, space (optional), parenthesis containing digits, then dot and extension
        return lowerName.matches(".*\\s?\\(\\d+\\)\\.[a-zA-Z0-9]+$");
    }
}
