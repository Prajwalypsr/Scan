package diskinsight;

import javax.swing.SwingUtilities;

/**
 * DiskInsight — see what your folders are holding.
 *
 * Start here. Everything the application does happens on the Event Dispatch
 * Thread except the folder scan itself, which runs in FolderScanner.
 */
public class DiskInsightApp {

    public static void main(String[] args) {
        // smoother text on Linux and Windows
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainFrame().setVisible(true);
        });
    }
}
