package diskinsight;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * The storage tape: one bar showing the whole folder, split into blocks sized
 * by how much space each file type takes, with a ruler marked in GB beneath it.
 *
 * Hovering a block highlights it and shows the exact figures; clicking one
 * opens that group in the file list.
 */
public class StorageTape extends JComponent {

    private static final int BAR_H = 52;
    private static final int RULER_H = 28;

    private static class Segment {
        Category category;
        long size;
        int count;
        int x, width;
    }

    private final List<Segment> segments = new ArrayList<>();
    private long total;
    private int hovered = -1;
    private Consumer<Category> onSelect = c -> { };

    public StorageTape() {
        setOpaque(false);
        setPreferredSize(new Dimension(600, BAR_H + RULER_H));
        setToolTipText("");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int was = hovered;
                hovered = indexAt(e.getX(), e.getY());
                if (was != hovered) repaint();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hovered = -1; repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                int i = indexAt(e.getX(), e.getY());
                if (i >= 0) onSelect.accept(segments.get(i).category);
            }
        });
    }

    public StorageTape onSelect(Consumer<Category> handler) {
        this.onSelect = handler;
        return this;
    }

    /** Recomputes the blocks from the current file list. */
    public void setFiles(List<FileRecord> files) {
        segments.clear();
        total = 0;
        for (Category c : Category.values()) {
            Segment s = new Segment();
            s.category = c;
            for (FileRecord f : files) {
                if (f.category == c) { s.size += f.size; s.count++; }
            }
            if (s.count > 0) segments.add(s);
            total += s.size;
        }
        segments.sort((a, b) -> Long.compare(b.size, a.size));
        hovered = -1;
        revalidate();
        repaint();
    }

    private void layoutSegments() {
        int w = getWidth();
        if (w <= 0 || total <= 0) return;
        int x = 0;
        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            int sw = (i == segments.size() - 1)
                    ? w - x
                    : Math.max(4, (int) Math.round(s.size * (double) w / total));
            s.x = x;
            s.width = sw;
            x += sw;
        }
    }

    private int indexAt(int x, int y) {
        if (y > BAR_H) return -1;
        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            if (x >= s.x && x < s.x + s.width) return i;
        }
        return -1;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = indexAt(e.getX(), e.getY());
        if (i < 0) return null;
        Segment s = segments.get(i);
        return s.category.label + "  \u2014  " + Fmt.bytes(s.size)
                + "  \u00b7  " + s.count + " files  \u00b7  " + Fmt.percent(s.size, total);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.aa(g);
        int w = getWidth();
        layoutSegments();

        // --- the bar -------------------------------------------------
        RoundRectangle2D clip = new RoundRectangle2D.Float(0, 0, w, BAR_H, 8, 8);
        Shape old = g2.getClip();
        g2.setColor(Theme.CARD);
        g2.fill(clip);
        g2.clip(clip);

        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            Color base = s.category.color;
            g2.setColor(i == hovered ? base.brighter() : base);
            g2.fillRect(s.x, 0, s.width, BAR_H);

            // temporary files get a hatched fill so they read as "not real content"
            if (s.category == Category.TEMP) {
                g2.setColor(new Color(255, 255, 255, 120));
                g2.setStroke(new BasicStroke(3f));
                for (int d = -BAR_H; d < s.width + BAR_H; d += 7) {
                    g2.drawLine(s.x + d, BAR_H, s.x + d + BAR_H, 0);
                }
                g2.setStroke(new BasicStroke(1f));
            }
            // hairline gap between blocks
            if (i < segments.size() - 1) {
                g2.setColor(new Color(255, 255, 255, 190));
                g2.fillRect(s.x + s.width - 1, 0, 1, BAR_H);
            }
        }
        g2.setClip(old);
        g2.setColor(Theme.LINE);
        g2.draw(clip);

        // --- the ruler -----------------------------------------------
        g2.setFont(Theme.MONO);
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i <= 4; i++) {
            int x = (int) Math.round(w * i / 4.0);
            int tickX = Math.min(Math.max(x, 0), w - 1);
            g2.setColor(Theme.LINE_STRONG);
            g2.drawLine(tickX, BAR_H + 4, tickX, BAR_H + 10);

            String text = i == 0 ? "0" : Fmt.bytes(total * i / 4);
            int tw = fm.stringWidth(text);
            int tx = tickX - tw / 2;
            if (i == 0) tx = 0;
            if (i == 4) tx = w - tw;
            g2.setColor(Theme.INK_3);
            g2.drawString(text, tx, BAR_H + 10 + fm.getAscent() + 2);
        }
        g2.dispose();
    }
}
