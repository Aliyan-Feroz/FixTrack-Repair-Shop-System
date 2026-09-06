package dao;

import database.DatabaseManager;
import model.Part;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  DATA ACCESS LAYER — InventoryDAO.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  Provides all SQLite persistence operations for the Parts /
 *  Inventory catalogue.  Replaces the in-memory {@code inventory}
 *  ArrayList that previously lived in Main.java.
 *
 *  Supported operations:
 *  - addPart      : INSERT a new part; sets auto-generated part_id.
 *  - updatePart   : UPDATE stock quantity and price (used after
 *                   restocking or after deducting parts for a job).
 *  - getAllParts   : SELECT all inventory rows.
 *  - getPartById  : SELECT a single part by primary key.
 */
public class InventoryDAO {

    // ─── Insert ───────────────────────────────────────────────

    /**
     * Persists a new part to the {@code inventory} table.
     * After a successful insert the auto-generated {@code part_id}
     * is written back into {@code part} via {@link Part#setPartId}.
     *
     * @param part The {@link Part} object to save (pass id = 0).
     */
    public void addPart(Part part) {
        String sql = "INSERT INTO inventory (part_name, stock_quantity, price) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, part.getPartName());
            pstmt.setInt   (2, part.getStockQuantity());
            pstmt.setDouble(3, part.getPrice());
            pstmt.executeUpdate();

            // Capture the auto-generated primary key and push it back into the object
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                part.setPartId(rs.getInt(1));
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error saving part to database: " + e.getMessage());
        }
    }

    // ─── Update ───────────────────────────────────────────────

    /**
     * Persists the current {@code stock_quantity} and {@code price}
     * of an existing part back to the database.
     * Call this after {@link Part#updateStock} to ensure the change survives a restart.
     *
     * @param part The {@link Part} object whose values should be written to the DB.
     */
    public void updatePart(Part part) {
        String sql = "UPDATE inventory SET stock_quantity = ?, price = ? WHERE part_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt   (1, part.getStockQuantity());
            pstmt.setDouble(2, part.getPrice());
            pstmt.setInt   (3, part.getPartId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("  [!] Error updating part in database: " + e.getMessage());
        }
    }

    // ─── Query: All ───────────────────────────────────────────

    /**
     * Returns the complete inventory catalogue from the database.
     *
     * @return A {@link List} of {@link Part} objects, ordered by {@code part_id}.
     */
    public List<Part> getAllParts() {
        List<Part> parts = new ArrayList<>();
        String sql = "SELECT * FROM inventory ORDER BY part_id";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                parts.add(new Part(
                        rs.getInt   ("part_id"),
                        rs.getString("part_name"),
                        rs.getInt   ("stock_quantity"),
                        rs.getDouble("price")
                ));
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error fetching inventory from database: " + e.getMessage());
        }
        return parts;
    }

    // ─── Query: By ID ─────────────────────────────────────────

    /**
     * Looks up a single part by its primary key.
     *
     * @param id The {@code part_id} to search for.
     * @return The matching {@link Part}, or {@code null} if not found.
     */
    public Part getPartById(int id) {
        String sql = "SELECT * FROM inventory WHERE part_id = ?";

        try (Connection conn  = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Part(
                        rs.getInt   ("part_id"),
                        rs.getString("part_name"),
                        rs.getInt   ("stock_quantity"),
                        rs.getDouble("price")
                );
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error fetching part by ID: " + e.getMessage());
        }
        return null;
    }
}
