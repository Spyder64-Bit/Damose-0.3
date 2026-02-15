package damose.view.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import damose.config.AppConstants;
import damose.database.SessionManager;
import damose.model.Stop;
import damose.service.FavoritesService;

/**
 * Search overlay component for stops, lines, and favorites.
 */
public class SearchOverlay extends JPanel {
    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_HEIGHT = 420;

    private final JTextField searchField;
    private final DefaultListModel<Stop> listModel;
    private final JList<Stop> resultList;
    private final JPanel contentPanel;
    private final SearchModeTabs modeTabs;
    private final JButton closeOverlayButton;

    private SearchOverlayMode currentMode = SearchOverlayMode.STOPS;
    private List<Stop> allStops = List.of();
    private List<Stop> allLines = List.of();
    private List<Stop> favoriteStops = List.of();
    private Consumer<Stop> onSelect;
    private Runnable onFavoritesLoginRequired;

    public SearchOverlay() {
        setLayout(null);
        setOpaque(false);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(AppConstants.BG_MEDIUM);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1),
                new EmptyBorder(16, 16, 16, 16)
        ));

        modeTabs = new SearchModeTabs();
        modeTabs.setOnModeChanged(mode -> {
            currentMode = mode;
            filterResults();
        });

        closeOverlayButton = new JButton();
        closeOverlayButton.setIcon(new CloseGlyphIcon(12, Color.WHITE));
        closeOverlayButton.setFocusPainted(false);
        closeOverlayButton.setContentAreaFilled(false);
        closeOverlayButton.setOpaque(false);
        closeOverlayButton.setBorderPainted(false);
        closeOverlayButton.setBorder(BorderFactory.createEmptyBorder());
        closeOverlayButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeOverlayButton.setPreferredSize(new Dimension(30, 30));
        closeOverlayButton.setMinimumSize(new Dimension(30, 30));
        closeOverlayButton.setMaximumSize(new Dimension(30, 30));
        closeOverlayButton.setToolTipText("Chiudi ricerca");
        closeOverlayButton.addActionListener(e -> closeOverlay());
        closeOverlayButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeOverlayButton.setIcon(new CloseGlyphIcon(12, AppConstants.ACCENT_HOVER));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeOverlayButton.setIcon(new CloseGlyphIcon(12, Color.WHITE));
            }
        });

        searchField = new JTextField();
        searchField.setBackground(AppConstants.BG_FIELD);
        searchField.setForeground(AppConstants.TEXT_PRIMARY);
        searchField.setCaretColor(AppConstants.TEXT_PRIMARY);
        searchField.setFont(AppConstants.FONT_BODY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppConstants.BORDER_COLOR),
                new EmptyBorder(12, 14, 12, 14)
        ));
        searchField.setPreferredSize(new Dimension(468, 48));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterResults();
            }

            public void removeUpdate(DocumentEvent e) {
                filterResults();
            }

            public void changedUpdate(DocumentEvent e) {
                filterResults();
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (shouldSuppressDeleteBeep(code)) {
                    e.consume();
                    return;
                }

                if (code == KeyEvent.VK_ESCAPE) {
                    closeOverlay();
                } else if (code == KeyEvent.VK_ENTER) {
                    e.consume();
                    selectCurrentAndClose();
                } else if (code == KeyEvent.VK_DOWN) {
                    e.consume();
                    moveSelection(1);
                } else if (code == KeyEvent.VK_UP) {
                    e.consume();
                    moveSelection(-1);
                } else if (code == KeyEvent.VK_TAB) {
                    e.consume();
                    modeTabs.cycleMode();
                }
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel modeHeader = new JPanel(new BorderLayout());
        modeHeader.setOpaque(false);
        modeHeader.add(modeTabs.panel(), BorderLayout.CENTER);
        modeHeader.add(closeOverlayButton, BorderLayout.EAST);

        topPanel.add(modeHeader, BorderLayout.NORTH);
        topPanel.add(searchField, BorderLayout.CENTER);
        contentPanel.add(topPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setBackground(AppConstants.LIST_BG);
        resultList.setForeground(AppConstants.TEXT_PRIMARY);
        resultList.setSelectionBackground(AppConstants.ACCENT);
        resultList.setSelectionForeground(Color.WHITE);
        resultList.setFont(AppConstants.FONT_BODY);
        resultList.setFixedCellHeight(58);
        resultList.setCellRenderer(new SearchStopCellRenderer());

        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    selectCurrentAndClose();
                } else if (e.getKeyCode() == KeyEvent.VK_F) {
                    e.consume();
                    toggleSelectedFavorite();
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE
                        || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                }
            }
        });

        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectCurrentAndClose();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppConstants.BORDER_COLOR));
        scrollPane.setPreferredSize(new Dimension(468, 280));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setOpaque(false);
        listPanel.setBorder(new EmptyBorder(12, 0, 0, 0));
        listPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(listPanel, BorderLayout.CENTER);

        JLabel hint = new JLabel("Tab = categoria | F = preferito | Enter = seleziona | Esc = chiudi");
        hint.setFont(AppConstants.FONT_HINT);
        hint.setForeground(AppConstants.TEXT_SECONDARY);
        hint.setBorder(new EmptyBorder(10, 0, 0, 0));
        contentPanel.add(hint, BorderLayout.SOUTH);

        add(contentPanel);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!contentPanel.getBounds().contains(e.getPoint())) {
                    closeOverlay();
                }
            }
        });
    }

    private void filterResults() {
        String query = searchField.getText().toLowerCase().trim();

        List<Stop> source = switch (currentMode) {
            case STOPS -> allStops;
            case LINES -> allLines;
            case FAVORITES -> favoriteStops;
        };

        SearchOverlayResultPopulator.populate(listModel, source, currentMode, query);
        if (!listModel.isEmpty()) {
            resultList.setSelectedIndex(0);
        }
    }

    private void moveSelection(int delta) {
        int idx = resultList.getSelectedIndex();
        int newIdx = idx + delta;
        if (newIdx >= 0 && newIdx < listModel.size()) {
            resultList.setSelectedIndex(newIdx);
            resultList.ensureIndexIsVisible(newIdx);
        }
    }

    private void selectCurrentAndClose() {
        Stop selected = resultList.getSelectedValue();
        if (selected == null) {
            return;
        }

        Consumer<Stop> callback = onSelect;
        closeOverlay();
        if (callback != null) {
            SwingUtilities.invokeLater(() -> callback.accept(selected));
        }
    }

    private void toggleSelectedFavorite() {
        if (!isLoggedIn()) {
            showFavoritesLoginRequiredPopup();
            return;
        }

        Stop selected = resultList.getSelectedValue();
        if (selected == null) {
            return;
        }

        if (selected.isFakeLine()) {
            FavoritesService.toggleLineFavorite(selected.getStopId());
        } else {
            FavoritesService.toggleFavorite(selected.getStopId());
        }
        resultList.repaint();

        if (currentMode == SearchOverlayMode.FAVORITES) {
            favoriteStops = FavoritesService.getAllFavorites();
            filterResults();
        }
    }

    private boolean isLoggedIn() {
        return SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null;
    }

    private void showFavoritesLoginRequiredPopup() {
        if (onFavoritesLoginRequired != null) {
            onFavoritesLoginRequired.run();
            return;
        }
        JOptionPane.showMessageDialog(
                this,
                "Per salvare i preferiti devi creare un account.\n"
                        + "Chiudi l'applicazione, riaprila e crea un account se vuoi usare i preferiti.",
                "Account richiesto",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void closeOverlay() {
        setVisible(false);
    }

    private boolean shouldSuppressDeleteBeep(int keyCode) {
        if (keyCode != KeyEvent.VK_BACK_SPACE && keyCode != KeyEvent.VK_DELETE) {
            return false;
        }

        int selStart = searchField.getSelectionStart();
        int selEnd = searchField.getSelectionEnd();
        if (selEnd > selStart) {
            return false;
        }

        int caret = searchField.getCaretPosition();
        int len = searchField.getDocument().getLength();
        if (keyCode == KeyEvent.VK_BACK_SPACE) {
            return caret <= 0;
        }
        return caret >= len;
    }

    /**
     * Updates the data value.
     */
    public void setData(List<Stop> stops, List<Stop> lines) {
        this.allStops = stops != null ? stops : List.of();
        this.allLines = lines != null ? lines : List.of();
    }

    /**
     * Registers callback for select.
     */
    public void setOnSelect(Consumer<Stop> callback) {
        this.onSelect = callback;
    }

    public void setOnFavoritesLoginRequired(Runnable callback) {
        this.onFavoritesLoginRequired = callback;
    }

    /**
     * Handles showSearch.
     */
    public void showSearch() {
        openOverlay(SearchOverlayMode.STOPS, null);
    }

    /**
     * Handles showFavorites.
     */
    public void showFavorites(List<Stop> favorites) {
        openOverlay(SearchOverlayMode.FAVORITES, favorites);
    }

    /**
     * Handles updateFavorites.
     */
    public void updateFavorites(List<Stop> favorites) {
        this.favoriteStops = favorites != null ? favorites : List.of();
        if (currentMode == SearchOverlayMode.FAVORITES) {
            filterResults();
        }
    }

    @Override
    /**
     * Updates the bounds value.
     */
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (contentPanel != null && isVisible()) {
            positionContentPanel(width, height);
        }
    }

    private void positionContentPanel(int width, int height) {
        int px = (width - PANEL_WIDTH) / 2;
        int py = (height - PANEL_HEIGHT) / 3;
        contentPanel.setBounds(px, py, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private void openOverlay(SearchOverlayMode mode, List<Stop> favorites) {
        searchField.setText("");
        if (favorites != null) {
            this.favoriteStops = favorites;
        }
        currentMode = mode;
        modeTabs.setCurrentMode(mode);
        filterResults();
        setVisible(true);
        positionContentPanel(getWidth(), getHeight());
        SwingUtilities.invokeLater(() -> searchField.requestFocusInWindow());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    @Override
    /**
     * Returns whether opaque.
     */
    public boolean isOpaque() {
        return false;
    }
}
