package diskinsight.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Skin — flat replacements for the stock Swing control painting.
 *
 * Without this, combo boxes and scroll bars keep the default Metal look:
 * bevelled grey arrows and boxed scroll buttons that do not belong next to
 * the flat cards. These delegates are registered once in Theme.install().
 */
public final class Skin {

    private Skin() { }

    public static void install() {
        UIManager.put("ScrollBarUI", FlatScrollBar.class.getName());
        UIManager.put("ComboBoxUI", FlatCombo.class.getName());
        UIManager.put("ComboBox.background", Theme.CARD);
        UIManager.put("ComboBox.foreground", Theme.INK);
        UIManager.put("ComboBox.selectionBackground", Theme.ACCENT_SOFT);
        UIManager.put("ComboBox.selectionForeground", Theme.INK);
        UIManager.put("ComboBox.font", Theme.BODY);
        UIManager.put("List.font", Theme.BODY);
    }

    /* ==================================================================
       Scroll bar — a rounded thumb, no arrow buttons
       ================================================================== */

    public static class FlatScrollBar extends BasicScrollBarUI {

        public static ComponentUI createUI(JComponent c) {
            return new FlatScrollBar();
        }

        @Override
        protected void configureScrollBarColors() {
            thumbColor = Theme.LINE_STRONG;
            trackColor = Theme.PAPER;
        }

        @Override protected JButton createDecreaseButton(int o) { return hiddenButton(); }
        @Override protected JButton createIncreaseButton(int o) { return hiddenButton(); }

        private JButton hiddenButton() {
            JButton b = new JButton();
            Dimension zero = new Dimension(0, 0);
            b.setPreferredSize(zero);
            b.setMinimumSize(zero);
            b.setMaximumSize(zero);
            b.setBorder(BorderFactory.createEmptyBorder());
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = Theme.aa(g);
            g2.setColor(trackColor);
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = Theme.aa(g);
            g2.setColor(isThumbRollover() ? Theme.INK_3 : thumbColor);
            int inset = 3;
            g2.fillRoundRect(r.x + inset, r.y + inset,
                    r.width - inset * 2, r.height - inset * 2, 8, 8);
            g2.dispose();
        }
    }

    /* ==================================================================
       Combo box — flat surface with a simple chevron
       ================================================================== */

    public static class FlatCombo extends BasicComboBoxUI {

        public static ComponentUI createUI(JComponent c) {
            return new FlatCombo();
        }

        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = Theme.aa(g);
                    int w = getWidth(), h = getHeight();
                    int cx = w / 2, cy = h / 2 + 1;
                    g2.setColor(Theme.INK_2);
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND,
                                                 BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                    g2.dispose();
                }
            };
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusable(false);
            b.setPreferredSize(new Dimension(24, 24));
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            Graphics2D g2 = Theme.aa(g);
            g2.setColor(Theme.CARD);
            g2.fillRoundRect(0, 0, comboBox.getWidth() - 1, comboBox.getHeight() - 1,
                             Theme.RADIUS_CTRL, Theme.RADIUS_CTRL);
            g2.dispose();
        }
    }

    /* ==================================================================
       Check box icon — a rounded box that fills with the accent colour
       ================================================================== */

    public static class CheckIcon implements Icon {
        private static final int SIZE = 16;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.aa(g);
            boolean on = (c instanceof AbstractButton b) && b.isSelected();

            g2.setColor(on ? Theme.ACCENT : Theme.CARD);
            g2.fillRoundRect(x, y, SIZE, SIZE, 4, 4);
            g2.setColor(on ? Theme.ACCENT : Theme.LINE_STRONG);
            g2.drawRoundRect(x, y, SIZE, SIZE, 4, 4);

            if (on) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 4, y + 8, x + 7, y + 11);
                g2.drawLine(x + 7, y + 11, x + 12, y + 5);
            }
            g2.dispose();
        }

        @Override public int getIconWidth()  { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }
}
