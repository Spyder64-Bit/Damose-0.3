package damose.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import damose.data.model.Stop;
import damose.database.SessionManager;

/**
 * Service for managing favorite stops and bus lines per user.
 * Favorites are stored in files in the user's home directory.
 */
public final class FavoritesService {

    private static final Set<String> favoriteStopIds = new HashSet<>();
    private static final Set<String> favoriteLineIds = new HashSet<>();
    private static List<Stop> allStopsCache = new ArrayList<>();
    private static List<Stop> allLinesCache = new ArrayList<>();
    private static Runnable onFavoritesChanged;

    private FavoritesService() {
        // Utility class
    }

    /**
     * Initialize the service with stops only.
     */
    public static void init(List<Stop> allStops) {
        init(allStops, new ArrayList<>());
    }
    
    /**
     * Initialize the service with the full list of stops and lines.
     */
    public static void init(List<Stop> allStops, List<Stop> allLines) {
        allStopsCache = new ArrayList<>(allStops);
        allLinesCache = new ArrayList<>(allLines);
        loadFavorites();
        loadLineFavorites();
    }

    /**
     * Set callback for when favorites change.
     */
    public static void setOnFavoritesChanged(Runnable callback) {
        onFavoritesChanged = callback;
    }

    /**
     * Check if a stop is a favorite.
     */
    public static boolean isFavorite(String stopId) {
        return favoriteStopIds.contains(stopId);
    }

    /**
     * Toggle favorite status of a stop.
     * @return true if now favorite, false if removed
     */
    public static boolean toggleFavorite(String stopId) {
        boolean result;
        if (favoriteStopIds.contains(stopId)) {
            favoriteStopIds.remove(stopId);
            result = false;
        } else {
            favoriteStopIds.add(stopId);
            result = true;
        }
        saveFavorites();
        if (onFavoritesChanged != null) {
            onFavoritesChanged.run();
        }
        return result;
    }

    /**
     * Add a stop to favorites.
     */
    public static void addFavorite(String stopId) {
        if (!favoriteStopIds.contains(stopId)) {
            favoriteStopIds.add(stopId);
            saveFavorites();
            if (onFavoritesChanged != null) {
                onFavoritesChanged.run();
            }
        }
    }

    /**
     * Remove a stop from favorites.
     */
    public static void removeFavorite(String stopId) {
        if (favoriteStopIds.remove(stopId)) {
            saveFavorites();
            if (onFavoritesChanged != null) {
                onFavoritesChanged.run();
            }
        }
    }

    /**
     * Get all favorite stops.
     */
    public static List<Stop> getFavoriteStops() {
        List<Stop> favorites = new ArrayList<>();
        for (Stop stop : allStopsCache) {
            if (favoriteStopIds.contains(stop.getStopId())) {
                favorites.add(stop);
            }
        }
        return favorites;
    }
    
    /**
     * Get all favorite lines.
     */
    public static List<Stop> getFavoriteLines() {
        List<Stop> favorites = new ArrayList<>();
        for (Stop line : allLinesCache) {
            if (favoriteLineIds.contains(line.getStopId())) {
                favorites.add(line);
            }
        }
        return favorites;
    }
    
    /**
     * Get all favorites (stops + lines).
     */
    public static List<Stop> getAllFavorites() {
        List<Stop> all = new ArrayList<>();
        all.addAll(getFavoriteStops());
        all.addAll(getFavoriteLines());
        return all;
    }

    /**
     * Get count of favorites.
     */
    public static int getFavoritesCount() {
        return favoriteStopIds.size() + favoriteLineIds.size();
    }
    
    // ===== LINE FAVORITES =====
    
    /**
     * Check if a line is a favorite.
     */
    public static boolean isLineFavorite(String lineId) {
        return favoriteLineIds.contains(lineId);
    }
    
    /**
     * Toggle favorite status of a line.
     * @return true if now favorite, false if removed
     */
    public static boolean toggleLineFavorite(String lineId) {
        boolean result;
        if (favoriteLineIds.contains(lineId)) {
            favoriteLineIds.remove(lineId);
            result = false;
        } else {
            favoriteLineIds.add(lineId);
            result = true;
        }
        saveLineFavorites();
        if (onFavoritesChanged != null) {
            onFavoritesChanged.run();
        }
        return result;
    }
    
    /**
     * Add a line to favorites.
     */
    public static void addLineFavorite(String lineId) {
        if (!favoriteLineIds.contains(lineId)) {
            favoriteLineIds.add(lineId);
            saveLineFavorites();
            if (onFavoritesChanged != null) {
                onFavoritesChanged.run();
            }
        }
    }
    
    /**
     * Remove a line from favorites.
     */
    public static void removeLineFavorite(String lineId) {
        if (favoriteLineIds.remove(lineId)) {
            saveLineFavorites();
            if (onFavoritesChanged != null) {
                onFavoritesChanged.run();
            }
        }
    }

    private static File getFavoritesFile() {
        String username = "default";
        if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null) {
            username = SessionManager.getCurrentUser().getUsername();
        }
        
        File dir = new File(System.getProperty("user.home"), ".damose");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "favorites_" + username + ".txt");
    }

    private static void loadFavorites() {
        favoriteStopIds.clear();
        File file = getFavoritesFile();
        
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = line.trim();
                if (!id.isEmpty()) {
                    favoriteStopIds.add(id);
                }
            }
            System.out.println("Loaded " + favoriteStopIds.size() + " favorites");
        } catch (Exception e) {
            System.out.println("Error loading favorites: " + e.getMessage());
        }
    }

    private static void saveFavorites() {
        File file = getFavoritesFile();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String id : favoriteStopIds) {
                writer.write(id);
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println("Error saving favorites: " + e.getMessage());
        }
    }
    
    // ===== LINE FAVORITES PERSISTENCE =====
    
    private static File getLineFavoritesFile() {
        String username = "default";
        if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null) {
            username = SessionManager.getCurrentUser().getUsername();
        }
        
        File dir = new File(System.getProperty("user.home"), ".damose");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "favorite_lines_" + username + ".txt");
    }

    private static void loadLineFavorites() {
        favoriteLineIds.clear();
        File file = getLineFavoritesFile();
        
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = line.trim();
                if (!id.isEmpty()) {
                    favoriteLineIds.add(id);
                }
            }
            System.out.println("Loaded " + favoriteLineIds.size() + " favorite lines");
        } catch (Exception e) {
            System.out.println("Error loading line favorites: " + e.getMessage());
        }
    }

    private static void saveLineFavorites() {
        File file = getLineFavoritesFile();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String id : favoriteLineIds) {
                writer.write(id);
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println("Error saving line favorites: " + e.getMessage());
        }
    }
}

