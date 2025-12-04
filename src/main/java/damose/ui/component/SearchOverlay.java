package damose.ui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import damose.config.AppConstants;
import damose.data.model.Stop;
import damose.service.FavoritesService;

/**
 * Spotlight-style search overlay.
 */
public class SearchOverlay extends JPanel {

    private final JTextField searchField;
    private final DefaultListModel<Stop> listModel;
    private final JList<Stop> resultList;
    private final JPanel contentPanel;
    private final JLabel stopsModeBtn;
    private final JLabel linesModeBtn;
    private final JLabel favoritesModeBtn;

    private enum SearchMode { STOPS, LINES, FAVORITES }
    private SearchMode currentMode = SearchMode.STOPS;
    private List<Stop> allStops = new ArrayList<>();
    private List<Stop> allLines = new ArrayList<>();
    private List<Stop> favoriteStops = new ArrayList<>();
    private Consumer<Stop> onSelect;

    public SearchOverlay() {
        setLayout(null);
        setOpaque(false);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(AppConstants.BG_MEDIUM);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1),
            new EmptyBorder(16, 16, 16, 16)
        ));

        JPanel modePanel = new JPanel();
        modePanel.setOpaque(false);
        modePanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        stopsModeBtn = createModeButton("Fermate", true);
        linesModeBtn = createModeButton("Linee", false);
        favoritesModeBtn = createModeButton("Preferiti", false);

        stopsModeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (currentMode != SearchMode.STOPS) {
                    currentMode = SearchMode.STOPS;
                    updateModeButtons();
                    filterResults();
                }
            }
        });
        linesModeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (currentMode != SearchMode.LINES) {
                    currentMode = SearchMode.LINES;
                    updateModeButtons();
                    filterResults();
                }
            }
        });
        favoritesModeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (currentMode != SearchMode.FAVORITES) {
                    currentMode = SearchMode.FAVORITES;
                    updateModeButtons();
                    filterResults();
                }
            }
        });

        modePanel.add(stopsModeBtn);
        modePanel.add(Box.createHorizontalStrut(6));
        modePanel.add(linesModeBtn);
        modePanel.add(Box.createHorizontalStrut(6));
        modePanel.add(favoritesModeBtn);

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
            public void insertUpdate(DocumentEvent e) { filterResults(); }
            public void removeUpdate(DocumentEvent e) { filterResults(); }
            public void changedUpdate(DocumentEvent e) { filterResults(); }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
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
                    // Cycle through modes: STOPS -> LINES -> FAVORITES -> STOPS
                    currentMode = switch (currentMode) {
                        case STOPS -> SearchMode.LINES;
                        case LINES -> SearchMode.FAVORITES;
                        case FAVORITES -> SearchMode.STOPS;
                    };
                    updateModeButtons();
                    filterResults();
                }
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(modePanel, BorderLayout.NORTH);
        topPanel.add(searchField, BorderLayout.CENTER);
        contentPanel.add(topPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setBackground(AppConstants.LIST_BG);
        resultList.setForeground(AppConstants.TEXT_PRIMARY);
        resultList.setSelectionBackground(AppConstants.ACCENT);
        resultList.setSelectionForeground(Color.WHITE);
        resultList.setFont(AppConstants.FONT_BODY);
        resultList.setFixedCellHeight(58); // Increased for better spacing
        resultList.setCellRenderer(new StopCellRenderer());

        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    selectCurrentAndClose();
                } else if (e.getKeyCode() == KeyEvent.VK_F) {
                    e.consume();
                    toggleSelectedFavorite();
                }
            }
        });

        resultList.addMouseListener(new MouseAdapter() {
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

    private JLabel createModeButton(String text, boolean selected) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setOpaque(true);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(new EmptyBorder(8, 20, 8, 20));
        if (selected) {
            label.setBackground(AppConstants.ACCENT);
            label.setForeground(Color.WHITE);
        } else {
            label.setBackground(AppConstants.BG_FIELD);
            label.setForeground(AppConstants.TEXT_SECONDARY);
        }
        return label;
    }

    private void updateModeButtons() {
        // Reset all to unselected
        stopsModeBtn.setBackground(AppConstants.BG_FIELD);
        stopsModeBtn.setForeground(AppConstants.TEXT_SECONDARY);
        linesModeBtn.setBackground(AppConstants.BG_FIELD);
        linesModeBtn.setForeground(AppConstants.TEXT_SECONDARY);
        favoritesModeBtn.setBackground(AppConstants.BG_FIELD);
        favoritesModeBtn.setForeground(AppConstants.TEXT_SECONDARY);
        
        // Highlight selected
        JLabel selected = switch (currentMode) {
            case STOPS -> stopsModeBtn;
            case LINES -> linesModeBtn;
            case FAVORITES -> favoritesModeBtn;
        };
        selected.setBackground(AppConstants.ACCENT);
        selected.setForeground(Color.WHITE);
    }

    private void filterResults() {
        String query = searchField.getText().toLowerCase().trim();
        listModel.clear();

        List<Stop> source = switch (currentMode) {
            case STOPS -> allStops;
            case LINES -> allLines;
            case FAVORITES -> favoriteStops;
        };
        
        int count = 0;
        int limit = (currentMode == SearchMode.FAVORITES) ? 100 : 50;

        for (Stop s : source) {
            if (count >= limit) break;
            String name = s.getStopName().toLowerCase();
            String id = s.getStopId().toLowerCase();
            if (query.isEmpty() || name.contains(query) || id.contains(query)) {
                listModel.addElement(s);
                count++;
            }
        }

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
        if (selected != null) {
            Consumer<Stop> callback = onSelect;
            closeOverlay();
            if (callback != null) {
                SwingUtilities.invokeLater(() -> callback.accept(selected));
            }
        }
    }
    
    private void toggleSelectedFavorite() {
        Stop selected = resultList.getSelectedValue();
        if (selected != null) {
            if (selected.isFakeLine()) {
                FavoritesService.toggleLineFavorite(selected.getStopId());
            } else {
                FavoritesService.toggleFavorite(selected.getStopId());
            }
            // Refresh the list to show updated star
            resultList.repaint();
            
            // If in favorites mode, refresh the list
            if (currentMode == SearchMode.FAVORITES) {
                favoriteStops = FavoritesService.getAllFavorites();
                filterResults();
            }
        }
    }

    private void closeOverlay() {
        setVisible(false);
    }

    public void setData(List<Stop> stops, List<Stop> lines) {
        this.allStops = stops != null ? new ArrayList<>(stops) : new ArrayList<>();
        this.allLines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    public void setOnSelect(Consumer<Stop> callback) {
        this.onSelect = callback;
    }

    public void showSearch() {
        searchField.setText("");
        currentMode = SearchMode.STOPS;
        updateModeButtons();
        filterResults();
        setVisible(true);

        int panelW = 520;
        int panelH = 420;
        int x = (getWidth() - panelW) / 2;
        int y = (getHeight() - panelH) / 3;
        contentPanel.setBounds(x, y, panelW, panelH);

        SwingUtilities.invokeLater(() -> searchField.requestFocusInWindow());
    }
    
    /**
     * Show favorites tab in the search overlay.
     */
    public void showFavorites(List<Stop> favorites) {
        searchField.setText("");
        this.favoriteStops = favorites != null ? new ArrayList<>(favorites) : new ArrayList<>();
        currentMode = SearchMode.FAVORITES;
        updateModeButtons();
        filterResults();
        
        setVisible(true);

        int panelW = 520;
        int panelH = 420;
        int x = (getWidth() - panelW) / 2;
        int y = (getHeight() - panelH) / 3;
        contentPanel.setBounds(x, y, panelW, panelH);

        SwingUtilities.invokeLater(() -> searchField.requestFocusInWindow());
    }
    
    /**
     * Update the favorites list (called when favorites change).
     */
    public void updateFavorites(List<Stop> favorites) {
        this.favoriteStops = new ArrayList<>(favorites);
        if (currentMode == SearchMode.FAVORITES) {
            filterResults();
        }
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (contentPanel != null && isVisible()) {
            int panelW = 520;
            int panelH = 420;
            int px = (width - panelW) / 2;
            int py = (height - panelH) / 3;
            contentPanel.setBounds(px, py, panelW, panelH);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    private class StopCellRenderer extends JPanel implements ListCellRenderer<Stop> {
        private final JLabel nameLabel;
        private final JLabel idLabel;
        private final JLabel starLabel;
        private ImageIcon yellowStarIcon;

        public StopCellRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setOpaque(true);
            
            // Create yellow star icon for favorites
            yellowStarIcon = new ImageIcon(createYellowStar(16));

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

            nameLabel = new JLabel();
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

            idLabel = new JLabel();
            idLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            textPanel.add(nameLabel);
            textPanel.add(Box.createVerticalStrut(3));
            textPanel.add(idLabel);

            add(textPanel, BorderLayout.CENTER);
            
            // Star indicator for favorites
            starLabel = new JLabel();
            starLabel.setBorder(new EmptyBorder(0, 8, 0, 4));
            starLabel.setPreferredSize(new Dimension(24, 24));
            add(starLabel, BorderLayout.EAST);
        }
        
        private Image createYellowStar(int size) {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                               java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            int[] xPoints = new int[10];
            int[] yPoints = new int[10];
            double angleStep = Math.PI / 5;
            int cx = size / 2;
            int cy = size / 2;
            int outerR = size / 2 - 1;
            int innerR = size / 4;

            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + i * angleStep;
                int r = (i % 2 == 0) ? outerR : innerR;
                xPoints[i] = (int) (cx + r * Math.cos(angle));
                yPoints[i] = (int) (cy + r * Math.sin(angle));
            }
            
            g2.setColor(new Color(255, 200, 50)); // Gold/Yellow fill
            g2.fillPolygon(xPoints, yPoints, 10);
            g2.setColor(new Color(200, 150, 0)); // Darker border
            g2.drawPolygon(xPoints, yPoints, 10);
            g2.dispose();
            return img;
        }

        @Override
        public java.awt.Component getListCellRendererComponent(JList<? extends Stop> list,
                Stop value, int index, boolean isSelected, boolean cellHasFocus) {

            String name = value.getStopName();
            if (name.length() > 38) name = name.substring(0, 38) + "...";
            nameLabel.setText(name);
            
            // Check favorite status
            boolean isFavorite;
            if (value.isFakeLine()) {
                idLabel.setText("Linea bus");
                isFavorite = FavoritesService.isLineFavorite(value.getStopId());
            } else {
                idLabel.setText("Stop ID: " + value.getStopId());
                isFavorite = FavoritesService.isFavorite(value.getStopId());
            }
            
            // Show yellow star if favorite
            starLabel.setIcon(isFavorite ? yellowStarIcon : null);

            if (isSelected) {
                setBackground(AppConstants.ACCENT);
                nameLabel.setForeground(Color.WHITE);
                idLabel.setForeground(new Color(220, 220, 220));
            } else {
                setBackground(AppConstants.LIST_BG);
                nameLabel.setForeground(AppConstants.TEXT_PRIMARY);
                idLabel.setForeground(AppConstants.TEXT_SECONDARY);
            }

            return this;
        }
    }
}

