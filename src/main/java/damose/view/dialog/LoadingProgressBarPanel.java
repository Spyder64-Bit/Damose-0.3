package damose.view.dialog;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import damose.config.AppConstants;

/**
 * Rounded progress bar panel used by LoadingDialog.
 */
final class LoadingProgressBarPanel extends JPanel {

    private int progress;

    LoadingProgressBarPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(392, 14));
        setMaximumSize(new Dimension(392, 14));
    }

    void setProgress(int value) {
        progress = Math.max(0, Math.min(100, value));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int arc = height;

        g2.setColor(AppConstants.PROGRESS_BG);
        g2.fillRoundRect(0, 0, width, height, arc, arc);

        if (progress > 0) {
            int fillWidth = (int) (width * progress / 100.0);
            if (fillWidth > arc) {
                g2.setColor(AppConstants.ACCENT);
                g2.fillRoundRect(0, 0, fillWidth, height, arc, arc);
            }
        }

        g2.dispose();
    }
}
