package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // The database file will be generated in your project root
    private static final String DB_URL = "jdbc:sqlite:repair_shop.db";

    public static Connection getConnection() throws SQLException {
        try {
            // Diagnostic check to ensure the driver is loaded properly
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("  [CRITICAL] SQLite Driver JAR is missing or not linked properly in VS Code!");
        }
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Enable foreign key constraints (required for SQLite)
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Customers Table
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "customer_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "phone TEXT NOT NULL)");

            // 2. Inventory Table
            stmt.execute("CREATE TABLE IF NOT EXISTS inventory (" +
                    "part_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "part_name TEXT NOT NULL, " +
                    "stock_quantity INTEGER NOT NULL, " +
                    "price REAL NOT NULL)");

            // 3. Technicians Table
            stmt.execute("CREATE TABLE IF NOT EXISTS technicians (" +
                    "tech_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "specialty TEXT NOT NULL)");

            // 4. Users Table (For authentication and roles)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "role TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "reset_token TEXT, " +
                    "token_expiry DATETIME)");

            // 5. Job Cards Table
            stmt.execute("CREATE TABLE IF NOT EXISTS job_cards (" +
                    "job_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "customer_id INTEGER REFERENCES customers(customer_id), " +
                    "tech_id INTEGER REFERENCES technicians(tech_id), " +
                    "device_details TEXT, " +
                    "serial_no TEXT, " +
                    "complaint TEXT, " +
                    "diagnosis TEXT, " +
                    "status TEXT NOT NULL, " +
                    "labor_charge REAL DEFAULT 0.0, " +
                    "date_created TEXT NOT NULL)");

            // 6. Job Parts Table (Maps multiple parts to a single job card)
            stmt.execute("CREATE TABLE IF NOT EXISTS job_parts (" +
                    "job_id INTEGER REFERENCES job_cards(job_id), " +
                    "part_id INTEGER REFERENCES inventory(part_id), " +
                    "quantity INTEGER NOT NULL, " +
                    "PRIMARY KEY (job_id, part_id))");

            // 7. Invoices Table
            stmt.execute("CREATE TABLE IF NOT EXISTS invoices (" +
                    "invoice_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "job_id INTEGER REFERENCES job_cards(job_id), " +
                    "discount REAL DEFAULT 0.0, " +
                    "grand_total REAL NOT NULL, " +
                    "payment_status TEXT NOT NULL)");

            System.out.println("  [✓] Database linked and all tables verified.");

        } catch (SQLException e) {
            System.out.println("  [!] Database Connection Error: " + e.getMessage());
        }
    }
}