package diskinsight.ui;

import diskinsight.dao.Database;
import diskinsight.model.Category;
import diskinsight.model.FileRecord;
import diskinsight.model.Rule;
import diskinsight.model.ScanRecord;
import diskinsight.service.FolderScanner;
import diskinsight.util.Fmt;
import diskinsight.engine.GlobalAnalyzer;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

/**
 * The application window: top bar, tabs, and the state every screen reads from.
 *
 * The four screens do not talk to each other. They ask this class for the
 * current file list and rules, and call refreshAll() when something changes.
 */
public class MainFrame extends JFrame {

    /* ---- state ---- */
    private List<FileRecord> files = new ArrayList<>();
    private List<Rule> rules = Rule.defaults();
    private final List<ScanRecord> history = new ArrayList<>();
    private String folder = "none";
    private long scannedAt = System.currentTimeMillis();
    private long scanStartTime = 0;

    private final Database database = new Database();
    private FolderScanner activeScan;

    /* ---- screens ---- */
    private final OverviewPanel overview = new OverviewPanel(this);
    private final FilesPanel filesPanel = new FilesPanel(this);
    private final RulesPanel rulesPanel = new RulesPanel(this);
    private final HistoryPanel historyPanel = new HistoryPanel(this);
    private final ScanningPanel scanning = new ScanningPanel();

    /* ---- chrome ---- */
    private final CardLayout rootCards = new CardLayout();
    private final JPanel rootContent = new JPanel(rootCards);
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel folderLabel = Ui.mono("none");
    private final JLabel statusLabel = Ui.hint("");
    private final List<TabButton> tabs = new ArrayList<>();
    private final Ui.Btn scanButton = Ui.primary("Scan folder");

    public MainFrame() {
        super("DiskInsight");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 840);
        setMinimumSize(new Dimension(1020, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.PAPER);
        JPanel dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setOpaque(false);
        dashboardPanel.add(buildTopBar(), BorderLayout.NORTH);
        dashboardPanel.add(buildBody(), BorderLayout.CENTER);
        dashboardPanel.add(buildFooter(), BorderLayout.SOUTH);
        
        rootContent.setOpaque(false);
        rootContent.add(new WelcomePanel(this), "welcome");
        rootContent.add(dashboardPanel, "dashboard");

        setLayout(new BorderLayout());
        add(rootContent, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { database.close(); }
        });

        connectDatabase();
        rootCards.show(rootContent, "welcome");
    }

    /* ==================================================================
       State, read by the screens
       ================================================================== */

    public List<FileRecord> files()   { return files; }
    public List<Rule> rules()         { return rules; }
    public List<ScanRecord> history() { return history; }
    public String folder()            { return folder; }
    public long scannedAt()           { return scannedAt; }

    /** Re-evaluates every file against the enabled rules. */
    public void applyRules() {
        GlobalAnalyzer analyzer = new GlobalAnalyzer(rules);
        for (FileRecord f : files) {
            analyzer.analyze(f);
        }
    }

    /** Redraws every screen from the current state. */
    public void refreshAll() {
        overview.refresh();
        filesPanel.refresh();
        rulesPanel.refresh();
        historyPanel.refresh();
        for (TabButton t : tabs) t.refreshCount();
    }

    public void saveRule(Rule r) {
        try {
            database.saveRule(r);
        } catch (diskinsight.exception.DatabaseConnectionException e) {
            JOptionPane.showMessageDialog(this, "Failed to save rule: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteRule(Rule r) {
        try {
            database.deleteRule(r.id);
        } catch (diskinsight.exception.DatabaseConnectionException e) {
            JOptionPane.showMessageDialog(this, "Failed to delete rule: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showFilesForCategory(Category c) {
        filesPanel.showCategory(c);
        setView("files");
    }

    public void showFilesForRule(Rule r) {
        filesPanel.showRule(r);
        setView("files");
    }

    public void setView(String name) {
        cards.show(content, name);
        for (TabButton t : tabs) t.setActive(t.view.equals(name));
    }

    /* ==================================================================
       Chrome
       ================================================================== */

    private JComponent buildTopBar() {
        JPanel bar = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.aa(g);
                g2.setColor(Theme.CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.LINE);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(14, Theme.L, 14, Theme.L));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        bar.add(new BrandMark(), gc);

        gc.gridx = 1;
        gc.insets = new Insets(0, Theme.S, 0, Theme.XL);
        bar.add(Ui.label("DiskInsight", Theme.H2, Theme.INK), gc);

        gc.gridx = 2;
        gc.insets = new Insets(0, 0, 0, Theme.S);
        bar.add(Ui.caption("Folder"), gc);

        gc.gridx = 3;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        folderLabel.setForeground(Theme.INK);
        bar.add(folderLabel, gc);

        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(0, Theme.S, 0, 0);

        Ui.Btn choose = Ui.ghost("Choose folder\u2026");
        choose.addActionListener(e -> chooseFolder());
        gc.gridx = 5;
        bar.add(choose, gc);

        scanButton.addActionListener(e -> chooseFolder());
        gc.gridx = 6;
        bar.add(scanButton, gc);

        return bar;
    }

    private JComponent buildBody() {
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.L, 0));
        tabBar.setOpaque(false);
        tabBar.setBorder(BorderFactory.createEmptyBorder(Theme.L, Theme.L - 5, 0, Theme.L));

        tabBar.add(makeTab("Overview", "overview", () -> null));
        tabBar.add(makeTab("Files", "files", () -> Fmt.count(files.size())));
        tabBar.add(makeTab("Rules", "rules", () -> {
            int on = 0;
            for (Rule r : rules) if (r.enabled) on++;
            return String.valueOf(on);
        }));
        tabBar.add(makeTab("Scan history", "history", () -> null));

        JPanel underline = new JPanel(new BorderLayout());
        underline.setOpaque(false);
        underline.setBorder(BorderFactory.createEmptyBorder(0, Theme.L, 0, Theme.L));
        underline.add(Ui.hairline(), BorderLayout.CENTER);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(tabBar, BorderLayout.CENTER);
        head.add(underline, BorderLayout.SOUTH);

        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(Theme.L, Theme.L, Theme.L, Theme.L));
        content.add(Ui.scroll(overview), "overview");
        content.add(filesPanel, "files");
        content.add(Ui.scroll(rulesPanel), "rules");
        content.add(Ui.scroll(historyPanel), "history");
        content.add(scanning, "scanning");

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(head, BorderLayout.NORTH);
        body.add(content, BorderLayout.CENTER);
        return body;
    }

    private TabButton makeTab(String label, String view, java.util.function.Supplier<String> count) {
        TabButton t = new TabButton(label, view, count);
        t.addActionListener(e -> setView(view));
        tabs.add(t);
        return t;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.S, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(Theme.S, Theme.L, Theme.M, Theme.L));
        footer.add(statusLabel);
        return footer;
    }

    private void updateStatus(String extra) {
        statusLabel.setText("Database: " + database.getStatus()
                + "     \u00b7     " + extra
                + "     \u00b7     DiskInsight never deletes files.");
    }

    /* ==================================================================
       Loading data
       ================================================================== */

    private void connectDatabase() {
        try {
            database.connect();
            if (database.isAvailable()) {
                List<Rule> saved = database.loadRules();
                if (!saved.isEmpty()) rules = saved;
                history.addAll(database.loadHistory(20));
            }
        } catch (diskinsight.exception.DatabaseConnectionException e) {
            // Handled naturally by offline degradation and status reporting
        }
    }

    public void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a folder to scan");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        startScan(chooser.getSelectedFile().toPath());
    }

    private void startScan(Path root) {
        scanStartTime = System.currentTimeMillis();
        folder = root.toString();
        folderLabel.setText(folder);
        scanButton.setEnabled(false);
        scanning.begin(folder);
        rootCards.show(rootContent, "dashboard");
        setView("scanning");

        activeScan = new FolderScanner(
                root,
                true,
                scanning::setCurrentPath,
                this::onScanFinished,
                this::onScanFailed);
        activeScan.execute();
    }

    private void onScanFinished(List<FileRecord> found) {
        long durationMs = System.currentTimeMillis() - scanStartTime;
        scanning.end();
        scanButton.setEnabled(true);
        files = found;
        scannedAt = System.currentTimeMillis();
        applyRules();

        long total = 0, flagged = 0;
        for (FileRecord f : files) {
            total += f.size;
            if (f.flagged) flagged += f.size;
        }
        try {
            int id = database.saveScan(folder, files, durationMs);
            if (id != -1) {
                history.add(0, new ScanRecord(id, folder, scannedAt, files.size(), total, flagged, durationMs));
            } else {
                JOptionPane.showMessageDialog(this, 
                        "Scan finished, but could not be saved because the database is offline.\n" +
                        "Status: " + database.getStatus(), 
                        "Database Offline", JOptionPane.WARNING_MESSAGE);
            }
        } catch (diskinsight.exception.DatabaseConnectionException e) {
            JOptionPane.showMessageDialog(this, 
                    "Failed to save scan to database:\n" + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        int skipped = activeScan == null ? 0 : activeScan.getSkipped();
        updateStatus(skipped > 0
                ? skipped + " files could not be read (permissions)"
                : "Scanned " + Fmt.count(files.size()) + " files");

        filesPanel.clearFilters();
        refreshAll();
        setView("overview");
    }

    private void onScanFailed(diskinsight.exception.DiskInsightException exception) {
        scanning.end();
        scanButton.setEnabled(true);
        setView("overview");
        JOptionPane.showMessageDialog(this, exception.getMessage(), "Scan stopped",
                JOptionPane.WARNING_MESSAGE);
    }

    /* ==================================================================
       Small custom pieces
       ================================================================== */

    /** The four-bar logo mark, echoing the storage tape. */
    private static class BrandMark extends JComponent {
        BrandMark() { setPreferredSize(new Dimension(26, 26)); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            g2.setColor(Theme.INK);
            g2.fillRoundRect(0, 0, 26, 26, 7, 7);
            g2.setColor(Color.WHITE);
            int[] widths = {18, 11, 15, 6};
            for (int i = 0; i < widths.length; i++) {
                g2.fillRoundRect(4, 5 + i * 5, widths[i], 2, 2, 2);
            }
            g2.dispose();
        }
    }

    /** A flat tab with an underline when active. */
    private class TabButton extends JButton {
        final String view;
        private final String label;
        private final java.util.function.Supplier<String> count;
        private boolean active;

        TabButton(String label, String view, java.util.function.Supplier<String> count) {
            this.label = label;
            this.view = view;
            this.count = count;
            this.active = view.equals("overview");
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 5, 12, 5));
            setFont(Theme.BODY_BOLD);
            refreshCount();
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        void refreshCount() {
            String c = count.get();
            setText(c == null ? label : label + "  " + c);
            setPreferredSize(null);
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            int h = getHeight();
            g2.setFont(Theme.BODY_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(active ? Theme.INK
                              : (getModel().isRollover() ? Theme.INK_2 : Theme.INK_3));
            g2.drawString(getText(), 5, h / 2 + fm.getAscent() / 2 - 3);
            if (active) {
                g2.setColor(Theme.INK);
                g2.fillRect(0, h - 2, getWidth(), 2);
            }
            g2.dispose();
        }
    }

    /** The "reading folder" screen shown while a scan runs. */
    private static class ScanningPanel extends JPanel {
        private final JLabel title = Ui.h1("Reading folder\u2026");
        private final JLabel path = Ui.hint(" ");
        private final ProgressLine line = new ProgressLine();

        ScanningPanel() {
            super(new GridBagLayout());
            setOpaque(false);
            JPanel column = new JPanel(new GridBagLayout());
            column.setOpaque(false);

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.anchor = GridBagConstraints.CENTER;
            gc.gridy = 0; column.add(title, gc);
            gc.gridy = 1; gc.insets = new Insets(Theme.S, 0, 0, 0); column.add(path, gc);
            gc.gridy = 2; gc.insets = new Insets(Theme.L, 0, 0, 0); column.add(line, gc);
            add(column);
        }

        void begin(String folder) {
            title.setText("Reading " + folder);
            path.setText(" ");
            line.start();
        }

        void setCurrentPath(String p) {
            path.setText(p.length() > 90 ? "\u2026" + p.substring(p.length() - 88) : p);
        }

        void end() { line.stop(); }
    }

    /** An indeterminate progress line — the file count is unknown up front. */
    private static class ProgressLine extends JComponent {
        private final Timer timer;
        private int offset;

        ProgressLine() {
            setPreferredSize(new Dimension(420, 6));
            timer = new Timer(16, e -> {
                offset = (offset + 4) % (420 + 160);
                repaint();
            });
        }

        void start() { timer.start(); }
        void stop()  { timer.stop(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(Theme.CARD);
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(Theme.LINE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, h, h));
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(offset - 160, 0, 160, h, h, h);
            g2.dispose();
        }
    }
}
