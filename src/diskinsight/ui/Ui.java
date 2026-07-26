package diskinsight.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * Ui — the component library.
 *
 * Every screen is assembled from these pieces, so a card on the Overview
 * screen is pixel-identical to a card on the Rules screen. No screen paints
 * its own surfaces or picks its own paddings.
 */
public final class Ui {

    private Ui() { }

    /* ==================================================================
       Text
       ================================================================== */

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JLabel hero(String text)  { return label(text, Theme.HERO, Theme.INK); }
    public static JLabel h1(String text)    { return label(text, Theme.H1,   Theme.INK); }
    public static JLabel h2(String text)    { return label(text, Theme.H2,   Theme.INK); }
    public static JLabel h3(String text)    { return label(text, Theme.H3,   Theme.INK); }
    public static JLabel body(String text)  { return label(text, Theme.BODY, Theme.INK); }
    public static JLabel muted(String text) { return label(text, Theme.SMALL, Theme.INK_2); }
    public static JLabel hint(String text)  { return label(text, Theme.SMALL, Theme.INK_3); }
    public static JLabel mono(String text)  { return label(text, Theme.MONO, Theme.INK_2); }

    /** Small uppercase caption used above values and over form fields. */
    public static JLabel caption(String text) {
        JLabel l = label(text.toUpperCase(), Theme.LABEL, Theme.INK_2);
        l.putClientProperty("tracking", Boolean.TRUE);
        return l;
    }

    /* ==================================================================
       Layout
       ================================================================== */

    /** Vertical stack: one column, children stretched to full width. */
    public static class Stack extends JPanel {
        private int row = 0;

        public Stack() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        /** Adds a component with a gap above it. */
        public Stack push(Component c, int gapAbove) {
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = row++;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.insets = new Insets(gapAbove, 0, 0, 0);
            add(c, gc);
            return this;
        }

        public Stack push(Component c) { return push(c, Theme.M); }

        /** Pushes everything to the top; call once after the last child. */
        public Stack finish() {
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = row++;
            gc.weighty = 1;
            gc.fill = GridBagConstraints.BOTH;
            add(Box.createGlue(), gc);
            return this;
        }
    }

    /** Left-aligned horizontal row with a consistent gap. */
    public static JPanel row(int gap, Component... children) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        for (Component c : children) p.add(c);
        return p;
    }

    /** Equal-width grid used for the stat tiles, legend chips and rule cards. */
    public static JPanel grid(int columns, int gap) {
        JPanel p = new JPanel(new GridLayout(0, columns, gap, gap));
        p.setOpaque(false);
        return p;
    }

    public static Component gap(int height) {
        return Box.createRigidArea(new Dimension(1, height));
    }

    public static Border pad(int t, int r, int b, int l) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }

    /* ==================================================================
       Card — the one surface used everywhere
       ================================================================== */

    public static class Card extends JPanel {
        private Color fill = Theme.CARD;
        private Color border = Theme.LINE;
        private Color accentEdge;   // optional 3px bar on the left

        public Card() { this(new BorderLayout()); }

        public Card(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(
                    Theme.CARD_PAD, Theme.CARD_PAD, Theme.CARD_PAD, Theme.CARD_PAD));
        }

        public Card colors(Color fill, Color border) {
            this.fill = fill;
            this.border = border;
            return this;
        }

        public Card accentEdge(Color c) {
            this.accentEdge = c;
            setBorder(BorderFactory.createEmptyBorder(
                    Theme.CARD_PAD, Theme.CARD_PAD + 3, Theme.CARD_PAD, Theme.CARD_PAD));
            return this;
        }

        /** Removes the inner padding, for cards that hold their own rows. */
        public Card flush() {
            setBorder(BorderFactory.createEmptyBorder());
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            Theme.surface(g2, getWidth(), getHeight(), Theme.RADIUS, fill, border);
            if (accentEdge != null) {
                g2.setColor(accentEdge);
                g2.fillRoundRect(0, 0, 6, getHeight() - 1, Theme.RADIUS, Theme.RADIUS);
                g2.fillRect(3, 0, 3, getHeight() - 1);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Section title with a one-line explanation beside it. */
    public static JPanel sectionHeader(String title, String subtitle) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.S, 0));
        p.setOpaque(false);
        JLabel t = h2(title);
        JLabel s = muted(subtitle);
        s.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0)); // sit on the baseline
        p.add(t);
        p.add(s);
        return p;
    }

    /** The "nothing is deleted" reassurance bar. */
    public static Card notice(String boldPart, String rest) {
        Card c = new Card(new BorderLayout());
        c.accentEdge(Theme.INK);
        JLabel l = new JLabel("<html><body style='width:640px'><b>" + boldPart
                + "</b> " + rest + "</body></html>");
        l.setFont(Theme.BODY);
        l.setForeground(Theme.INK_2);
        c.add(l, BorderLayout.CENTER);
        return c;
    }

    /* ==================================================================
       Stat tile
       ================================================================== */

    public static class StatCard extends Card {
        public StatCard(String captionText, String value, String meta, boolean flagged) {
            super(new GridBagLayout());
            if (flagged) colors(Theme.FLAG_SOFT, Theme.FLAG_LINE);

            JLabel k = caption(captionText);
            JLabel v = label(value, Theme.H1, flagged ? Theme.FLAG : Theme.INK);
            JLabel m = hint(meta);

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.anchor = GridBagConstraints.WEST;
            gc.gridy = 0; add(k, gc);
            gc.insets = new Insets(Theme.XS, 0, 0, 0);
            gc.gridy = 1; add(v, gc);
            gc.insets = new Insets(2, 0, 0, 0);
            gc.gridy = 2; add(m, gc);
            gc.gridy = 3; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
            add(Box.createGlue(), gc);
        }
    }

    /* ==================================================================
       Buttons
       ================================================================== */

    public enum BtnKind { PRIMARY, GHOST, ON_DARK }

    public static class Btn extends JButton {
        private final BtnKind kind;
        private final boolean small;

        public Btn(String text, BtnKind kind, boolean small) {
            super(text);
            this.kind = kind;
            this.small = small;
            setFont(Theme.BODY_BOLD);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            int padY = small ? 7 : 10;
            int padX = small ? 12 : 16;
            setBorder(BorderFactory.createEmptyBorder(padY, padX, padY, padX));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            boolean hot = getModel().isRollover() && isEnabled();
            boolean down = getModel().isPressed() && isEnabled();
            float alpha = isEnabled() ? 1f : 0.45f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            Color fill, border, text;
            switch (kind) {
                case PRIMARY -> {
                    fill = down ? Theme.ACCENT_DARK : (hot ? Theme.ACCENT_DARK : Theme.ACCENT);
                    border = null;
                    text = Color.WHITE;
                }
                case ON_DARK -> {
                    fill = hot ? new Color(255, 255, 255, 34) : new Color(255, 255, 255, 0);
                    border = new Color(255, 255, 255, 90);
                    text = Color.WHITE;
                }
                default -> {
                    fill = hot ? Theme.PAPER : Theme.CARD;
                    border = Theme.LINE_STRONG;
                    text = Theme.INK;
                }
            }
            Theme.surface(g2, getWidth(), getHeight(), Theme.RADIUS_CTRL, fill, border);

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(text);
            int tx = (getWidth() - fm.stringWidth(getText())) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    public static Btn primary(String text)     { return new Btn(text, BtnKind.PRIMARY, false); }
    public static Btn ghost(String text)       { return new Btn(text, BtnKind.GHOST, false); }
    public static Btn ghostSmall(String text)  { return new Btn(text, BtnKind.GHOST, true); }
    public static Btn onDarkSmall(String text) { return new Btn(text, BtnKind.ON_DARK, true); }

    /* ==================================================================
       Toggle switch (Rules screen)
       ================================================================== */

    public static class Toggle extends JComponent {
        private boolean on;
        private Runnable onChange = () -> { };

        public Toggle(boolean on) {
            this.on = on;
            setPreferredSize(new Dimension(38, 22));
            setMinimumSize(new Dimension(38, 22));
            setMaximumSize(new Dimension(38, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { flip(); }
            });
            getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "flip");
            getActionMap().put("flip", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { flip(); }
            });
        }

        private void flip() {
            on = !on;
            repaint();
            onChange.run();
        }

        public boolean isOn() { return on; }

        public Toggle onChange(Runnable r) { this.onChange = r; return this; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            g2.setColor(on ? Theme.ACCENT : Theme.LINE_STRONG);
            g2.fillRoundRect(0, 0, 38, 22, 22, 22);
            g2.setColor(Color.WHITE);
            g2.fillOval(on ? 19 : 3, 3, 16, 16);
            if (isFocusOwner()) {
                g2.setColor(Theme.ACCENT);
                g2.drawRoundRect(-2, -2, 41, 25, 24, 24);
            }
            g2.dispose();
        }
    }

    /* ==================================================================
       Legend chip (Overview screen)
       ================================================================== */

    public static class Chip extends JButton {
        private final Color swatch;
        private final String title;
        private final String value;

        public Chip(Color swatch, String title, String value) {
            this.swatch = swatch;
            this.title = title;
            this.value = value;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(210, 34));
            setToolTipText(title + " \u2014 " + value);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(Theme.CARD);
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setColor(getModel().isRollover() ? Theme.LINE_STRONG : Theme.LINE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

            g2.setColor(swatch);
            g2.fillRoundRect(11, h / 2 - 5, 10, 10, 3, 3);

            g2.setFont(Theme.BODY_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            int baseline = h / 2 + fm.getAscent() / 2 - 1;
            g2.setColor(Theme.INK);
            int tx = 28;
            String t = Theme.clip(g2, title, w - 46 - g2.getFontMetrics(Theme.MONO).stringWidth(value));
            g2.drawString(t, tx, baseline);

            g2.setFont(Theme.MONO);
            fm = g2.getFontMetrics();
            g2.setColor(Theme.INK_2);
            g2.drawString(value, w - 13 - fm.stringWidth(value), baseline);
            g2.dispose();
        }
    }

    /* ==================================================================
       Form controls
       ================================================================== */

    private static Border controlBorder() {
        return BorderFactory.createCompoundBorder(
                new RoundedLine(Theme.LINE_STRONG, Theme.RADIUS_CTRL),
                BorderFactory.createEmptyBorder(8, 11, 8, 11));
    }

    public static JTextField field(String placeholder) {
        JTextField f = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.aa(g);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                                 Theme.RADIUS_CTRL, Theme.RADIUS_CTRL);
                g2.dispose();
                super.paintComponent(g);
                if (getText().isEmpty() && placeholder != null && !isFocusOwner()) {
                    Graphics2D g3 = Theme.aa(g);
                    g3.setFont(Theme.BODY);
                    g3.setColor(Theme.INK_3);
                    Insets in = getInsets();
                    FontMetrics fm = g3.getFontMetrics();
                    g3.drawString(placeholder, in.left,
                            (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                    g3.dispose();
                }
            }
        };
        f.setOpaque(false);
        f.setBackground(Theme.PAPER);
        f.setForeground(Theme.INK);
        f.setCaretColor(Theme.INK);
        f.setFont(Theme.BODY);
        f.setBorder(controlBorder());
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.repaint(); }
            @Override public void focusLost(FocusEvent e) { f.repaint(); }
        });
        return f;
    }

    public static <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setFont(Theme.BODY);
        c.setBackground(Theme.CARD);
        c.setForeground(Theme.INK);
        c.setBorder(new RoundedLine(Theme.LINE_STRONG, Theme.RADIUS_CTRL));
        c.setFocusable(false);
        ((JComponent) c.getRenderer()).setBorder(
                BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return c;
    }

    public static JCheckBox checkBox(String text) {
        JCheckBox b = new JCheckBox(text);
        b.setIcon(new Skin.CheckIcon());
        b.setIconTextGap(Theme.XS + 2);
        b.setFont(Theme.BODY);
        b.setForeground(Theme.INK);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** A 1px rounded border used by all controls. */
    public static class RoundedLine implements Border {
        private final Color color;
        private final int radius;

        public RoundedLine(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = Theme.aa(g);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public boolean isBorderOpaque() { return false; }
    }

    /** A horizontal hairline, the only divider style in the app. */
    public static JComponent hairline() {
        JPanel p = new JPanel();
        p.setBackground(Theme.LINE);
        p.setPreferredSize(new Dimension(1, 1));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return p;
    }

    /** Wraps any view in a scroll pane with consistent scrolling behaviour. */
    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.PAPER);
        sp.setBackground(Theme.PAPER);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }
}
