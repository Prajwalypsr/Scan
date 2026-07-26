package diskinsight.exception;

/**
 * Thrown when user input for a rule is invalid (e.g., non-numeric size or age).
 */
public class RuleValidationException extends DiskInsightException {
    
    public RuleValidationException(String message) {
        super(message);
    }
    
    public RuleValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
