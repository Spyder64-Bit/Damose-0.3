package damose.database;

/**
 * Database support for session manager.
 */
public final class SessionManager {

    private static User currentUser = null;

    private SessionManager() {
    }

    /**
     * Updates the current user value.
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Returns the current user.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns whether logged in.
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Returns the result of logout.
     */
    public static void logout() {
        currentUser = null;
    }

    /**
     * Returns the current user id.
     */
    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
}

