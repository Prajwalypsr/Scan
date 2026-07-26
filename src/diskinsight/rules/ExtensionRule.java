package diskinsight.rules;

import diskinsight.model.FileRecord;
import java.util.List;

/**
 * Flags files that match a specific set of extensions.
 */
public class ExtensionRule implements AnalysisRule {
    private final List<String> extensions;

    public ExtensionRule(List<String> extensions) {
        this.extensions = extensions;
    }

    @Override
    public boolean matches(FileRecord f) {
        if (extensions == null || extensions.isEmpty()) {
            return false;
        }
        return extensions.contains(f.extension);
    }
}
