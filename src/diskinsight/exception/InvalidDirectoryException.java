package diskinsight.exception;

/**
 * Thrown when a folder scan fails due to I/O issues, permissions, or invalid paths.
 */
public class InvalidDirectoryException extends DiskInsightException {
    
    public InvalidDirectoryException(String message) {
        super(message);
    }
    
    public InvalidDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
