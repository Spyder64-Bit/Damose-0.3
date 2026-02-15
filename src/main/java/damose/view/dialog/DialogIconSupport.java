package damose.view.dialog;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import damose.view.ImageResourceLoader;

/**
 * Shared icon helpers for dialog windows.
 */
final class DialogIconSupport {

    private DialogIconSupport() {
    }

    static void applyAppIcons(JFrame frame, Class<?> resourceOwner) {
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

    static JLabel createTitleIconLabel(Class<?> resourceOwner, int size) {
        try {
            Image trimmedIcon = ImageResourceLoader.loadTrimmedImage(resourceOwner, "/sprites/icon.png");
            if (trimmedIcon == null) {
                return null;
            }
            Image scaled = trimmedIcon.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaled));
            iconLabel.setPreferredSize(new java.awt.Dimension(size, size));
            iconLabel.setMaximumSize(new java.awt.Dimension(size, size));
            iconLabel.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
            return iconLabel;
        } catch (Exception e) {
            return null;
        }
    }
}
