package dao;

import database.DatabaseManager;
import model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    // Save a new customer to SQLite
    public void addCustomer(Customer customer) {
        String sql = "INSERT INTO customers (name, phone) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getPhone());
            pstmt.executeUpdate();

            // Capture the auto-generated database primary key ID
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                customer.setCustomerId(rs.getInt(1));
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error saving customer to database: " + e.getMessage());
        }
    }

    // Fetch all customers from SQLite
    public List<Customer> getAllCustomers() {
        List<Customer> customersList = new ArrayList<>();
        String sql = "SELECT * FROM customers";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Customer c = new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("name"),
                        rs.getString("phone")
                );
                customersList.add(c);
            }
        } catch (SQLException e) {
            System.out.println("  [!] Error fetching customers from database: " + e.getMessage());
        }
        return customersList;
    }

    // Fetch a specific customer by their ID from SQLite
    public Customer getCustomerById(int id) {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("name"),
                        rs.getString("phone")
                );
            }
        } catch (SQLException e) {
            System.out.println("  [!] Error fetching customer by ID: " + e.getMessage());
        }
        return null;
    }
}