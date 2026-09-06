package dao;

import database.DatabaseManager;
import model.Technician;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  DATA ACCESS LAYER — TechnicianDAO.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  Provides all SQLite persistence operations for the
 *  {@code technicians} table. Replaces the in-memory
 *  {@code technicians} ArrayList that previously lived in Main.java.
 *
 *  Supported operations:
 *  - addTechnician      : INSERT a new technician; sets auto-generated tech_id.
 *  - getAllTechnicians   : SELECT all technicians; joins with job_cards to compute
 *                         a live {@code activeJobsCount} for each technician.
 *  - getTechnicianById  : SELECT a single technician by primary key.
 *
 *  Design note: The {@code activeJobsCount} field on {@link Technician} is
 *  derived from a LEFT JOIN against {@code job_cards} at query time rather
 *  than being stored as a redundant column, keeping the DB normalised.
 */
public class TechnicianDAO {

    // ── Shared SQL fragment that computes the live active-job count ──────
    // Counts rows in job_cards where status is NOT a terminal state.
    private static final String SELECT_WITH_COUNT =
            "SELECT t.tech_id, t.name, t.specialty, " +
            "COUNT(CASE WHEN j.status NOT IN ('COMPLETED','CANCELLED') THEN 1 END) AS active_count " +
            "FROM technicians t " +
            "LEFT JOIN job_cards j ON t.tech_id = j.tech_id ";

    // ─── Insert ───────────────────────────────────────────────

    /**
     * Persists a new technician to the {@code technicians} table.
     * After a successful insert the auto-generated {@code tech_id}
     * is written back into {@code technician} via {@link Technician#setTechId}.
     *
     * @param technician The {@link Technician} object to save (pass id = 0).
     */
    public void addTechnician(Technician technician) {
        String sql = "INSERT INTO technicians (name, specialty) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, technician.getName());
            pstmt.setString(2, technician.getSpecialty());
            pstmt.executeUpdate();

            // Capture the auto-generated primary key and push it back into the object
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                technician.setTechId(rs.getInt(1));
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error saving technician to database: " + e.getMessage());
        }
    }

    // ─── Query: All ───────────────────────────────────────────

    /**
     * Returns all technicians on the roster from the database.
     * Each technician's {@code activeJobsCount} is computed live from the
     * {@code job_cards} table via a LEFT JOIN and COUNT.
     *
     * @return A {@link List} of {@link Technician} objects ordered by {@code tech_id}.
     */
    public List<Technician> getAllTechnicians() {
        List<Technician> list = new ArrayList<>();
        String sql = SELECT_WITH_COUNT + "GROUP BY t.tech_id ORDER BY t.tech_id";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(buildTechnician(rs));
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error fetching technicians from database: " + e.getMessage());
        }
        return list;
    }

    // ─── Query: By ID ─────────────────────────────────────────

    /**
     * Looks up a single technician by their primary key.
     * The returned object includes a live {@code activeJobsCount}.
     *
     * @param id The {@code tech_id} to search for.
     * @return The matching {@link Technician}, or {@code null} if not found.
     */
    public Technician getTechnicianById(int id) {
        String sql = SELECT_WITH_COUNT + "WHERE t.tech_id = ? GROUP BY t.tech_id";

        try (Connection conn  = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return buildTechnician(rs);
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error fetching technician by ID: " + e.getMessage());
        }
        return null;
    }

    // ─── Private Helper ───────────────────────────────────────

    /**
     * Builds a {@link Technician} from the current row of a {@link ResultSet},
     * also setting the computed {@code activeJobsCount}.
     */
    private Technician buildTechnician(ResultSet rs) throws SQLException {
        Technician t = new Technician(
                rs.getInt   ("tech_id"),
                rs.getString("name"),
                rs.getString("specialty")
        );
        // Override the default 0 with the live count derived from the DB join
        t.setActiveJobsCount(rs.getInt("active_count"));
        return t;
    }
}
