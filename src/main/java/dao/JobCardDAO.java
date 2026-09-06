package dao;

import business.JobCard;
import database.DatabaseManager;
import model.Customer;
import model.Part;
import model.Technician;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  DATA ACCESS LAYER — JobCardDAO.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  Provides all SQLite persistence operations for the
 *  {@code job_cards} and the related {@code job_parts} join-table.
 *  Replaces the in-memory {@code jobCards} ArrayList in Main.java.
 *
 *  Dependencies (passed in constructor for re-hydration):
 *  - {@link CustomerDAO}    — re-links Customer objects by FK
 *  - {@link TechnicianDAO}  — re-links Technician objects by FK
 *  - {@link InventoryDAO}   — re-links Part objects via job_parts join
 *
 *  Supported operations:
 *  - addJobCard    : INSERT a new job card row; sets auto-generated job_id.
 *  - updateJobCard : UPDATE mutable fields (status, diagnosis, labor, tech).
 *  - saveJobParts  : Atomically replace all job_parts rows for a given job.
 *  - getAllJobCards : Full re-hydration of every job card from the database.
 *  - getJobCardById: Re-hydrate a single job card by primary key.
 *
 *  Re-hydration strategy:
 *  Because {@link JobCard} holds live object references (Customer, Technician,
 *  List&lt;PartLineItem&gt;), this DAO reconstructs the full object graph on every
 *  load.  It uses the three re-hydration setters added to {@link JobCard}
 *  ({@code setStatus}, {@code setAssignedTech}, {@code addPartLineItem}) to
 *  restore persisted state without triggering business-logic side-effects such
 *  as stock deduction or state-machine transition validation.
 */
public class JobCardDAO {

    // ─── Dependencies ─────────────────────────────────────────
    private final CustomerDAO   customerDAO;
    private final TechnicianDAO technicianDAO;
    private final InventoryDAO  inventoryDAO;

    /**
     * Creates a new {@code JobCardDAO} with its required dependencies.
     *
     * @param customerDAO   DAO used to look up Customer objects by FK.
     * @param technicianDAO DAO used to look up Technician objects by FK.
     * @param inventoryDAO  DAO used to look up Part objects via job_parts.
     */
    public JobCardDAO(CustomerDAO customerDAO, TechnicianDAO technicianDAO, InventoryDAO inventoryDAO) {
        this.customerDAO   = customerDAO;
        this.technicianDAO = technicianDAO;
        this.inventoryDAO  = inventoryDAO;
    }

    // ─── Insert ───────────────────────────────────────────────

    /**
     * Persists a new job card to the {@code job_cards} table.
     * The current state of the {@link JobCard} object (including status,
     * diagnosis, labor charge, and technician) is saved at insert time.
     * After a successful insert the auto-generated {@code job_id} is
     * written back via {@link JobCard#setJobId}.
     *
     * @param jc The {@link JobCard} to persist (pass with id = 0).
     */
    public void addJobCard(JobCard jc) {
        String sql = "INSERT INTO job_cards " +
                     "(customer_id, tech_id, device_details, serial_no, complaint, " +
                     " diagnosis, status, labor_charge, date_created) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt   (1, jc.getCustomer().getCustomerId());
            if (jc.getAssignedTech() != null)
                pstmt.setInt(2, jc.getAssignedTech().getTechId());
            else
                pstmt.setNull(2, Types.INTEGER);
            pstmt.setString(3, jc.getDeviceDetails());
            pstmt.setString(4, jc.getSerialNo());
            pstmt.setString(5, jc.getComplaint());
            pstmt.setString(6, jc.getDiagnosis());
            pstmt.setString(7, jc.getStatus().name());
            pstmt.setDouble(8, jc.getLaborCharge());
            pstmt.setString(9, jc.getDateCreated());
            pstmt.executeUpdate();

            // Capture the auto-generated primary key and push it back into the object
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                jc.setJobId(rs.getInt(1));
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error saving job card to database: " + e.getMessage());
        }
    }

    // ─── Update ───────────────────────────────────────────────

    /**
     * Persists the mutable fields of an existing job card back to the database.
     * Call this after any status transition, diagnosis update, labor charge
     * change, or technician assignment.
     *
     * @param jc The {@link JobCard} whose current state should be written to DB.
     */
    public void updateJobCard(JobCard jc) {
        String sql = "UPDATE job_cards " +
                     "SET status = ?, diagnosis = ?, labor_charge = ?, tech_id = ? " +
                     "WHERE job_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, jc.getStatus().name());
            pstmt.setString(2, jc.getDiagnosis());
            pstmt.setDouble(3, jc.getLaborCharge());
            if (jc.getAssignedTech() != null)
                pstmt.setInt(4, jc.getAssignedTech().getTechId());
            else
                pstmt.setNull(4, Types.INTEGER);
            pstmt.setInt   (5, jc.getJobId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("  [!] Error updating job card in database: " + e.getMessage());
        }
    }

    // ─── Save Parts (atomic replace) ──────────────────────────

    /**
     * Atomically replaces all {@code job_parts} rows for the given job card
     * with the current contents of {@link JobCard#getPartsUsed()}.
     *
     * <p>Uses a transaction: DELETE existing rows first, then INSERT all
     * current {@link business.JobCard.PartLineItem} entries as a batch.
     * Rolls back on any error to avoid partial writes.</p>
     *
     * @param jc The {@link JobCard} whose parts list should be persisted.
     */
    public void saveJobParts(JobCard jc) {
        String deleteSql = "DELETE FROM job_parts WHERE job_id = ?";
        String insertSql = "INSERT INTO job_parts (job_id, part_id, quantity) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Begin transaction
            try {
                // Step 1: Remove all existing parts for this job
                try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                    del.setInt(1, jc.getJobId());
                    del.executeUpdate();
                }
                // Step 2: Re-insert all parts currently on the job card
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    for (JobCard.PartLineItem item : jc.getPartsUsed()) {
                        ins.setInt(1, jc.getJobId());
                        ins.setInt(2, item.getPart().getPartId());
                        ins.setInt(3, item.getQuantity());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
                conn.commit(); // Commit transaction
            } catch (SQLException e) {
                conn.rollback(); // Rollback on failure
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("  [!] Error saving job parts to database: " + e.getMessage());
        }
    }

    // ─── Query: All ───────────────────────────────────────────

    /**
     * Re-hydrates and returns every job card from the database.
     * Each job card is fully reconstructed with its linked Customer,
     * Technician, and list of {@link business.JobCard.PartLineItem} objects.
     *
     * @return A {@link List} of fully populated {@link JobCard} objects.
     */
    public List<JobCard> getAllJobCards() {
        List<JobCard> jobs = new ArrayList<>();
        String sql = "SELECT * FROM job_cards ORDER BY job_id";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                JobCard jc = buildJobCard(rs);
                if (jc != null) jobs.add(jc);
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error fetching job cards from database: " + e.getMessage());
        }
        return jobs;
    }

    // ─── Query: By ID ─────────────────────────────────────────

    /**
     * Re-hydrates a single job card by its primary key.
     *
     * @param id The {@code job_id} to search for.
     * @return A fully populated {@link JobCard}, or {@code null} if not found.
     */
    public JobCard getJobCardById(int id) {
        String sql = "SELECT * FROM job_cards WHERE job_id = ?";

        try (Connection conn  = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return buildJobCard(rs);
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error fetching job card by ID: " + e.getMessage());
        }
        return null;
    }

    // ─── Private: Re-hydration Helpers ────────────────────────

    /**
     * Constructs a fully re-hydrated {@link JobCard} from the current
     * row of a {@link ResultSet}.  Uses the re-hydration setters on
     * {@link JobCard} to restore persisted state without triggering
     * state-machine transitions or inventory side-effects.
     */
    private JobCard buildJobCard(ResultSet rs) throws SQLException {
        int    jobId       = rs.getInt   ("job_id");
        int    customerId  = rs.getInt   ("customer_id");
        int    techIdRaw   = rs.getInt   ("tech_id");
        boolean hasTech    = !rs.wasNull();           // wasNull() reflects the LAST column read
        String device      = rs.getString("device_details");
        String serial      = rs.getString("serial_no");
        String complaint   = rs.getString("complaint");
        String diagnosis   = rs.getString("diagnosis");
        String statusStr   = rs.getString("status");
        double laborCharge = rs.getDouble("labor_charge");

        // Re-link the Customer object (required — job cannot exist without one)
        Customer customer = customerDAO.getCustomerById(customerId);
        if (customer == null) {
            System.out.println("  [!] Data integrity warning: no customer found for job #" + jobId);
            return null;
        }

        // Construct via the normal constructor (sets status = INTAKE, calls customer.addJob())
        // jobId is the real DB id, so customer.addJob(jobId) is accurate.
        JobCard jc = new JobCard(jobId, customer, device, serial, complaint);

        // ── Restore persisted fields (bypassing state-machine validation) ──
        jc.setStatus     (JobCard.RepairStatus.valueOf(statusStr));
        jc.setDiagnosis  (diagnosis);
        jc.setLaborCharge(laborCharge);

        // Re-link the Technician if one was assigned
        if (hasTech) {
            Technician tech = technicianDAO.getTechnicianById(techIdRaw);
            if (tech != null) jc.setAssignedTech(tech);
        }

        // Re-hydrate the parts list (no stock deduction — stock is already correct in DB)
        loadPartsForJob(jc, jobId);

        return jc;
    }

    /**
     * Loads all {@code job_parts} rows for the given job and injects them
     * directly into the job card's parts list via {@link JobCard#addPartLineItem}.
     * Parts are re-linked to their live {@link Part} objects from the inventory table.
     */
    private void loadPartsForJob(JobCard jc, int jobId) {
        String sql = "SELECT jp.quantity, i.part_id, i.part_name, i.stock_quantity, i.price " +
                     "FROM job_parts jp " +
                     "JOIN inventory i ON jp.part_id = i.part_id " +
                     "WHERE jp.job_id = ?";

        try (Connection conn  = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, jobId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Part part = new Part(
                        rs.getInt   ("part_id"),
                        rs.getString("part_name"),
                        rs.getInt   ("stock_quantity"),
                        rs.getDouble("price")
                );
                // Inject the PartLineItem directly — avoids calling addPart() which would deduct stock
                jc.addPartLineItem(new JobCard.PartLineItem(part, rs.getInt("quantity")));
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error loading parts for job #" + jobId + ": " + e.getMessage());
        }
    }
}
