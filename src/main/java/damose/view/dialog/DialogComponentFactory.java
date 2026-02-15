package damose.view.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;

/**
 * Shared widget factory for authentication/loading dialogs.
 */
final class DialogComponentFactory {

    private DialogComponentFactory() {
    }

    static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppConstants.TEXT_SECONDARY);
        label.setFont(AppConstants.FONT_SMALL);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    static JTextField createRoundedTextField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        configureInputField(field);
        return field;
    }

    static JPasswordField createRoundedPasswordField() {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        configureInputField(field);
        return field;
    }

    static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setBackground(AppConstants.ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(AppConstants.FONT_BUTTON);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(320, 44));
        btn.setPreferredSize(new Dimension(320, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(AppConstants.ACCENT_HOVER);
                btn.repaint();
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(AppConstants.ACCENT);
                btn.repaint();
            }
        });
        return btn;
    }

    static JButton createLinkButton(String text) {
        JButton btn = new JButton(text);
        btn.setForeground(AppConstants.TEXT_SECONDARY);
        btn.setFont(AppConstants.FONT_SMALL);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(AppConstants.ACCENT);
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(AppConstants.TEXT_SECONDARY);
            }
        });
        return btn;
    }

    private static void configureInputField(javax.swing.text.JTextComponent field) {
        field.setOpaque(false);
        field.setBackground(AppConstants.BG_LIGHT);
        field.setForeground(AppConstants.TEXT_PRIMARY);
        field.setCaretColor(AppConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        field.setFont(AppConstants.FONT_BODY);
        field.setMaximumSize(new Dimension(320, 42));
        field.setPreferredSize(new Dimension(320, 42));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
    }
}
