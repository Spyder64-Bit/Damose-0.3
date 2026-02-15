package damose.database;

import java.time.LocalDateTime;

/**
 * Database support for user.
 */
public class User {

    private int id;
    private String username;
    private String email;
    private LocalDateTime createdAt;

    public User(int id, String username, String email, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }

    /**
     * Returns the id.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the created at.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    /**
     * Returns the result of toString.
     */
    public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
}

