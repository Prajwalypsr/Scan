package diskinsight.exception;

/**
 * Base checked exception for all application-specific errors in DiskInsight.
 */
public class DiskInsightException extends Exception {
    
    public DiskInsightException(String message) {
        super(message);
    }
    
    public DiskInsightException(String message, Throwable cause) {
        super(message, cause);
    }
}
