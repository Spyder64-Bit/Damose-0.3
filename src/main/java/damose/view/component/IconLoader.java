package damose.view.component;

import java.awt.Image;

import javax.swing.ImageIcon;

/**
 * Loads and scales classpath icons.
 */
final class IconLoader {

    private IconLoader() {
    }

    static ImageIcon loadScaled(Class<?> anchor, String path, int size) {
        try {
            ImageIcon raw = new ImageIcon(anchor.getResource(path));
            Image scaled = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }
}
