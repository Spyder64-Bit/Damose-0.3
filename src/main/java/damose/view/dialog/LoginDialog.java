package damose.view.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
    private static final int LOGIN_HEIGHT = 540;
    private static final int REGISTER_HEIGHT = 680;
    private static final int DIALOG_WIDTH = 450;
    private static final int TITLE_ICON_SIZE = 56;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField emailField;
    private JLabel confirmLabel;
    private JLabel emailLabel;
    private JLabel messageLabel;
    private JLabel subtitleLabel;
    private JButton actionButton;
    private JButton switchModeButton;

    private boolean isLoginMode = true;
    private User loggedInUser;
    private boolean wasCancelled = true;

    private Consumer<User> onComplete;
    private Point dragOffset;

    public LoginDialog(JFrame parent) {
        super("Damose - Accesso");
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        DialogIconSupport.applyAppIcons(this, getClass());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                wasCancelled = true;
                dispose();
                if (onComplete != null) {
                    onComplete.accept(null);
                }
            }
        });

        initComponents();
        setSize(DIALOG_WIDTH, LOGIN_HEIGHT);
        setLocationRelativeTo(parent);
        setResizable(false);
        setShape(new RoundRectangle2D.Double(0, 0, DIALOG_WIDTH, LOGIN_HEIGHT, 16, 16));
    }

    /**
     * Registers callback for complete.
     */
    public void setOnComplete(Consumer<User> callback) {
        this.onComplete = callback;
    }

    private void closeWithResult(User user, boolean cancelled) {
        loggedInUser = user;
        wasCancelled = cancelled;
        dispose();
        if (onComplete != null) {
            onComplete.accept(user);
        }
    }

    private void initComponents() {
        JPanel mainPanel = createMainPanel();
        JPanel titleBar = createTitleBar();
        JPanel contentPanel = buildContentPanel();

        mainPanel.add(titleBar, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

        bindEnterAndEscape();
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout()) {
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
        return mainPanel;
    }

    private JPanel buildContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(10, 40, 20, 40));

        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLabel = DialogIconSupport.createTitleIconLabel(getClass(), TITLE_ICON_SIZE);
        if (iconLabel != null) {
            titleRow.add(iconLabel);
            titleRow.add(Box.createHorizontalStrut(12));
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

        contentPanel.add(DialogComponentFactory.createFieldLabel("Username"));
        contentPanel.add(Box.createVerticalStrut(6));
        usernameField = DialogComponentFactory.createRoundedTextField();
        contentPanel.add(usernameField);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(DialogComponentFactory.createFieldLabel("Password"));
        contentPanel.add(Box.createVerticalStrut(6));
        passwordField = DialogComponentFactory.createRoundedPasswordField();
        contentPanel.add(passwordField);
        contentPanel.add(Box.createVerticalStrut(16));

        confirmLabel = DialogComponentFactory.createFieldLabel("Conferma Password");
        confirmPasswordField = DialogComponentFactory.createRoundedPasswordField();
        confirmLabel.setVisible(false);
        confirmPasswordField.setVisible(false);
        contentPanel.add(confirmLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(confirmPasswordField);
        contentPanel.add(Box.createVerticalStrut(16));

        emailLabel = DialogComponentFactory.createFieldLabel("Email (opzionale)");
        emailField = DialogComponentFactory.createRoundedTextField();
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

        actionButton = DialogComponentFactory.createPrimaryButton("Accedi");
        actionButton.addActionListener(e -> performAction());
        contentPanel.add(actionButton);
        contentPanel.add(Box.createVerticalStrut(12));

        switchModeButton = DialogComponentFactory.createLinkButton(SWITCH_TO_REGISTER_TEXT);
        switchModeButton.addActionListener(e -> toggleMode());
        contentPanel.add(switchModeButton);
        contentPanel.add(Box.createVerticalStrut(8));

        JButton skipButton = DialogComponentFactory.createLinkButton("Continua senza account");
        skipButton.addActionListener(e -> closeWithResult(null, false));
        contentPanel.add(skipButton);

        return contentPanel;
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
            public void mouseClicked(MouseEvent e) {
                closeWithResult(null, true);
            }

            public void mouseEntered(MouseEvent e) {
                closeBtn.setText("<html><b style='font-size:14px;color:#ff6b6b'>X</b></html>");
            }

            public void mouseExited(MouseEvent e) {
                closeBtn.setText("<html><b style='font-size:14px'>X</b></html>");
            }
        });

        titleBar.add(closeBtn, BorderLayout.EAST);
        return titleBar;
    }

    private void bindEnterAndEscape() {
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performAction();
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeWithResult(null, true);
                }
            }
        };

        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);
        confirmPasswordField.addKeyListener(enterKey);
        emailField.addKeyListener(enterKey);
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        updateModeUi();
    }

    private void updateModeUi() {
        confirmLabel.setVisible(!isLoginMode);
        confirmPasswordField.setVisible(!isLoginMode);
        emailLabel.setVisible(!isLoginMode);
        emailField.setVisible(!isLoginMode);
        actionButton.setText(isLoginMode ? "Accedi" : "Registrati");
        switchModeButton.setText(isLoginMode ? SWITCH_TO_REGISTER_TEXT : SWITCH_TO_LOGIN_TEXT);
        subtitleLabel.setText(isLoginMode ? "Accedi al tuo account" : "Crea un nuovo account");
        messageLabel.setText(" ");

        int newHeight = isLoginMode ? LOGIN_HEIGHT : REGISTER_HEIGHT;
        setSize(DIALOG_WIDTH, newHeight);
        setShape(new RoundRectangle2D.Double(0, 0, DIALOG_WIDTH, newHeight, 16, 16));
    }

    private void performAction() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        String validationError = validateCredentials(username, password);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        if (isLoginMode) {
            handleLogin(username, password);
        } else {
            handleRegistration(username, password);
        }
    }

    private String validateCredentials(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            return "Compila tutti i campi";
        }
        if (username.length() < 3) {
            return "Username min 3 caratteri";
        }
        if (password.length() < 4) {
            return "Password min 4 caratteri";
        }
        return null;
    }

    private void handleLogin(String username, String password) {
        User user = UserService.login(username, password);
        if (user != null) {
            closeWithResult(user, false);
        } else {
            showError("Credenziali non valide");
        }
    }

    private void handleRegistration(String username, String password) {
        String confirm = new String(confirmPasswordField.getPassword());
        if (!password.equals(confirm)) {
            showError("Le password non coincidono");
            return;
        }

        String email = emailField.getText().trim();
        boolean registered = UserService.register(username, password, email.isEmpty() ? null : email);
        if (registered) {
            showSuccess("Registrazione completata!");
            toggleMode();
            usernameField.setText(username);
            passwordField.setText("");
            confirmPasswordField.setText("");
        } else {
            showError("Username gi\u00E0 in uso");
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
