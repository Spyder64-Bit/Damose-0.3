package damose.view;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JFrame;

import org.jxmapviewer.JXMapViewer;

/**
 * Installs click-drag window movement over the map surface.
 */
final class MapWindowDragSupport {

    private MapWindowDragSupport() {
    }

    static void install(JFrame frame, JXMapViewer mapViewer) {
        final Point[] dragOffset = new Point[1];
        final boolean[] dragging = new boolean[1];

        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getY() < 50 && e.getX() < mapViewer.getWidth() - 120) {
                    dragOffset[0] = e.getPoint();
                    dragging[0] = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging[0] = false;
            }
        });

        mapViewer.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging[0] && dragOffset[0] != null) {
                    Point loc = frame.getLocation();
                    frame.setLocation(loc.x + e.getX() - dragOffset[0].x, loc.y + e.getY() - dragOffset[0].y);
                }
            }
        });
    }
}
