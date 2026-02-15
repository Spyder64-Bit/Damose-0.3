package damose.view;

import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Manages custom window controls (close/maximize/minimize).
 */
final class WindowControlsCoordinator {
    private static final int WINDOW_CONTROL_SIZE = 34;
    private static final int WINDOW_CONTROL_TOP = 6;
    private static final int WINDOW_CONTROL_RIGHT_MARGIN = 6;
    private static final int WINDOW_CONTROL_GAP = 2;

    private final JFrame frame;
    private final JPanel overlayPanel;
    private JButton closeButton;
    private JButton maxButton;
    private JButton minButton;
    private Rectangle normalBounds = new Rectangle(100, 100, 1100, 750);

    WindowControlsCoordinator(JFrame frame, JPanel overlayPanel) {
        this.frame = frame;
        this.overlayPanel = overlayPanel;
    }

    void create(int width) {
        closeButton = WindowControlButtonFactory.create(WindowControlButtonFactory.Type.CLOSE);
        closeButton.addActionListener(e -> System.exit(0));
        overlayPanel.add(closeButton);

        maxButton = WindowControlButtonFactory.create(WindowControlButtonFactory.Type.MAXIMIZE);
        maxButton.addActionListener(e -> {
            if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                frame.setExtendedState(JFrame.NORMAL);
                frame.setBounds(normalBounds);
            } else {
                normalBounds = frame.getBounds();
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
        overlayPanel.add(maxButton);

        minButton = WindowControlButtonFactory.create(WindowControlButtonFactory.Type.MINIMIZE);
        minButton.addActionListener(e -> frame.setState(JFrame.ICONIFIED));
        overlayPanel.add(minButton);

        updatePositions(width);
    }

    void updatePositions(int width) {
        int closeX = width - WINDOW_CONTROL_RIGHT_MARGIN - WINDOW_CONTROL_SIZE;
        int maxX = closeX - WINDOW_CONTROL_SIZE - WINDOW_CONTROL_GAP;
        int minX = maxX - WINDOW_CONTROL_SIZE - WINDOW_CONTROL_GAP;

        if (closeButton != null) {
            closeButton.setBounds(closeX, WINDOW_CONTROL_TOP, WINDOW_CONTROL_SIZE, WINDOW_CONTROL_SIZE);
        }
        if (maxButton != null) {
            maxButton.setBounds(maxX, WINDOW_CONTROL_TOP, WINDOW_CONTROL_SIZE, WINDOW_CONTROL_SIZE);
        }
        if (minButton != null) {
            minButton.setBounds(minX, WINDOW_CONTROL_TOP, WINDOW_CONTROL_SIZE, WINDOW_CONTROL_SIZE);
        }
    }
}
