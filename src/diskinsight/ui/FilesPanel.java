package diskinsight.ui;

import diskinsight.model.Category;
import diskinsight.model.FileRecord;
import diskinsight.model.Rule;
import diskinsight.util.Fmt;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;

/** The file list: search, filter, sort, select, and copy paths out. */
public class FilesPanel extends JPanel {

    private final MainFrame app;

    private final FilesModel model = new FilesModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<FilesModel> sorter = new TableRowSorter<>(model);

    private final JLabel summary = Ui.muted("");
    private final JTextField search = Ui.field("Search by file name or folder");
    private final JComboBox<TypeOption> typeBox = Ui.combo(new TypeOption[0]);
    private final JComboBox<Option> sizeBox = Ui.combo(SIZE_OPTIONS);
    private final JComboBox<Option> ageBox = Ui.combo(AGE_OPTIONS);
    private final JCheckBox onlyFlagged = Ui.checkBox("Only cleanup matches");

    private final SelectionBar selectionBar = new SelectionBar();

    /* filter state */
    private Category filterCategory;
    private Rule filterRule;
    private long filterMinSize;
    private int filterMinAge;

    private boolean updatingControls;
    private long maxVisibleSize = 1;
    private int hoverRow = -1;

    private record Option(String label, long value) {
        @Override public String toString() { return label; }
    }

    private record TypeOption(String label, Category category) {
        @Override public String toString() { return label; }
    }

    private static final Option[] SIZE_OPTIONS = {
        new Option("Any size", 0),
        new Option("Over 10 MB", 10 * Fmt.MB),
        new Option("Over 100 MB", 100 * Fmt.MB),
        new Option("Over 500 MB", 500 * Fmt.MB),
        new Option("Over 1 GB", Fmt.GB)
    };

    private static final Option[] AGE_OPTIONS = {
        new Option("Any age", 0),
        new Option("Older than 30 days", 30),
        new Option("Older than 90 days", 90),
        new Option("Older than 6 months", 180),
        new Option("Older than 1 year", 365)
    };

    public FilesPanel(MainFrame app) {
        super(new BorderLayout(0, Theme.M));
        this.app = app;
        setOpaque(false);

        buildTable();

        JPanel head = new JPanel(new BorderLayout(0, Theme.S));
        head.setOpaque(false);
        head.add(Ui.sectionHeader("Files", ""), BorderLayout.NORTH);
        head.add(summary, BorderLayout.CENTER);
        head.add(buildToolbar(), BorderLayout.SOUTH);

        Ui.Card tableCard = new Ui.Card(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.CARD);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        tableCard.add(sp, BorderLayout.CENTER);

        add(head, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
        add(selectionBar, BorderLayout.SOUTH);
        selectionBar.setVisible(false);
    }

    /* ==================================================================
       Table setup
       ================================================================== */

    private void buildTable() {
        table.setRowHeight(Theme.ROW_H);
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(false);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(Theme.CARD);
        table.setRowSorter(sorter);

        sorter.setSortable(0, false);
        sorter.setSortable(4, false);
        sorter.setComparator(1, (FileRecord a, FileRecord b) ->
                a.name.compareToIgnoreCase(b.name));
        sorter.setSortKeys(List.of(new RowSorter.SortKey(2, SortOrder.DESCENDING)));

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setMinWidth(38);
        cm.getColumn(0).setMaxWidth(38);
        cm.getColumn(2).setPreferredWidth(150);
        cm.getColumn(2).setMaxWidth(170);
        cm.getColumn(3).setPreferredWidth(140);
        cm.getColumn(3).setMaxWidth(160);
        cm.getColumn(4).setPreferredWidth(230);
        cm.getColumn(4).setMaxWidth(260);

        cm.getColumn(0).setCellRenderer(new CheckCell());
        cm.getColumn(0).setCellEditor(new CheckEditor());
        cm.getColumn(1).setCellRenderer(new NameCell());
        cm.getColumn(2).setCellRenderer(new SizeCell());
        cm.getColumn(3).setCellRenderer(new AgeCell());
        cm.getColumn(4).setCellRenderer(new StatusCell());

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new HeaderCell());
        header.setPreferredSize(new Dimension(10, 38));
        header.setReorderingAllowed(false);
        header.setBackground(Theme.CARD);

        table.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r != hoverRow) { hoverRow = r; table.repaint(); }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hoverRow = -1; table.repaint(); }
        });
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new GridBagLayout());
        bar.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, Theme.S);

        gc.gridx = 0; gc.weightx = 1;
        search.setPreferredSize(new Dimension(240, 36));
        bar.add(search, gc);

        gc.weightx = 0;
        gc.gridx = 1; bar.add(sized(typeBox, 190), gc);
        gc.gridx = 2; bar.add(sized(sizeBox, 140), gc);
        gc.gridx = 3; bar.add(sized(ageBox, 175), gc);
        gc.gridx = 4; gc.insets = new Insets(0, 0, 0, 0);
        bar.add(onlyFlagged, gc);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        typeBox.addActionListener(e -> {
            if (updatingControls) return;
            TypeOption t = (TypeOption) typeBox.getSelectedItem();
            filterCategory = t == null ? null : t.category();
            filterRule = null;
            applyFilters();
        });
        sizeBox.addActionListener(e -> {
            if (updatingControls) return;
            filterMinSize = ((Option) sizeBox.getSelectedItem()).value();
            filterRule = null;
            applyFilters();
        });
        ageBox.addActionListener(e -> {
            if (updatingControls) return;
            filterMinAge = (int) ((Option) ageBox.getSelectedItem()).value();
            filterRule = null;
            applyFilters();
        });
        onlyFlagged.addActionListener(e -> {
            if (!updatingControls) applyFilters();
        });

        return bar;
    }

    private JComponent sized(JComponent c, int width) {
        c.setPreferredSize(new Dimension(width, 36));
        return c;
    }

    /* ==================================================================
       Data + filtering
       ================================================================== */

    /** Called after every scan or rule change. */
    public void refresh() {
        model.setData(app.files());
        rebuildTypeOptions();
        applyFilters();
    }

    private void rebuildTypeOptions() {
        updatingControls = true;
        List<TypeOption> options = new ArrayList<>();
        options.add(new TypeOption("All types", null));

        for (Category c : Category.values()) {
            int n = 0;
            for (FileRecord f : app.files()) if (f.category == c) n++;
            if (n > 0) options.add(new TypeOption(c.label + " (" + n + ")", c));
        }

        typeBox.setModel(new DefaultComboBoxModel<>(options.toArray(new TypeOption[0])));
        for (TypeOption o : options) {
            if (o.category() == filterCategory) { typeBox.setSelectedItem(o); break; }
        }
        updatingControls = false;
    }

    /** Clears every filter back to "show everything". */
    public void clearFilters() {
        updatingControls = true;
        filterCategory = null;
        filterRule = null;
        filterMinSize = 0;
        filterMinAge = 0;
        search.setText("");
        sizeBox.setSelectedIndex(0);
        ageBox.setSelectedIndex(0);
        onlyFlagged.setSelected(false);
        if (typeBox.getItemCount() > 0) typeBox.setSelectedIndex(0);
        updatingControls = false;
    }

    public void showCategory(Category c) {
        clearFilters();
        filterCategory = c;
        updatingControls = true;
        for (int i = 0; i < typeBox.getItemCount(); i++) {
            if (typeBox.getItemAt(i).category() == c) { typeBox.setSelectedIndex(i); break; }
        }
        updatingControls = false;
        applyFilters();
    }

    public void showRule(Rule r) {
        clearFilters();
        filterRule = r;
        applyFilters();
    }

    private void applyFilters() {
        final String q = search.getText().trim().toLowerCase();

        sorter.setRowFilter(new RowFilter<FilesModel, Integer>() {
            @Override
            public boolean include(Entry<? extends FilesModel, ? extends Integer> entry) {
                FileRecord f = model.get(entry.getIdentifier());
                if (filterRule != null && !filterRule.matches(f)) return false;
                if (filterCategory != null && f.category != filterCategory) return false;
                if (filterMinSize > 0 && f.size < filterMinSize) return false;
                if (filterMinAge > 0 && Fmt.daysOld(f.modified) < filterMinAge) return false;
                if (onlyFlagged.isSelected() && !f.flagged) return false;
                if (!q.isEmpty()
                        && !f.name.toLowerCase().contains(q)
                        && !f.folder.toLowerCase().contains(q)) return false;
                return true;
            }
        });

        long shownSize = 0;
        maxVisibleSize = 1;
        for (int i = 0; i < table.getRowCount(); i++) {
            FileRecord f = model.get(table.convertRowIndexToModel(i));
            shownSize += f.size;
            maxVisibleSize = Math.max(maxVisibleSize, f.size);
        }

        summary.setText(Fmt.count(table.getRowCount()) + " of "
                + Fmt.count(app.files().size()) + " files  \u00b7  " + Fmt.bytes(shownSize)
                + (filterRule != null ? "  \u00b7  filtered by rule \u201c" + filterRule.name + "\u201d" : ""));

        selectionBar.update();
        table.repaint();
    }

    /* ==================================================================
       Selection
       ================================================================== */

    private List<FileRecord> selectedFiles() {
        List<FileRecord> out = new ArrayList<>();
        for (FileRecord f : app.files()) if (f.selected) out.add(f);
        return out;
    }

    private void clearSelection() {
        for (FileRecord f : app.files()) f.selected = false;
        model.fireTableDataChanged();
        selectionBar.update();
    }

    /** The dark bar that appears once files are ticked. */
    private class SelectionBar extends JPanel {
        private final JLabel text = Ui.label("", Theme.BODY, Color.WHITE);

        SelectionBar() {
            super(new BorderLayout(Theme.M, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

            Ui.Btn copy = Ui.onDarkSmall("Copy file paths");
            copy.addActionListener(e -> {
                List<FileRecord> sel = selectedFiles();
                StringBuilder sb = new StringBuilder();
                for (FileRecord f : sel) sb.append(f.fullPath()).append(System.lineSeparator());
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(sb.toString()), null);
                copy.setText("Copied " + sel.size());
                Timer t = new Timer(1600, ev -> copy.setText("Copy file paths"));
                t.setRepeats(false);
                t.start();
            });

            Ui.Btn clear = Ui.onDarkSmall("Clear selection");
            clear.addActionListener(e -> clearSelection());

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.S, 0));
            right.setOpaque(false);
            right.add(copy);
            right.add(clear);

            add(text, BorderLayout.WEST);
            add(right, BorderLayout.EAST);
        }

        void update() {
            List<FileRecord> sel = selectedFiles();
            if (sel.isEmpty()) { setVisible(false); return; }
            long size = 0;
            for (FileRecord f : sel) size += f.size;
            text.setText(Fmt.files(sel.size()) + " selected  \u00b7  " + Fmt.bytes(size));
            setVisible(true);
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            Theme.surface(g2, getWidth(), getHeight(), Theme.RADIUS, Theme.INK, null);
            g2.dispose();
        }
    }

    /* ==================================================================
       Table model
       ================================================================== */

    private class FilesModel extends AbstractTableModel {
        private final String[] columns = {"", "Name", "Size", "Last modified", "Status"};
        private List<FileRecord> data = new ArrayList<>();

        void setData(List<FileRecord> data) {
            this.data = data;
            fireTableDataChanged();
        }

        FileRecord get(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int c) { return columns[c]; }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 0 -> Boolean.class;
                case 2, 3 -> Long.class;
                default -> FileRecord.class;
            };
        }

        @Override public boolean isCellEditable(int r, int c) { return c == 0; }

        @Override
        public Object getValueAt(int r, int c) {
            FileRecord f = data.get(r);
            return switch (c) {
                case 0 -> f.selected;
                case 2 -> f.size;
                case 3 -> f.modified;
                default -> f;
            };
        }

        @Override
        public void setValueAt(Object value, int r, int c) {
            if (c != 0) return;
            data.get(r).selected = Boolean.TRUE.equals(value);
            fireTableRowsUpdated(r, r);
            selectionBar.update();
        }
    }

    /* ==================================================================
       Cell renderers
       ================================================================== */

    private Color rowBackground(int viewRow, FileRecord f) {
        if (f != null && f.selected) return Theme.ACCENT_SOFT;
        if (viewRow == hoverRow) return Theme.ROW_HOVER;
        return Theme.CARD;
    }

    private class CheckCell extends JCheckBox implements TableCellRenderer {
        CheckCell() {
            setHorizontalAlignment(CENTER);
            setOpaque(true);
            setIcon(new Skin.CheckIcon());
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                                                       boolean focus, int row, int col) {
            setSelected(Boolean.TRUE.equals(v));
            // FilesPanel.this.model — plain "model" would resolve to AbstractButton.model
            FileRecord f = FilesPanel.this.model.get(t.convertRowIndexToModel(row));
            setBackground(rowBackground(row, f));
            return this;
        }
    }

    private static class CheckEditor extends DefaultCellEditor {
        CheckEditor() {
            super(new JCheckBox());
            JCheckBox b = (JCheckBox) getComponent();
            b.setHorizontalAlignment(SwingConstants.CENTER);
            b.setIcon(new Skin.CheckIcon());
            b.setBackground(Theme.ACCENT_SOFT);
        }
    }

    private class NameCell extends JComponent implements TableCellRenderer {
        private FileRecord f;
        private int viewRow;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                                                       boolean focus, int row, int col) {
            this.f = (FileRecord) v;
            this.viewRow = row;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (f == null) return;
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(rowBackground(viewRow, f));
            g2.fillRect(0, 0, w, h);

            // extension badge
            g2.setColor(f.category.color);
            g2.fillRoundRect(12, h / 2 - 11, 36, 22, 5, 5);
            g2.setFont(Theme.MONO_BOLD.deriveFont(9.5f));
            FontMetrics bm = g2.getFontMetrics();
            String badge = f.badge();
            g2.setColor(Color.WHITE);
            g2.drawString(badge, 12 + (36 - bm.stringWidth(badge)) / 2,
                    h / 2 + bm.getAscent() / 2 - 1);

            int textX = 58;
            int available = w - textX - 12;

            g2.setFont(Theme.BODY);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Theme.INK);
            g2.drawString(Theme.clip(g2, f.name, available), textX, h / 2 - 1);

            g2.setFont(Theme.MONO.deriveFont(11f));
            g2.setColor(Theme.INK_3);
            g2.drawString(Theme.clip(g2, f.folder, available), textX,
                    h / 2 + fm.getAscent() - 1);
            g2.dispose();
        }
    }

    private class SizeCell extends JComponent implements TableCellRenderer {
        private FileRecord f;
        private int viewRow;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                                                       boolean focus, int row, int col) {
            this.f = model.get(t.convertRowIndexToModel(row));
            this.viewRow = row;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (f == null) return;
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(rowBackground(viewRow, f));
            g2.fillRect(0, 0, w, h);

            g2.setFont(Theme.MONO);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Theme.INK);
            g2.drawString(Fmt.bytes(f.size), 14, h / 2 - 1);

            int barW = Math.min(88, w - 28);
            int y = h / 2 + 5;
            g2.setColor(Theme.PAPER);
            g2.fillRoundRect(14, y, barW, 4, 4, 4);
            int fill = (int) Math.max(3, Math.round(f.size * (double) barW / maxVisibleSize));
            g2.setColor(f.category.color);
            g2.fillRoundRect(14, y, Math.min(fill, barW), 4, 4, 4);
            g2.dispose();
        }
    }

    private class AgeCell extends JComponent implements TableCellRenderer {
        private FileRecord f;
        private int viewRow;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                                                       boolean focus, int row, int col) {
            this.f = model.get(t.convertRowIndexToModel(row));
            this.viewRow = row;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (f == null) return;
            Graphics2D g2 = Theme.aa(g);
            g2.setColor(rowBackground(viewRow, f));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(Theme.MONO);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Theme.INK_2);
            g2.drawString(Fmt.ago(f.modified), 14,
                    getHeight() / 2 + fm.getAscent() / 2 - 2);
            g2.dispose();
        }
    }

    private class StatusCell extends JComponent implements TableCellRenderer {
        private FileRecord f;
        private int viewRow;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                                                       boolean focus, int row, int col) {
            this.f = (FileRecord) v;
            this.viewRow = row;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (f == null) return;
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(rowBackground(viewRow, f));
            g2.fillRect(0, 0, w, h);

            if (!f.flagged) {
                g2.setFont(Theme.BODY);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Theme.INK_3);
                g2.drawString("Keep", 14, h / 2 + fm.getAscent() / 2 - 2);
                g2.dispose();
                return;
            }

            String pill = "Recommended for cleanup";
            g2.setFont(Theme.LABEL);
            FontMetrics fm = g2.getFontMetrics();
            int pw = fm.stringWidth(pill) + 26;
            int ph = 20;
            int py = h / 2 - ph;

            g2.setColor(Theme.FLAG_SOFT);
            g2.fillRoundRect(14, py, pw, ph, ph, ph);
            g2.setColor(Theme.FLAG_LINE);
            g2.drawRoundRect(14, py, pw, ph, ph, ph);
            g2.setColor(Theme.FLAG);
            g2.fillOval(23, py + ph / 2 - 2, 5, 5);
            g2.drawString(pill, 33, py + ph / 2 + fm.getAscent() / 2 - 1);

            g2.setFont(Theme.SMALL);
            FontMetrics rm = g2.getFontMetrics();
            g2.setColor(Theme.INK_3);
            g2.drawString(Theme.clip(g2, f.reason(), w - 28), 14,
                    py + ph + rm.getAscent() + 2);
            g2.dispose();
        }
    }

    private static class HeaderCell extends JComponent implements TableCellRenderer {
        private String text = "";
        private String arrow = "";

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                                                       boolean focus, int row, int col) {
            this.text = v == null ? "" : v.toString().toUpperCase();
            this.arrow = "";
            List<? extends RowSorter.SortKey> keys = t.getRowSorter().getSortKeys();
            if (!keys.isEmpty()) {
                RowSorter.SortKey k = keys.get(0);
                if (k.getColumn() == t.convertColumnIndexToModel(col)) {
                    arrow = k.getSortOrder() == SortOrder.ASCENDING ? "  \u25b2" : "  \u25bc";
                }
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(Theme.CARD);
            g2.fillRect(0, 0, w, h);
            g2.setColor(Theme.LINE);
            g2.fillRect(0, h - 1, w, 1);
            g2.setFont(Theme.LABEL);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Theme.INK_2);
            g2.drawString(text + arrow, 14, h / 2 + fm.getAscent() / 2 - 1);
            g2.dispose();
        }
    }
}
