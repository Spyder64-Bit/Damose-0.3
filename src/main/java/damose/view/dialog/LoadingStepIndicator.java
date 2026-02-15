package damose.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import damose.config.AppConstants;

/**
 * Row widget used by LoadingDialog for per-step status.
 */
final class LoadingStepIndicator extends JPanel {

    enum State {
        PENDING,
        ACTIVE,
        DONE,
        WARNING
    }

    private final JLabel iconLabel;
    private final JLabel textLabel;
    private State state = State.PENDING;

    LoadingStepIndicator(String text) {
        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setAlignmentX(CENTER_ALIGNMENT);

        iconLabel = new JLabel("o");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        iconLabel.setForeground(AppConstants.TEXT_MUTED);
        iconLabel.setPreferredSize(new Dimension(28, 28));
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        iconLabel.setVerticalAlignment(JLabel.CENTER);

        textLabel = new JLabel(text);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        textLabel.setForeground(AppConstants.TEXT_SECONDARY);

        add(iconLabel, BorderLayout.WEST);
        add(textLabel, BorderLayout.CENTER);
    }

    void setState(State newState) {
        this.state = newState;
        SwingUtilities.invokeLater(() -> {
            switch (state) {
                case PENDING:
                    iconLabel.setIcon(null);
                    iconLabel.setText("o");
                    iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                    iconLabel.setForeground(AppConstants.TEXT_MUTED);
                    textLabel.setForeground(AppConstants.TEXT_SECONDARY);
                    textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                    break;
                case ACTIVE:
                    applyLoadingIcon();
                    textLabel.setForeground(AppConstants.TEXT_PRIMARY);
                    textLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
                    break;
                case DONE:
                    applyDoneIcon();
                    textLabel.setForeground(AppConstants.SUCCESS_COLOR);
                    textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                    break;
                case WARNING:
                    iconLabel.setIcon(null);
                    iconLabel.setText("!");
                    iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                    iconLabel.setForeground(AppConstants.WARNING_COLOR);
                    textLabel.setForeground(AppConstants.WARNING_COLOR);
                    textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                    break;
                default:
                    break;
            }
        });
    }

    void setText(String text) {
        SwingUtilities.invokeLater(() -> textLabel.setText(text));
    }

    private void applyLoadingIcon() {
        try {
            ImageIcon gifIcon = new ImageIcon(getClass().getResource("/sprites/loading.gif"));
            Image scaled = gifIcon.getImage().getScaledInstance(28, 28, Image.SCALE_DEFAULT);
            iconLabel.setText(null);
            iconLabel.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            iconLabel.setIcon(null);
            iconLabel.setText(">");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            iconLabel.setForeground(AppConstants.ACCENT);
        }
    }

    private void applyDoneIcon() {
        try {
            java.net.URL url = getClass().getResource("/sprites/tick.png");
            if (url == null) {
                throw new IllegalStateException("tick.png not found");
            }
            ImageIcon tickIcon = new ImageIcon(url);
            Image scaled = tickIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaled));
            iconLabel.setText(null);
        } catch (Exception e) {
            iconLabel.setIcon(null);
            iconLabel.setText("v");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            iconLabel.setForeground(AppConstants.SUCCESS_COLOR);
        }
    }
}
