package damose.view.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;
import damose.database.User;
import damose.database.UserService;

/**
 * Core class for login dialog.
 */
public class LoginDialog extends JFrame {

    private static final String SWITCH_TO_REGISTER_TEXT = "Non hai ancora un account? Registrati";
    private static final String SWITCH_TO_LOGIN_TEXT = "Hai gi\u00E0 un account? Accedi";
    private static final int TITLE_ICON_SIZE = 56;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField emailField;
    private JLabel confirmLabel, emailLabel, messageLabel, subtitleLabel;
    private JButton actionButton, switchModeButton;
    private JPanel mainPanel;

    private boolean isLoginMode = true;
    private User loggedInUser = null;
    private boolean wasCancelled = true;

    private Consumer<User> onComplete;
    private Point dragOffset;

    public LoginDialog(JFrame parent) {
        super("Damose - Accesso");
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        try {
            Image trimmedIcon = loadTrimmedImage("/sprites/icon.png");
            if (trimmedIcon != null) {
                List<Image> icons = new ArrayList<>();
                icons.add(trimmedIcon.getScaledInstance(256, 256, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(128, 128, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(64, 64, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(48, 48, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
                setIconImages(icons);
            }
        } catch (Exception e) {
            System.out.println("Could not load app icon: " + e.getMessage());
        }

        addWindowListener(new WindowAdapter() {
            @Override
            /**
             * Handles windowClosing.
             */
            public void windowClosing(WindowEvent e) {
                wasCancelled = true;
                dispose();
                if (onComplete != null) {
                    onComplete.accept(null);
                }
            }
        });

        initComponents();
        setSize(450, 540);
        setLocationRelativeTo(parent);
        setResizable(false);
        setShape(new RoundRectangle2D.Double(0, 0, 450, 540, 16, 16));
    }

    /**
     * Registers callback for complete.
     */
    public void setOnComplete(Consumer<User> callback) {
        this.onComplete = callback;
    }

    private void closeWithResult(User user, boolean cancelled) {
        this.loggedInUser = user;
        this.wasCancelled = cancelled;
        dispose();
        if (onComplete != null) {
            onComplete.accept(user);
        }
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppConstants.BG_DARK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(AppConstants.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel titleBar = createTitleBar();

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(10, 40, 20, 40));

        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        try {
            Image trimmedIcon = loadTrimmedImage("/sprites/icon.png");
            Image scaled = trimmedIcon != null
                    ? trimmedIcon.getScaledInstance(TITLE_ICON_SIZE, TITLE_ICON_SIZE, Image.SCALE_SMOOTH)
                    : null;
            JLabel iconLabel = new JLabel(new ImageIcon(scaled));
            iconLabel.setPreferredSize(new Dimension(TITLE_ICON_SIZE, TITLE_ICON_SIZE));
            iconLabel.setMaximumSize(new Dimension(TITLE_ICON_SIZE, TITLE_ICON_SIZE));
            iconLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            titleRow.add(iconLabel);
            titleRow.add(Box.createHorizontalStrut(12));
        } catch (Exception e) {
        }

        JLabel titleLabel = new JLabel("Damose");
        titleLabel.setFont(AppConstants.FONT_TITLE);
        titleLabel.setForeground(AppConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        titleRow.add(titleLabel);

        contentPanel.add(titleRow);
        contentPanel.add(Box.createVerticalStrut(8));

        subtitleLabel = new JLabel("Accedi al tuo account");
        subtitleLabel.setFont(AppConstants.FONT_SUBTITLE);
        subtitleLabel.setForeground(AppConstants.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(18));

        contentPanel.add(createLabel("Username"));
        contentPanel.add(Box.createVerticalStrut(6));
        usernameField = createTextField();
        contentPanel.add(usernameField);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(createLabel("Password"));
        contentPanel.add(Box.createVerticalStrut(6));
        passwordField = createPasswordField();
        contentPanel.add(passwordField);
        contentPanel.add(Box.createVerticalStrut(16));

        confirmLabel = createLabel("Conferma Password");
        confirmPasswordField = createPasswordField();
        confirmLabel.setVisible(false);
        confirmPasswordField.setVisible(false);
        contentPanel.add(confirmLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(confirmPasswordField);
        contentPanel.add(Box.createVerticalStrut(16));

        emailLabel = createLabel("Email (opzionale)");
        emailField = createTextField();
        emailLabel.setVisible(false);
        emailField.setVisible(false);
        contentPanel.add(emailLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(emailField);
        contentPanel.add(Box.createVerticalStrut(20));

        messageLabel = new JLabel(" ");
        messageLabel.setFont(AppConstants.FONT_SMALL);
        messageLabel.setForeground(AppConstants.TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(messageLabel);
        contentPanel.add(Box.createVerticalStrut(16));

        actionButton = createPrimaryButton("Accedi");
        actionButton.addActionListener(e -> performAction());
        contentPanel.add(actionButton);
        contentPanel.add(Box.createVerticalStrut(12));

        switchModeButton = createLinkButton(SWITCH_TO_REGISTER_TEXT);
        switchModeButton.addActionListener(e -> toggleMode());
        contentPanel.add(switchModeButton);
        contentPanel.add(Box.createVerticalStrut(8));

        JButton skipButton = createLinkButton("Continua senza account");
        skipButton.addActionListener(e -> closeWithResult(null, false));
        contentPanel.add(skipButton);

        mainPanel.add(titleBar, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

        KeyAdapter enterKey = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performAction();
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) closeWithResult(null, true);
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);
        confirmPasswordField.addKeyListener(enterKey);
        emailField.addKeyListener(enterKey);
    }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        titleBar.setPreferredSize(new Dimension(400, 40));
        titleBar.setBorder(new EmptyBorder(8, 12, 0, 12));

        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragOffset.x, loc.y + e.getY() - dragOffset.y);
            }
        });

        JLabel closeBtn = new JLabel("<html><b style='font-size:14px'>X</b></html>");
        closeBtn.setForeground(AppConstants.TEXT_SECONDARY);
        closeBtn.setPreferredSize(new Dimension(36, 36));
        closeBtn.setHorizontalAlignment(JLabel.CENTER);
        closeBtn.setVerticalAlignment(JLabel.CENTER);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { closeWithResult(null, true); }
            /**
             * Handles mouseEntered.
             */
            public void mouseEntered(MouseEvent e) {
                closeBtn.setText("<html><b style='font-size:14px;color:#ff6b6b'>X</b></html>");
            }
            /**
             * Handles mouseExited.
             */
            public void mouseExited(MouseEvent e) {
                closeBtn.setText("<html><b style='font-size:14px'>X</b></html>");
            }
        });

        titleBar.add(closeBtn, BorderLayout.EAST);
        return titleBar;
    }

    private Image loadTrimmedImage(String path) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            return null;
        }
        ImageIcon rawIcon = new ImageIcon(url);
        if (rawIcon.getIconWidth() <= 0 || rawIcon.getIconHeight() <= 0) {
            return null;
        }

        BufferedImage source = new BufferedImage(rawIcon.getIconWidth(), rawIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = source.createGraphics();
        g2.drawImage(rawIcon.getImage(), 0, 0, null);
        g2.dispose();

        return trimTransparentBorders(source);
    }

    private BufferedImage trimTransparentBorders(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (source.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 8) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source;
        }
        return source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppConstants.TEXT_SECONDARY);
        label.setFont(AppConstants.FONT_SMALL);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private JTextField createTextField() {
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
        field.setOpaque(false);
        field.setBackground(AppConstants.BG_LIGHT);
        field.setForeground(AppConstants.TEXT_PRIMARY);
        field.setCaretColor(AppConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1, true),
            new EmptyBorder(10, 14, 10, 14)));
        field.setFont(AppConstants.FONT_BODY);
        field.setMaximumSize(new Dimension(320, 42));
        field.setPreferredSize(new Dimension(320, 42));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        return field;
    }

    private JPasswordField createPasswordField() {
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
        field.setOpaque(false);
        field.setBackground(AppConstants.BG_LIGHT);
        field.setForeground(AppConstants.TEXT_PRIMARY);
        field.setCaretColor(AppConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1, true),
            new EmptyBorder(10, 14, 10, 14)));
        field.setFont(AppConstants.FONT_BODY);
        field.setMaximumSize(new Dimension(320, 42));
        field.setPreferredSize(new Dimension(320, 42));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        return field;
    }

    private JButton createPrimaryButton(String text) {
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

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(AppConstants.ACCENT_HOVER); btn.repaint(); }
            public void mouseExited(MouseEvent e) { btn.setBackground(AppConstants.ACCENT); btn.repaint(); }
        });

        return btn;
    }

    private JButton createLinkButton(String text) {
        JButton btn = new JButton(text);
        btn.setForeground(AppConstants.TEXT_SECONDARY);
        btn.setFont(AppConstants.FONT_SMALL);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(AppConstants.ACCENT); }
            public void mouseExited(MouseEvent e) { btn.setForeground(AppConstants.TEXT_SECONDARY); }
        });

        return btn;
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        confirmLabel.setVisible(!isLoginMode);
        confirmPasswordField.setVisible(!isLoginMode);
        emailLabel.setVisible(!isLoginMode);
        emailField.setVisible(!isLoginMode);
        actionButton.setText(isLoginMode ? "Accedi" : "Registrati");
        switchModeButton.setText(isLoginMode ? SWITCH_TO_REGISTER_TEXT : SWITCH_TO_LOGIN_TEXT);
        subtitleLabel.setText(isLoginMode ? "Accedi al tuo account" : "Crea un nuovo account");
        messageLabel.setText(" ");

        int newHeight = isLoginMode ? 540 : 680;
        int newWidth = 450;
        setSize(newWidth, newHeight);
        setShape(new RoundRectangle2D.Double(0, 0, newWidth, newHeight, 16, 16));
    }

    private void performAction() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Compila tutti i campi");
            return;
        }
        if (username.length() < 3) { showError("Username min 3 caratteri"); return; }
        if (password.length() < 4) { showError("Password min 4 caratteri"); return; }

        if (isLoginMode) {
            User user = UserService.login(username, password);
            if (user != null) {
                closeWithResult(user, false);
            } else {
                showError("Credenziali non valide");
            }
        } else {
            String confirm = new String(confirmPasswordField.getPassword());
            if (!password.equals(confirm)) { showError("Le password non coincidono"); return; }
            String email = emailField.getText().trim();
            if (UserService.register(username, password, email.isEmpty() ? null : email)) {
                showSuccess("Registrazione completata!");
                toggleMode();
                usernameField.setText(username);
            } else {
                showError("Username gi\u00E0 in uso");
            }
        }
    }

    private void showError(String msg) {
        messageLabel.setForeground(AppConstants.ERROR_COLOR);
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setForeground(AppConstants.SUCCESS_COLOR);
        messageLabel.setText(msg);
    }

    /**
     * Returns the result of wasCancelled.
     */
    public boolean wasCancelled() {
        return wasCancelled;
    }

    /**
     * Returns the logged in user.
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }
}

