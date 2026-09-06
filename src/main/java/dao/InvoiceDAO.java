package dao;

import business.Invoice;
import business.JobCard;
import database.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  DATA ACCESS LAYER — InvoiceDAO.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  Provides all SQLite persistence operations for the
 *  {@code invoices} table. Replaces the in-memory {@code invoices}
 *  ArrayList that previously lived in Main.java.
 *
 *  Dependencies (passed in constructor for re-hydration):
 *  - {@link JobCardDAO} — re-hydrates the linked JobCard (including its
 *    Customer, Technician, and PartLineItems) needed to reconstruct
 *    the Invoice object.
 *
 *  Supported operations:
 *  - addInvoice         : INSERT a new invoice; sets auto-generated invoice_id.
 *  - updateInvoice      : UPDATE payment_status and grand_total after payment.
 *  - getAllInvoices      : Full re-hydration of all invoices from the database.
 *  - getInvoiceById     : Re-hydrate a single invoice by primary key.
 *  - getInvoiceByJobId  : Look up the invoice for a specific job card.
 *  - existsForJob       : Boolean guard — prevents duplicate invoice generation.
 *
 *  Re-hydration strategy:
 *  {@link Invoice} stores a snapshot of {@code laborCharge} and
 *  {@code partsTotal} taken from the job card at creation time.
 *  Because the job card object is fully re-hydrated by {@link JobCardDAO},
 *  the {@code Invoice(invoiceId, jobCard, discount)} constructor will
 *  recompute these correctly from the restored job card state.
 *  The {@code paymentStatus} is then restored using the DAO re-hydration
 *  setter {@link Invoice#setPaymentStatus}.
 */
public class InvoiceDAO {

    // ─── Dependency ───────────────────────────────────────────
    private final JobCardDAO jobCardDAO;

    /**
     * Creates a new {@code InvoiceDAO} with its required dependency.
     *
     * @param jobCardDAO DAO used to re-hydrate the linked JobCard object.
     */
    public InvoiceDAO(JobCardDAO jobCardDAO) {
        this.jobCardDAO = jobCardDAO;
    }

    // ─── Insert ───────────────────────────────────────────────

    /**
     * Persists a new invoice to the {@code invoices} table.
     * After a successful insert the auto-generated {@code invoice_id}
     * is written back via {@link Invoice#setInvoiceId}.
     *
     * @param invoice The {@link Invoice} to persist (pass with id = 0).
     */
    public void addInvoice(Invoice invoice) {
        String sql = "INSERT INTO invoices (job_id, discount, grand_total, payment_status) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt   (1, invoice.getJobCard().getJobId());
            pstmt.setDouble(2, invoice.getDiscount());
            pstmt.setDouble(3, invoice.getGrandTotal());
            pstmt.setString(4, invoice.getPaymentStatus().name());
            pstmt.executeUpdate();

            // Capture the auto-generated primary key and push it back into the object
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                invoice.setInvoiceId(rs.getInt(1));
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error saving invoice to database: " + e.getMessage());
        }
    }

    // ─── Update ───────────────────────────────────────────────

    /**
     * Persists the payment status and grand total of an existing invoice.
     * Call this immediately after {@link Invoice#processPayment} to ensure
     * the payment is durable across restarts.
     *
     * @param invoice The {@link Invoice} whose current state should be written to DB.
     */
    public void updateInvoice(Invoice invoice) {
        String sql = "UPDATE invoices SET payment_status = ?, grand_total = ? WHERE invoice_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, invoice.getPaymentStatus().name());
            pstmt.setDouble(2, invoice.getGrandTotal());
            pstmt.setInt   (3, invoice.getInvoiceId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("  [!] Error updating invoice in database: " + e.getMessage());
        }
    }

    // ─── Query: All ───────────────────────────────────────────

    /**
     * Re-hydrates and returns every invoice from the database.
     * Each invoice is fully reconstructed including its linked {@link JobCard}.
     *
     * @return A {@link List} of fully populated {@link Invoice} objects.
     */
    public List<Invoice> getAllInvoices() {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices ORDER BY invoice_id";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Invoice inv = buildInvoice(rs);
                if (inv != null) invoices.add(inv);
            }

        } catch (SQLException e) {
            System.out.println("  [!] Error fetching invoices from database: " + e.getMessage());
        }
        return invoices;
    }

    // ─── Query: By Invoice ID ─────────────────────────────────

    /**
     * Re-hydrates a single invoice by its primary key.
     *
     * @param id The {@code invoice_id} to search for.
     * @return A fully populated {@link Invoice}, or {@code null} if not found.
     */
    public Invoice getInvoiceById(int id) {
        String sql = "SELECT * FROM invoices WHERE invoice_id = ?";
        return queryInvoice(sql, id);
    }

    // ─── Query: By Job ID ─────────────────────────────────────

    /**
     * Re-hydrates the invoice associated with a given job card.
     * Used to display an existing invoice when a duplicate generation
     * is attempted.
     *
     * @param jobId The {@code job_id} to look up the invoice for.
     * @return The matching {@link Invoice}, or {@code null} if none exists.
     */
    public Invoice getInvoiceByJobId(int jobId) {
        String sql = "SELECT * FROM invoices WHERE job_id = ?";
        return queryInvoice(sql, jobId);
    }

    // ─── Guard: Duplicate Check ───────────────────────────────

    /**
     * Checks whether an invoice already exists for the given job card ID.
     * Used as a guard in the generate-invoice flow to prevent duplicates.
     *
     * @param jobId The {@code job_id} to check.
     * @return {@code true} if an invoice already exists for this job.
     */
    public boolean existsForJob(int jobId) {
        String sql = "SELECT COUNT(*) FROM invoices WHERE job_id = ?";

        try (Connection conn  = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, jobId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.out.println("  [!] Error checking invoice existence: " + e.getMessage());
        }
        return false;
    }

    // ─── Private: Re-hydration Helpers ────────────────────────

    /**
     * Shared helper — runs a parameterised SELECT that returns a single invoice row.
     */
    private Invoice queryInvoice(String sql, int param) {
        try (Connection conn  = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, param);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return buildInvoice(rs);

        } catch (SQLException e) {
            System.out.println("  [!] Error fetching invoice: " + e.getMessage());
        }
        return null;
    }

    /**
     * Constructs a fully re-hydrated {@link Invoice} from the current row
     * of a {@link ResultSet}.
     *
     * <p>The linked {@link JobCard} is re-hydrated via {@link JobCardDAO}.
     * The {@code paymentStatus} is restored using the DAO re-hydration setter
     * {@link Invoice#setPaymentStatus} so it reflects the persisted value
     * rather than the default {@code PENDING}.</p>
     */
    private Invoice buildInvoice(ResultSet rs) throws SQLException {
        int    invoiceId  = rs.getInt   ("invoice_id");
        int    jobId      = rs.getInt   ("job_id");
        double discount   = rs.getDouble("discount");
        String statusStr  = rs.getString("payment_status");

        // Re-hydrate the full job card (Customer + Technician + Parts)
        JobCard jc = jobCardDAO.getJobCardById(jobId);
        if (jc == null) {
            System.out.println("  [!] Data integrity warning: no job card found for invoice #" + invoiceId);
            return null;
        }

        // Construct the invoice — this recalculates grand_total from the restored job card
        Invoice invoice = new Invoice(invoiceId, jc, discount);

        // Restore the persisted payment status (overrides the default PENDING)
        invoice.setPaymentStatus(Invoice.PaymentStatus.valueOf(statusStr));

        return invoice;
    }
}
