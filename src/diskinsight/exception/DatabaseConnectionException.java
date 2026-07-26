package diskinsight.exception;

/**
 * Thrown when a database operation fails, wrapping the underlying SQLException.
 */
public class DatabaseConnectionException extends DiskInsightException {
    
    public DatabaseConnectionException(String message) {
        super(message);
    }
    
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
