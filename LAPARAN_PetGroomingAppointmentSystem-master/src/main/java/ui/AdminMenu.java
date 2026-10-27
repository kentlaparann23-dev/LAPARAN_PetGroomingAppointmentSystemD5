
package ui;

import dao.UserDao;
import model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final Scanner scanner;
    private final UserDao userDao;

    public AdminMenu(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
    }

    public void start() {
        while (true) {
            System.out.println("\n===== ADMINISTRATOR MENU =====");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" ->
                            displayUsers(
                                    userDao.getAllUsers(),
                                    "ALL SYSTEM USERS"
                            );

                    case "2" ->
                            displayUsers(
                                    userDao.getPendingUsers(),
                                    "PENDING REGISTRATIONS"
                            );

                    case "3" -> approveUser();

                    case "4" -> {
                        System.out.println("Logged out.");
                        return;
                    }

                    default ->
                            System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private void approveUser() throws SQLException {
        List<User> pendingUsers = userDao.getPendingUsers();

        displayUsers(pendingUsers, "PENDING REGISTRATIONS");

        if (pendingUsers.isEmpty()) {
            System.out.println("No pending accounts to approve.");
            return;
        }

        System.out.print("Enter User ID to approve (0 to cancel): ");
        String input = scanner.nextLine().trim();

        if (input.equals("0")) {
            System.out.println("Approval cancelled.");
            return;
        }

        int userId;

        try {
            userId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID. Enter a number.");
            return;
        }

        boolean exists = pendingUsers.stream()
                .anyMatch(user -> user.id() == userId);

        if (!exists) {
            System.out.println(
                    "User ID not found in pending registrations."
            );
            return;
        }

        if (userDao.approveUser(userId)) {
            System.out.println(
                    "SUCCESS: User ID " + userId
                            + " has been approved!"
            );
            System.out.println("Account status: APPROVED");
        } else {
            System.out.println("Approval failed.");
        }
    }

    private void displayUsers(List<User> users, String title) {
        System.out.println("\n===== " + title + " =====");

        String border =
                "+-------+-----------------+--------------+------------+";

        System.out.println(border);
        System.out.printf(
                "| %-5s | %-15s | %-12s | %-10s |%n",
                "ID", "USERNAME", "ROLE", "STATUS"
        );
        System.out.println(border);

        if (users.isEmpty()) {
            System.out.printf(
                    "| %-51s |%n", "No accounts found."
            );
        } else {
            for (User user : users) {
                System.out.printf(
                        "| %-5s | %-15s | %-12s | %-10s |%n",
                        fit(String.valueOf(user.id()), 5),
                        fit(user.username(), 15),
                        fit(user.role(), 12),
                        fit(user.status(), 10)
                );
            }
        }

        System.out.println(border);
    }

    private String fit(String value, int width) {
        if (value == null) {
            return "";
        }

        return value.length() > width
                ? value.substring(0, width)
                : value;
    }
}
