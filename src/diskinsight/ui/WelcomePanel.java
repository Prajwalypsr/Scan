package diskinsight.ui;

import java.awt.*;
import javax.swing.*;

public class WelcomePanel extends JPanel {

    private final MainFrame app;

    public WelcomePanel(MainFrame app) {
        super(new GridBagLayout());
        this.app = app;
        setOpaque(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.CENTER;

        // Illustration
        gc.insets = new Insets(0, 0, Theme.XL, 0);
        content.add(new FolderIcon(), gc);

        // Title
        gc.gridy++;
        gc.insets = new Insets(0, 0, Theme.S, 0);
        content.add(Ui.label("DiskInsight", Theme.H1, Theme.INK), gc);

        // Subtitle
        gc.gridy++;
        gc.insets = new Insets(0, 0, Theme.XL, 0);
        content.add(Ui.label("Analyze your disk usage and discover large, old and unnecessary files.", Theme.BODY, Theme.INK_2), gc);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.M, 0));
        buttons.setOpaque(false);
        
        Ui.Btn chooseBtn = Ui.primary("Choose Folder");
        chooseBtn.addActionListener(e -> app.chooseFolder());
        
        buttons.add(chooseBtn);

        gc.gridy++;
        gc.insets = new Insets(0, 0, Theme.L, 0);
        content.add(buttons, gc);

        // Footer text
        gc.gridy++;
        gc.insets = new Insets(0, 0, 0, 0);
        content.add(Ui.hint("No scan available yet. Choose a folder to begin analysis."), gc);

        add(content);
    }

    private static class FolderIcon extends JComponent {
        FolderIcon() {
            setPreferredSize(new Dimension(80, 80));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth();
            int h = getHeight();

            // Back folder tab
            g2.setColor(Theme.ACCENT);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g2.fillRoundRect(10, 10, w / 2, 25, 12, 12);
            
            // Front folder body
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(10, 25, w - 20, h - 35, 12, 12);
            
            g2.dispose();
        }
    }
}
