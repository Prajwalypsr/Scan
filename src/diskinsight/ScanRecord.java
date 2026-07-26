package diskinsight;

/** One completed scan, kept so a folder can be compared with itself over time. */
public class ScanRecord {

    public int id;
    public String folder;
    public long scannedAt;    // epoch millis
    public int fileCount;
    public long totalSize;
    public long flaggedSize;  // bytes matched by the active rules

    public ScanRecord(int id, String folder, long scannedAt,
                      int fileCount, long totalSize, long flaggedSize) {
        this.id = id;
        this.folder = folder;
        this.scannedAt = scannedAt;
        this.fileCount = fileCount;
        this.totalSize = totalSize;
        this.flaggedSize = flaggedSize;
    }
}
