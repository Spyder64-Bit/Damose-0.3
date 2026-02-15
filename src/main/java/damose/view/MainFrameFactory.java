package damose.view;

import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

/**
 * Builds the main application frame with shared window settings.
 */
final class MainFrameFactory {

    private MainFrameFactory() {
    }

    static JFrame create(Class<?> resourceOwner, String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setShape(new RoundRectangle2D.Double(0, 0, width, height, 20, 20));

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    frame.setShape(null);
                } else {
                    frame.setShape(new RoundRectangle2D.Double(
                            0,
                            0,
                            frame.getWidth(),
                            frame.getHeight(),
                            20,
                            20
                    ));
                }
            }
        });

        applyWindowIcons(frame, resourceOwner);
        return frame;
    }

    private static void applyWindowIcons(JFrame frame, Class<?> resourceOwner) {
        try {
            Image trimmedIcon = ImageResourceLoader.loadTrimmedImage(resourceOwner, "/sprites/icon.png");
            if (trimmedIcon == null) {
                return;
            }
            List<Image> icons = new ArrayList<>();
            icons.add(trimmedIcon.getScaledInstance(256, 256, Image.SCALE_SMOOTH));
            icons.add(trimmedIcon.getScaledInstance(128, 128, Image.SCALE_SMOOTH));
            icons.add(trimmedIcon.getScaledInstance(64, 64, Image.SCALE_SMOOTH));
            icons.add(trimmedIcon.getScaledInstance(48, 48, Image.SCALE_SMOOTH));
            icons.add(trimmedIcon.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
            icons.add(trimmedIcon.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            frame.setIconImages(icons);
        } catch (Exception e) {
            System.out.println("Could not load app icon: " + e.getMessage());
        }
    }
}
