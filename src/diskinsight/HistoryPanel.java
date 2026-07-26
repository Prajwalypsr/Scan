package diskinsight;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;

/** Past scans, newest first, with the change in size since the one before it. */
public class HistoryPanel extends JPanel {

    private final MainFrame app;
    private final JPanel rows = new JPanel();
    private final SimpleDateFormat stamp = new SimpleDateFormat("dd MMM yyyy, HH:mm");

    public HistoryPanel(MainFrame app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);

        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);

        Ui.Card card = new Ui.Card(new BorderLayout());
        card.flush();
        card.add(rows, BorderLayout.CENTER);

        Ui.Stack stack = new Ui.Stack();
        stack.push(Ui.sectionHeader("Scan history",
                "Saved scans, so you can see whether a folder is growing."), 0);
        stack.push(card, Theme.S);
        stack.push(Ui.notice("Scans are kept in the database.",
                "Each row is one completed scan. Nothing here changes the files "
                + "on disk."), Theme.M);
        stack.finish();

        add(stack, BorderLayout.CENTER);
    }

    public void refresh() {
        rows.removeAll();
        List<ScanRecord> history = app.history();

        if (history.isEmpty()) {
            JPanel empty = new JPanel(new BorderLayout(0, Theme.XS));
            empty.setOpaque(false);
            empty.setBorder(BorderFactory.createEmptyBorder(
                    Theme.XL, Theme.CARD_PAD, Theme.XL, Theme.CARD_PAD));
            empty.add(Ui.h3("No scans saved yet"), BorderLayout.NORTH);
            empty.add(Ui.muted("Scan a folder and the result is recorded here."),
                    BorderLayout.CENTER);
            rows.add(empty);
        }

        for (int i = 0; i < history.size(); i++) {
            if (i > 0) rows.add(Ui.hairline());
            ScanRecord current = history.get(i);
            ScanRecord older = (i + 1 < history.size()) ? history.get(i + 1) : null;
            rows.add(buildRow(current, older, i == 0));
        }

        revalidate();
        repaint();
    }

    private JPanel buildRow(ScanRecord scan, ScanRecord previous, boolean latest) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(14, Theme.CARD_PAD, 14, Theme.CARD_PAD));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 0, Theme.L);

        gc.gridx = 0;
        row.add(Ui.mono(latest ? "Just now" : stamp.format(new Date(scan.scannedAt))), gc);

        gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        row.add(Ui.label(scan.folder, Theme.BODY_BOLD, Theme.INK), gc);

        gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
        gc.gridx = 2;
        row.add(Ui.mono(Fmt.files(scan.fileCount)), gc);

        gc.gridx = 3;
        row.add(Ui.mono(Fmt.bytes(scan.totalSize)), gc);

        gc.gridx = 4;
        row.add(buildDelta(scan, previous, latest), gc);

        gc.gridx = 5; gc.insets = new Insets(0, 0, 0, 0);
        row.add(Ui.mono(Fmt.bytes(scan.flaggedSize) + " worth reviewing"), gc);

        return row;
    }

    /** The pill showing growth or shrinkage against the previous scan. */
    private JComponent buildDelta(ScanRecord scan, ScanRecord previous, boolean latest) {
        String text;
        Color fg, bg;

        if (previous == null) {
            text = latest ? "latest" : "first scan";
            fg = Theme.INK_2;
            bg = Theme.PAPER;
        } else {
            long diff = scan.totalSize - previous.totalSize;
            if (diff >= 0) {
                text = "+" + Fmt.bytes(diff) + " since";
                fg = Theme.FLAG;
                bg = Theme.FLAG_SOFT;
            } else {
                text = "\u2212" + Fmt.bytes(-diff) + " since";
                fg = new Color(0x3F7A4E);
                bg = new Color(0xE9F4EC);
            }
        }

        JLabel pill = Ui.label(text, Theme.MONO, fg);
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.aa(g);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.add(pill, BorderLayout.CENTER);
        return wrap;
    }
}
