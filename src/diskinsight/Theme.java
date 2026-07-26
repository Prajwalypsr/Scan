package diskinsight;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;

/**
 * Theme — the design system for DiskInsight.
 *
 * Every colour, font size, gap and corner radius used anywhere in the
 * application is defined here and nowhere else. If a screen needs to look
 * different, the value changes here once and every screen follows.
 *
 *   Spacing scale ....... XS 6, S 10, M 16, L 24, XL 36   (nothing else is used)
 *   Corner radius ....... RADIUS 10 for cards, 8 for controls, pill for chips
 *   Card padding ........ CARD_PAD 18 on all four sides, every card
 *   Table row height .... ROW_H 46, every table
 *   Type scale .......... HERO 42 / H1 26 / H2 20 / BODY 14 / SMALL 12 / LABEL 11
 */
public final class Theme {

    private Theme() { }

    /* ------------------------------------------------------------------
       Colour tokens
       ------------------------------------------------------------------ */
    public static final Color PAPER       = new Color(0xF2F5F9); // app background
    public static final Color CARD        = new Color(0xFFFFFF); // panel surface
    public static final Color INK         = new Color(0x141E2B); // primary text
    public static final Color INK_2       = new Color(0x5C6A7B); // secondary text
    public static final Color INK_3       = new Color(0x8B98A8); // hint text
    public static final Color LINE        = new Color(0xE1E7EF); // hairline border
    public static final Color LINE_STRONG = new Color(0xCBD5E1); // control border
    public static final Color ACCENT      = new Color(0x3A4FC4); // primary action
    public static final Color ACCENT_DARK = new Color(0x31429F); // pressed / hover
    public static final Color ACCENT_SOFT = new Color(0xEAEDFB); // selected row
    public static final Color FLAG        = new Color(0xB26A05); // cleanup marker
    public static final Color FLAG_SOFT   = new Color(0xFDF3E2);
    public static final Color FLAG_LINE   = new Color(0xF0DFC0);
    public static final Color ROW_HOVER   = new Color(0xFAFBFD);
    public static final Color DANGER      = new Color(0xC0392B);

    /* ------------------------------------------------------------------
       Spacing, radius, sizing
       ------------------------------------------------------------------ */
    public static final int XS = 6, S = 10, M = 16, L = 24, XL = 36;
    public static final int RADIUS = 10;
    public static final int RADIUS_CTRL = 8;
    public static final int CARD_PAD = 18;
    public static final int ROW_H = 46;
    public static final int CONTENT_WIDTH = 1120;

    /* ------------------------------------------------------------------
       Type
       ------------------------------------------------------------------ */
    private static String pick(String... candidates) {
        try {
            Set<String> have = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                                       .getAvailableFontFamilyNames()));
            for (String c : candidates) {
                if (have.contains(c)) return c;
            }
        } catch (Throwable ignored) { }
        return Font.SANS_SERIF;
    }

    private static final String DISPLAY_FAMILY = pick(
            "Bricolage Grotesque", "Inter", "Segoe UI", "SF Pro Display",
            "Helvetica Neue", "Roboto", "DejaVu Sans", "Arial");

    private static final String BODY_FAMILY = pick(
            "Public Sans", "Inter", "Segoe UI", "SF Pro Text",
            "Helvetica Neue", "Roboto", "DejaVu Sans", "Arial");

    private static final String MONO_FAMILY = pick(
            "JetBrains Mono", "IBM Plex Mono", "Consolas", "Menlo",
            "DejaVu Sans Mono", "Monospaced");

    public static final Font HERO      = new Font(DISPLAY_FAMILY, Font.BOLD,  42);
    public static final Font H1        = new Font(DISPLAY_FAMILY, Font.BOLD,  26);
    public static final Font H2        = new Font(DISPLAY_FAMILY, Font.BOLD,  20);
    public static final Font H3        = new Font(DISPLAY_FAMILY, Font.BOLD,  16);
    public static final Font BODY      = new Font(BODY_FAMILY,    Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font(BODY_FAMILY,    Font.BOLD,  14);
    public static final Font SMALL     = new Font(BODY_FAMILY,    Font.PLAIN, 12);
    public static final Font LABEL     = new Font(BODY_FAMILY,    Font.BOLD,  11);
    public static final Font MONO      = new Font(MONO_FAMILY,    Font.PLAIN, 12);
    public static final Font MONO_BOLD = new Font(MONO_FAMILY,    Font.BOLD,  12);

    /* ------------------------------------------------------------------
       Painting helpers — used by every custom component
       ------------------------------------------------------------------ */
    public static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                            RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /** Fills a rounded rectangle and strokes a 1px border of the given colour. */
    public static void surface(Graphics2D g2, int w, int h, int radius,
                               Color fill, Color border) {
        if (fill != null) {
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
        }
        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
        }
    }

    /** Draws text clipped to maxWidth, adding an ellipsis when it does not fit. */
    public static String clip(Graphics2D g2, String text, int maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return text;
        String dots = "\u2026";
        int dotW = fm.stringWidth(dots);
        StringBuilder sb = new StringBuilder();
        int w = 0;
        for (char c : text.toCharArray()) {
            int cw = fm.charWidth(c);
            if (w + cw + dotW > maxWidth) break;
            sb.append(c);
            w += cw;
        }
        return sb + dots;
    }

    /** Applies global Swing defaults so stock components match the theme. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }

        UIManager.put("Panel.background", PAPER);
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", BODY_BOLD);
        UIManager.put("ToolTip.background", INK);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font", SMALL);
        UIManager.put("ToolTip.border",
                BorderFactory.createEmptyBorder(6, 9, 6, 9));
        UIManager.put("ScrollBar.width", 12);
        Skin.install();
        UIManager.put("FileChooser.listFont", BODY);
        ToolTipManager.sharedInstance().setInitialDelay(250);
    }
}
