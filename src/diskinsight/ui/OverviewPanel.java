package diskinsight.ui;

import diskinsight.model.Category;
import diskinsight.model.FileRecord;
import diskinsight.model.Rule;
import diskinsight.util.Fmt;

import java.awt.*;
import java.util.List;
import javax.swing.*;

/** The dashboard: what is in this folder, and what the rules think about it. */
public class OverviewPanel extends JPanel {

    private final MainFrame app;

    private final HeroLine heroLine = new HeroLine();
    private final JLabel eyebrow = Ui.mono("");
    private final StorageTape tape = new StorageTape();
    private final JPanel legend = Ui.grid(4, Theme.S);
    private final JPanel stats = Ui.grid(3, Theme.M);
    private final JPanel ruleCards = Ui.grid(2, Theme.M);

    public OverviewPanel(MainFrame app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);

        tape.onSelect(app::showFilesForCategory);

        Ui.Stack stack = new Ui.Stack();
        stack.push(eyebrow, Theme.S);
        stack.push(heroLine, Theme.S);

        JLabel note = Ui.muted("<html><body style='width:520px'>Every block below is one "
                + "file type, sized by how much space it takes. Hover a block to see the "
                + "type; click it to open that group in the file list.</body></html>");
        stack.push(note, Theme.XS);

        stack.push(tape, Theme.L);
        stack.push(legend, Theme.M);

        stack.push(Ui.sectionHeader("At a glance",
                "Counts for the groups people usually clear out first."), Theme.XL);
        stack.push(stats, Theme.S);

        stack.push(Ui.sectionHeader("Recommended for cleanup",
                "Files matched by your active rules."), Theme.XL);
        stack.push(ruleCards, Theme.S);

        stack.push(Ui.notice("DiskInsight never deletes anything.",
                "It only points at files worth a second look. Open the file list to "
                + "review a group, and decide for yourself what stays."), Theme.M);
        stack.finish();

        add(stack, BorderLayout.CENTER);
    }

    /** Rebuilds every figure on the screen from the current file list. */
    public void refresh() {
        List<FileRecord> files = app.files();
        long total = 0;
        for (FileRecord f : files) total += f.size;

        eyebrow.setText(app.folder()
                + "   \u00b7   scanned " + Fmt.ago(app.scannedAt())
                + "   \u00b7   including subfolders");
        heroLine.set(Fmt.bytes(total), "across " + Fmt.files(files.size()));

        tape.setFiles(files);
        buildLegend(files, total);
        buildStats(files, total);
        buildRuleCards(files, total);

        revalidate();
        repaint();
    }

    /* ------------------------------------------------------------------ */

    private void buildLegend(List<FileRecord> files, long total) {
        legend.removeAll();
        for (Category c : sortedCategories(files)) {
            long size = 0;
            int count = 0;
            for (FileRecord f : files) {
                if (f.category == c) { size += f.size; count++; }
            }
            if (count == 0) continue;
            Ui.Chip chip = new Ui.Chip(c.color, c.label, Fmt.bytes(size) + " \u00b7 " + count);
            chip.addActionListener(e -> app.showFilesForCategory(c));
            legend.add(chip);
        }
    }

    private Category[] sortedCategories(List<FileRecord> files) {
        Category[] cats = Category.values().clone();
        long[] sizes = new long[cats.length];
        for (FileRecord f : files) sizes[f.category.ordinal()] += f.size;
        // simple insertion sort, largest group first
        for (int i = 1; i < cats.length; i++) {
            Category c = cats[i];
            long s = sizes[c.ordinal()];
            int j = i - 1;
            while (j >= 0 && sizes[cats[j].ordinal()] < s) {
                cats[j + 1] = cats[j];
                j--;
            }
            cats[j + 1] = c;
        }
        return cats;
    }

    private void buildStats(List<FileRecord> files, long total) {
        stats.removeAll();

        long bigCount = 0, bigSize = 0;
        long tmpCount = 0, tmpSize = 0;
        long zipCount = 0, zipSize = 0;
        long pdfCount = 0, pdfSize = 0;
        long flagCount = 0, flagSize = 0;

        for (FileRecord f : files) {
            if (f.size > 500 * Fmt.MB) { bigCount++; bigSize += f.size; }
            if (f.category == Category.TEMP) { tmpCount++; tmpSize += f.size; }
            if (f.extension.equals("zip")) { zipCount++; zipSize += f.size; }
            if (f.extension.equals("pdf")) { pdfCount++; pdfSize += f.size; }
            if (f.flagged) { flagCount++; flagSize += f.size; }
        }

        stats.add(new Ui.StatCard("Total files", Fmt.count(files.size()),
                Fmt.bytes(total) + " in total", false));
        stats.add(new Ui.StatCard("Over 500 MB", Fmt.count(bigCount),
                Fmt.bytes(bigSize) + " between them", false));
        stats.add(new Ui.StatCard("Temporary files", Fmt.count(tmpCount),
                ".tmp .part .log \u00b7 " + Fmt.bytes(tmpSize), false));
        stats.add(new Ui.StatCard("ZIP files", Fmt.count(zipCount),
                Fmt.bytes(zipSize), false));
        stats.add(new Ui.StatCard("PDF files", Fmt.count(pdfCount),
                Fmt.bytes(pdfSize), false));
        stats.add(new Ui.StatCard("Worth reviewing", Fmt.count(flagCount),
                Fmt.bytes(flagSize) + " could be freed", true));
    }

    private void buildRuleCards(List<FileRecord> files, long total) {
        ruleCards.removeAll();

        boolean any = false;
        for (Rule r : app.rules()) {
            if (!r.enabled) continue;
            any = true;

            int count = 0;
            long size = 0;
            for (FileRecord f : files) {
                if (new diskinsight.engine.RuleEngine(r).evaluate(f)) { count++; size += f.size; }
            }

            Ui.Card card = new Ui.Card(new BorderLayout(0, Theme.S));

            JPanel top = new JPanel(new BorderLayout(Theme.S, 0));
            top.setOpaque(false);
            JPanel titles = new JPanel(new GridLayout(0, 1, 0, 2));
            titles.setOpaque(false);
            titles.add(Ui.label(r.name, Theme.BODY_BOLD, Theme.INK));
            titles.add(Ui.mono(r.describe()));
            top.add(titles, BorderLayout.CENTER);
            top.add(Ui.label(String.valueOf(count), Theme.H1, Theme.INK), BorderLayout.EAST);

            JPanel bottom = new JPanel(new BorderLayout(0, Theme.XS));
            bottom.setOpaque(false);
            bottom.add(new MiniBar(size, total), BorderLayout.NORTH);
            bottom.add(Ui.muted(Fmt.bytes(size) + " \u2014 "
                    + Fmt.percent(size, total) + " of this folder"), BorderLayout.CENTER);

            Ui.Btn review = Ui.ghostSmall("Review these files");
            review.addActionListener(e -> app.showFilesForRule(r));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            actions.setOpaque(false);
            actions.add(review);
            bottom.add(actions, BorderLayout.SOUTH);

            card.add(top, BorderLayout.NORTH);
            card.add(bottom, BorderLayout.CENTER);
            ruleCards.add(card);
        }

        if (!any) {
            Ui.Card empty = new Ui.Card(new BorderLayout(0, Theme.XS));
            empty.add(Ui.h3("No rules are switched on"), BorderLayout.NORTH);
            empty.add(Ui.muted("Open the Rules tab and turn one on to start "
                    + "flagging files."), BorderLayout.CENTER);
            ruleCards.add(empty);
        }
    }

    /* ------------------------------------------------------------------
       Small painted pieces
       ------------------------------------------------------------------ */

    /** "18.62 GB across 842 files" — big figure, quieter tail. */
    private static class HeroLine extends JComponent {
        private String big = "\u2014";
        private String tail = "";

        HeroLine() {
            setPreferredSize(new Dimension(400, 54));
        }

        void set(String big, String tail) {
            this.big = big;
            this.tail = tail;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            g2.setFont(Theme.HERO);
            FontMetrics fm = g2.getFontMetrics();
            int y = fm.getAscent();
            g2.setColor(Theme.INK);
            g2.drawString(big, 0, y);
            int x = fm.stringWidth(big) + 14;
            g2.setColor(Theme.INK_3);
            g2.drawString(tail, x, y);
            g2.dispose();
        }
    }

    /** The thin proportion bar inside a rule card. */
    private static class MiniBar extends JComponent {
        private final long part, total;

        MiniBar(long part, long total) {
            this.part = part;
            this.total = total;
            setPreferredSize(new Dimension(100, 6));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth();
            g2.setColor(Theme.PAPER);
            g2.fillRoundRect(0, 0, w, 6, 6, 6);
            if (total > 0) {
                int fw = (int) Math.min(w, Math.round(part * (double) w / total));
                g2.setColor(Theme.FLAG);
                g2.fillRoundRect(0, 0, Math.max(fw, 3), 6, 6, 6);
            }
            g2.dispose();
        }
    }
}
