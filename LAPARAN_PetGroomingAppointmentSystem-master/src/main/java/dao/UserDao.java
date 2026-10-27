
package dao;

import model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class UserDao {

    public void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL CHECK (
                        role IN ('ADMINISTRATOR', 'RECEPTIONIST',
                                 'GROOMER', 'CUSTOMER')
                    ),
                    email TEXT,
                    account_status TEXT NOT NULL DEFAULT 'PENDING'
                        CHECK (account_status IN (
                            'PENDING', 'APPROVED',
                            'REJECTED', 'INACTIVE'
                        )),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public boolean register(
            String username,
            String password,
            String fullName,
            String email
    ) throws SQLException {
        String sql = """
                INSERT INTO users
                    (username, password_hash, full_name,
                     role, email, account_status)
                VALUES (?, ?, ?, 'CUSTOMER', ?, 'PENDING')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, hashPassword(password));
            statement.setString(3, fullName);
            statement.setString(
                    4, email == null || email.isBlank() ? null : email
            );

            statement.executeUpdate();
            return true;

        } catch (SQLException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().toUpperCase().contains("UNIQUE")) {
                return false;
            }
            throw exception;
        }
    }

    public User login(String username, String password)
            throws SQLException {
        String sql = """
                SELECT user_id, username, full_name, role, account_status
                FROM users
                WHERE username = ? AND password_hash = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, hashPassword(password));

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapUser(result);
                }
            }
        }

        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();

        String sql = """
                SELECT user_id, username, full_name, role, account_status
                FROM users
                ORDER BY user_id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                users.add(mapUser(result));
            }
        }

        return users;
    }

    public List<User> getPendingUsers() throws SQLException {
        List<User> users = new ArrayList<>();

        String sql = """
                SELECT user_id, username, full_name, role, account_status
                FROM users
                WHERE account_status = 'PENDING'
                ORDER BY user_id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                users.add(mapUser(result));
            }
        }

        return users;
    }

    public boolean approveUser(int userId) throws SQLException {
        String sql = """
                UPDATE users
                SET account_status = 'APPROVED'
                WHERE user_id = ? AND account_status = 'PENDING'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public void createAdminAccount(
            String username,
            String password,
            String fullName
    ) throws SQLException {
        String sql = """
                INSERT INTO users
                    (username, password_hash, full_name,
                     role, email, account_status)
                VALUES (?, ?, ?, 'ADMINISTRATOR', NULL, 'APPROVED')
                ON CONFLICT(username) DO NOTHING
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, hashPassword(password));
            statement.setString(3, fullName);

            int rows = statement.executeUpdate();

            if (rows == 1) {
                System.out.println("Administrator account created.");
            } else {
                System.out.println(
                        "That username already exists. No account was changed."
                );
            }
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        return new User(
                result.getInt("user_id"),
                result.getString("username"),
                result.getString("full_name"),
                result.getString("role"),
                result.getString("account_status")
        );
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Unable to hash password.", exception
            );
        }
    }
}
