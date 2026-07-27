package diskinsight.dao;

import diskinsight.model.FileRecord;
import diskinsight.model.Rule;
import diskinsight.model.ScanRecord;

import diskinsight.exception.DatabaseConnectionException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import diskinsight.util.DatabaseConnection;

/**
 * MySQL storage for scans, files and rules.
 *
 * The application is usable without a database: if the driver or the server is
 * missing, connect() records the reason, isAvailable() returns false and every
 * method below becomes a no-op. That way the UI can still be demonstrated on a
 * machine where MySQL has not been set up.
 *
 * To switch it on:
 *   1. run schema.sql on your MySQL server
 *   2. put mysql-connector-j-x.x.x.jar on the classpath
 *   3. edit the db.properties file
 */
public class Database {

    private Connection connection;
    private boolean available;
    private String status = "not connected";

    /** Tries to connect. Throws DatabaseConnectionException if it fails. */
    public boolean connect() throws DatabaseConnectionException {
        try {
            connection = DatabaseConnection.getConnection();
            available = true;
            status = "connected to " + connection.getCatalog();
        } catch (Exception e) {
            available = false;
            status = "offline \u2014 " + e.getMessage();
            throw new DatabaseConnectionException(status, e);
        }
        return available;
    }

    public boolean isAvailable() { return available; }

    public String getStatus() { return status; }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) { }
    }

    /* ==================================================================
       Scans and files
       ================================================================== */

    /** Saves a completed scan and its files. Returns the new scan id, or -1. */
    public int saveScan(String folder, List<FileRecord> files, long durationMs) throws DatabaseConnectionException {
        if (!available) return -1;

        long total = 0, flagged = 0;
        for (FileRecord f : files) {
            total += f.size;
            if (f.flagged) flagged += f.size;
        }

        String insertScan = "INSERT INTO ScanSessions "
                + "(folder_path, scanned_at, file_count, total_size, flagged_size, duration_ms) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String insertFile = "INSERT INTO FileIndex "
                + "(scan_id, file_name, extension, category, size_bytes, "
                + " last_modified, folder_path) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            connection.setAutoCommit(false);

            int scanId;
            try (PreparedStatement ps = connection.prepareStatement(
                    insertScan, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, folder);
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                ps.setInt(3, files.size());
                ps.setLong(4, total);
                ps.setLong(5, flagged);
                ps.setLong(6, durationMs);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    scanId = keys.next() ? keys.getInt(1) : -1;
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(insertFile)) {
                int batch = 0;
                for (FileRecord f : files) {
                    ps.setInt(1, scanId);
                    ps.setString(2, f.name);
                    ps.setString(3, f.extension);
                    ps.setString(4, f.category.name());
                    ps.setLong(5, f.size);
                    ps.setTimestamp(6, new Timestamp(f.modified));
                    ps.setString(7, f.folder);
                    ps.addBatch();
                    if (++batch % 500 == 0) ps.executeBatch();
                }
                ps.executeBatch();
            }

            connection.commit();
            connection.setAutoCommit(true);
            return scanId;

        } catch (SQLException e) {
            rollback();
            status = "save failed \u2014 " + e.getMessage();
            throw new DatabaseConnectionException(status, e);
        }
    }

    /** Reads back the files of a saved scan. */
    public List<FileRecord> loadFiles(int scanId) throws DatabaseConnectionException {
        List<FileRecord> out = new ArrayList<>();
        if (!available) return out;

        String sql = "SELECT file_id, file_name, size_bytes, last_modified, folder_path "
                   + "FROM FileIndex WHERE scan_id = ? ORDER BY size_bytes DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, scanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new FileRecord(
                            rs.getInt("file_id"),
                            rs.getString("file_name"),
                            rs.getLong("size_bytes"),
                            rs.getTimestamp("last_modified").getTime(),
                            rs.getString("folder_path")));
                }
            }
        } catch (SQLException e) {
            status = "load failed \u2014 " + e.getMessage();
            throw new DatabaseConnectionException(status, e);
        }
        return out;
    }

    public List<ScanRecord> loadHistory(int limit) throws DatabaseConnectionException {
        List<ScanRecord> out = new ArrayList<>();
        if (!available) return out;

        String sql = "SELECT scan_id, folder_path, scanned_at, file_count, "
                   + "total_size, flagged_size, duration_ms FROM ScanSessions "
                   + "ORDER BY scanned_at DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ScanRecord(
                            rs.getInt("scan_id"),
                            rs.getString("folder_path"),
                            rs.getTimestamp("scanned_at").getTime(),
                            rs.getInt("file_count"),
                            rs.getLong("total_size"),
                            rs.getLong("flagged_size"),
                            rs.getLong("duration_ms")));
                }
            }
        } catch (SQLException e) {
            status = "history failed \u2014 " + e.getMessage();
            throw new DatabaseConnectionException(status, e);
        }
        return out;
    }

    /* ==================================================================
       Rules
       ================================================================== */

    public List<Rule> loadRules() throws DatabaseConnectionException {
        List<Rule> out = new ArrayList<>();
        if (!available) return out;

        String sql = "SELECT rule_id, rule_name, enabled, extensions, "
                   + "min_size, older_than_days FROM rules ORDER BY rule_id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new Rule(
                        rs.getInt("rule_id"),
                        rs.getString("rule_name"),
                        rs.getBoolean("enabled"),
                        rs.getString("extensions"),
                        rs.getLong("min_size"),
                        rs.getInt("older_than_days")));
            }
        } catch (SQLException e) {
            status = "rules failed \u2014 " + e.getMessage();
            throw new DatabaseConnectionException(status, e);
        }
        return out;
    }

    public void saveRule(Rule r) throws DatabaseConnectionException {
        if (!available) return;
        String sql = "INSERT INTO rules "
                + "(rule_id, rule_name, enabled, extensions, min_size, older_than_days) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE rule_name = VALUES(rule_name), "
                + "enabled = VALUES(enabled), extensions = VALUES(extensions), "
                + "min_size = VALUES(min_size), older_than_days = VALUES(older_than_days)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, r.id);
            ps.setString(2, r.name);
            ps.setBoolean(3, r.enabled);
            ps.setString(4, r.extensionsAsText());
            ps.setLong(5, r.minSize);
            ps.setInt(6, r.olderThanDays);
            ps.executeUpdate();
        } catch (SQLException e) {
            status = "rule save failed \u2014 " + e.getMessage();
            throw new DatabaseConnectionException(status, e);
        }
    }

    public void deleteRule(int ruleId) throws DatabaseConnectionException {
        if (!available) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM rules WHERE rule_id = ?")) {
            ps.setInt(1, ruleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            status = "rule delete failed \u2014 " + e.getMessage();
            throw new DatabaseConnectionException(status, e);
        }
    }

    private void rollback() {
        try {
            if (connection != null) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException ignored) { }
    }
}
