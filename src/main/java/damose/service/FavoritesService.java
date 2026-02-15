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

import damose.model.Stop;
import damose.database.SessionManager;

/**
 * Provides service logic for favorites service.
 */
public final class FavoritesService {

    private static final Set<String> favoriteStopIds = new HashSet<>();
    private static final Set<String> favoriteLineIds = new HashSet<>();
    private static List<Stop> allStopsCache = List.of();
    private static List<Stop> allLinesCache = List.of();
    private static Runnable onFavoritesChanged;

    private FavoritesService() {
    }

    /**
     * Returns the result of init.
     */
    public static void init(List<Stop> allStops) {
        init(allStops, List.of());
    }

    /**
     * Returns the result of init.
     */
    public static void init(List<Stop> allStops, List<Stop> allLines) {
        allStopsCache = allStops != null ? allStops : List.of();
        allLinesCache = allLines != null ? allLines : List.of();
        loadFavorites();
        loadLineFavorites();
    }

    /**
     * Registers callback for favorites changed.
     */
    public static void setOnFavoritesChanged(Runnable callback) {
        onFavoritesChanged = callback;
    }

    /**
     * Returns whether favorite.
     */
    public static boolean isFavorite(String stopId) {
        if (!canPersistFavorites()) return false;
        return favoriteStopIds.contains(stopId);
    }

    /**
     * Returns the result of toggleFavorite.
     */
    public static boolean toggleFavorite(String stopId) {
        if (!canPersistFavorites()) {
            return false;
        }
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
     * Returns the result of addFavorite.
     */
    public static void addFavorite(String stopId) {
        if (!canPersistFavorites()) return;
        if (!favoriteStopIds.contains(stopId)) {
            favoriteStopIds.add(stopId);
            saveFavorites();
            if (onFavoritesChanged != null) {
                onFavoritesChanged.run();
            }
        }
    }

    /**
     * Returns the result of removeFavorite.
     */
    public static void removeFavorite(String stopId) {
        if (!canPersistFavorites()) return;
        if (favoriteStopIds.remove(stopId)) {
            saveFavorites();
            if (onFavoritesChanged != null) {
                onFavoritesChanged.run();
            }
        }
    }

    /**
     * Returns the favorite stops.
     */
    public static List<Stop> getFavoriteStops() {
        if (!canPersistFavorites()) return new ArrayList<>();
        List<Stop> favorites = new ArrayList<>();
        for (Stop stop : allStopsCache) {
            if (favoriteStopIds.contains(stop.getStopId())) {
                favorites.add(stop);
            }
        }
        return favorites;
    }

    /**
     * Returns the favorite lines.
     */
    public static List<Stop> getFavoriteLines() {
        if (!canPersistFavorites()) return new ArrayList<>();
        List<Stop> favorites = new ArrayList<>();
        for (Stop line : allLinesCache) {
            if (favoriteLineIds.contains(line.getStopId())) {
                favorites.add(line);
            }
        }
        return favorites;
    }

    /**
     * Returns the all favorites.
     */
    public static List<Stop> getAllFavorites() {
        if (!canPersistFavorites()) return new ArrayList<>();
        List<Stop> all = new ArrayList<>();
        all.addAll(getFavoriteStops());
        all.addAll(getFavoriteLines());
        return all;
    }

    /**
     * Returns the favorites count.
     */
    public static int getFavoritesCount() {
        if (!canPersistFavorites()) return 0;
        return favoriteStopIds.size() + favoriteLineIds.size();
    }


    /**
     * Returns whether line favorite.
     */
    public static boolean isLineFavorite(String lineId) {
        if (!canPersistFavorites()) return false;
        return favoriteLineIds.contains(lineId);
    }

    /**
     * Returns the result of toggleLineFavorite.
     */
    public static boolean toggleLineFavorite(String lineId) {
        if (!canPersistFavorites()) {
            return false;
        }
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
     * Returns the result of addLineFavorite.
     */
    public static void addLineFavorite(String lineId) {
        if (!canPersistFavorites()) return;
        if (!favoriteLineIds.contains(lineId)) {
            favoriteLineIds.add(lineId);
            saveLineFavorites();
            if (onFavoritesChanged != null) {
                onFavoritesChanged.run();
            }
        }
    }

    /**
     * Returns the result of removeLineFavorite.
     */
    public static void removeLineFavorite(String lineId) {
        if (!canPersistFavorites()) return;
        if (favoriteLineIds.remove(lineId)) {
            saveLineFavorites();
            if (onFavoritesChanged != null) {
                onFavoritesChanged.run();
            }
        }
    }

    private static File getFavoritesFile() {
        if (!canPersistFavorites()) return null;
        String username = SessionManager.getCurrentUser().getUsername();

        File dir = new File(System.getProperty("user.home"), ".damose");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "favorites_" + username + ".txt");
    }

    private static void loadFavorites() {
        favoriteStopIds.clear();
        File file = getFavoritesFile();

        if (file == null || !file.exists()) {
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
        } catch (java.io.FileNotFoundException e) {
            System.out.println("Favorites file not found: " + e.getMessage());
        } catch (java.io.IOException e) {
            System.err.println("Error loading favorites: " + e.getMessage());
        }
    }

    private static void saveFavorites() {
        File file = getFavoritesFile();
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String id : favoriteStopIds) {
                writer.write(id);
                writer.newLine();
            }
        } catch (java.io.IOException e) {
            System.err.println("Error saving favorites: " + e.getMessage());
        }
    }


    private static File getLineFavoritesFile() {
        if (!canPersistFavorites()) return null;
        String username = SessionManager.getCurrentUser().getUsername();

        File dir = new File(System.getProperty("user.home"), ".damose");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "favorite_lines_" + username + ".txt");
    }

    private static void loadLineFavorites() {
        favoriteLineIds.clear();
        File file = getLineFavoritesFile();

        if (file == null || !file.exists()) {
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
        } catch (java.io.FileNotFoundException e) {
            System.out.println("Line favorites file not found: " + e.getMessage());
        } catch (java.io.IOException e) {
            System.err.println("Error loading line favorites: " + e.getMessage());
        }
    }

    private static void saveLineFavorites() {
        File file = getLineFavoritesFile();
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String id : favoriteLineIds) {
                writer.write(id);
                writer.newLine();
            }
        } catch (java.io.IOException e) {
            System.err.println("Error saving line favorites: " + e.getMessage());
        }
    }

    /**
     * Returns whether favorites can be persisted for current session.
     */
    public static boolean canPersistFavorites() {
        return SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null;
    }
}

