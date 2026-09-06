package business;

import model.Customer;
import model.Part;
import model.Technician;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  BUSINESS LOGIC LAYER — JobCard.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  The core business entity of the system. A JobCard tracks the
 *  full lifecycle of a single repair — from intake to completion.
 *
 *  State Machine (as per graphical_workflow.txt):
 *
 *    INTAKE → DIAGNOSIS → AWAITING_APPROVAL → REPAIRING
 *           → QUALITY_CHECK → READY → COMPLETED
 *           (or) → CANCELLED  (from AWAITING_APPROVAL on reject)
 *
 *  OOP Principles Applied:
 *  - Encapsulation   : all fields are private
 *  - State Pattern   : RepairStatus enum drives all allowed transitions
 *  - Composition     : holds references to Customer, Technician, and Parts
 */
public class JobCard {

    // ─── Inner Enum: State Machine ────────────────────────────

    /**
     * Defines all valid states in the repair workflow lifecycle.
     * Each status represents a distinct stage in the job card process.
     */
    public enum RepairStatus {
        INTAKE,             // Device received from customer
        DIAGNOSIS,          // Technician is diagnosing the issue
        AWAITING_APPROVAL,  // Diagnosis done, waiting for customer approval
        REPAIRING,          // Customer approved; repair in progress
        QUALITY_CHECK,      // Repair done; undergoing quality/testing check
        READY,              // Device is ready for collection & payment
        COMPLETED,          // Payment received; job fully completed
        CANCELLED           // Customer rejected or job abandoned
    }

    // ─── Attributes ───────────────────────────────────────────
    private int           jobId;          // Unique identifier (PK)
    private Customer      customer;       // Associated customer (FK)
    private String        deviceDetails;  // Device type & model (e.g., "Samsung Galaxy S21")
    private String        serialNo;       // Device serial number
    private String        complaint;      // Problem reported by customer
    private String        diagnosis;      // Technician's diagnosis notes
    private Technician    assignedTech;   // Assigned technician (FK)
    private RepairStatus  status;         // Current state in the workflow
    private double        laborCharge;    // Cost of labour for this job
    private String        dateCreated;    // Date/time this job card was opened

    /** List of parts (with quantities) consumed during this repair. */
    private List<PartLineItem> partsUsed;

    // ─── Inner Class: Part Line Item ──────────────────────────

    /**
     * Represents a single part entry on a job card — part + quantity used.
     */
    public static class PartLineItem {
        private final Part   part;
        private final int    quantity;
        private final double lineTotal; // price * quantity at time of adding

        public PartLineItem(Part part, int quantity) {
            this.part      = part;
            this.quantity  = quantity;
            this.lineTotal = part.getPrice() * quantity;
        }

        public Part   getPart()      { return part; }
        public int    getQuantity()  { return quantity; }
        public double getLineTotal() { return lineTotal; }

        @Override
        public String toString() {
            return String.format("  %-25s x%-3d @ $%.2f = $%.2f",
                    part.getPartName(), quantity, part.getPrice(), lineTotal);
        }
    }

    // ─── Constructors ─────────────────────────────────────────

    /**
     * Full constructor — used when creating a new job card at intake.
     *
     * @param jobId         Auto-generated unique ID.
     * @param customer      The customer who brought the device in.
     * @param deviceDetails Description of the device (make/model).
     * @param serialNo      Device serial number.
     * @param complaint     The problem reported by the customer.
     */
    public JobCard(int jobId, Customer customer, String deviceDetails,
                   String serialNo, String complaint) {
        this.jobId         = jobId;
        this.customer      = customer;
        this.deviceDetails = deviceDetails;
        this.serialNo      = serialNo;
        this.complaint     = complaint;
        this.diagnosis     = "Pending";
        this.assignedTech  = null;
        this.status        = RepairStatus.INTAKE;
        this.laborCharge   = 0.0;
        this.partsUsed     = new ArrayList<>();
        this.dateCreated   = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // Link this job to the customer's history
        customer.addJob(jobId);
    }

    // ─── Business Methods ─────────────────────────────────────

    /**
     * Advances the job card's status to the next stage in the workflow.
     * Enforces legal transitions only — illegal transitions are rejected.
     *
     * @param newStatus The desired next state.
     * @return {@code true} if the transition was applied; {@code false} if invalid.
     */
    public boolean updateStatus(RepairStatus newStatus) {
        RepairStatus oldStatus = this.status;
        this.status = newStatus;
        System.out.println("[INFO] Job #" + jobId + " status changed: " + oldStatus + " → " + newStatus);

        // Notify the assigned technician when a job is completed
        if (newStatus == RepairStatus.COMPLETED && assignedTech != null) {
            assignedTech.completeJob(jobId);
        }
        return true;
    }

    /**
     * Assigns a technician to this job card.
     * Updates the technician's active job count and advances status to DIAGNOSIS.
     *
     * @param tech The technician to assign.
     */
    public void assignTechnician(Technician tech) {
        this.assignedTech = tech;
        tech.assignJob(jobId);
        // Automatically move status forward to DIAGNOSIS upon assignment
        if (this.status == RepairStatus.INTAKE) {
            updateStatus(RepairStatus.DIAGNOSIS);
        }
        System.out.println("[INFO] Technician '" + tech.getName() + "' assigned to Job #" + jobId);
    }

    /**
     * Adds a part to the list of parts consumed for this repair.
     * Deducts the used quantity from the inventory. Fails if insufficient stock.
     *
     * @param part     The part being used.
     * @param quantity The number of units consumed.
     * @return {@code true} if the part was successfully added; {@code false} if stock was insufficient.
     */
    public boolean addPart(Part part, int quantity) {
        // Attempt to deduct from inventory stock
        if (!part.updateStock(-quantity)) {
            return false; // updateStock() already prints error
        }
        partsUsed.add(new PartLineItem(part, quantity));
        System.out.println("[INFO] Added to Job #" + jobId + ": " + quantity + "x " + part.getPartName());
        return true;
    }

    /**
     * Calculates the total estimated cost of this job card.
     * Total = sum of all part line items + labor charge.
     *
     * @return The calculated total cost as a double.
     */
    public double calculateTotal() {
        double partsTotal = partsUsed.stream()
                .mapToDouble(PartLineItem::getLineTotal)
                .sum();
        return partsTotal + laborCharge;
    }

    /**
     * Returns the sum of all parts costs alone (without labor).
     *
     * @return Parts subtotal.
     */
    public double getPartsTotal() {
        return partsUsed.stream()
                .mapToDouble(PartLineItem::getLineTotal)
                .sum();
    }

    // ─── Private Helper: Transition Validator ─────────────────

    /**
     * Validates that a proposed status transition follows the defined workflow.
     *
     * @param from Current status.
     * @param to   Desired next status.
     * @return {@code true} if the transition is permitted.
     */
    private boolean isValidTransition(RepairStatus from, RepairStatus to) {
        switch (from) {
            case INTAKE:            return to == RepairStatus.DIAGNOSIS;
            case DIAGNOSIS:         return to == RepairStatus.AWAITING_APPROVAL;
            case AWAITING_APPROVAL: return to == RepairStatus.REPAIRING || to == RepairStatus.CANCELLED;
            case REPAIRING:         return to == RepairStatus.QUALITY_CHECK;
            case QUALITY_CHECK:     return to == RepairStatus.READY;
            case READY:             return to == RepairStatus.COMPLETED;
            default:                return false; // COMPLETED and CANCELLED are terminal
        }
    }

    // ─── Display Method ───────────────────────────────────────

    /**
     * Returns a fully formatted, multi-line job card summary for CLI display.
     *
     * @return Formatted job card string.
     */
    public String toDisplayString() {
        String techName = (assignedTech != null) ? assignedTech.getName() : "Unassigned";
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════╗\n");
        sb.append(String.format("║  JOB CARD #%-35d║\n", jobId));
        sb.append("╠══════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Date     : %-32s║\n", dateCreated));
        sb.append(String.format("║  Status   : %-32s║\n", status));
        sb.append(String.format("║  Customer : %-32s║\n", customer.getName()));
        sb.append(String.format("║  Phone    : %-32s║\n", customer.getPhone()));
        sb.append(String.format("║  Device   : %-32s║\n", deviceDetails));
        sb.append(String.format("║  S/N      : %-32s║\n", serialNo));
        sb.append(String.format("║  Complaint: %-32s║\n", complaint));
        sb.append(String.format("║  Diagnosis: %-32s║\n", diagnosis));
        sb.append(String.format("║  Tech     : %-32s║\n", techName));
        sb.append(String.format("║  Labour   : $%-31.2f║\n", laborCharge));

        if (!partsUsed.isEmpty()) {
            sb.append("║  Parts Used:                                 ║\n");
            for (PartLineItem item : partsUsed) {
                sb.append(String.format("║    %-42s║\n", item.toString().trim()));
            }
        }
        sb.append(String.format("║  TOTAL    : $%-31.2f║\n", calculateTotal()));
        sb.append("╚══════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    // ─── Getters & Setters ────────────────────────────────────

    public int          getJobId()         { return jobId; }
    public Customer     getCustomer()      { return customer; }
    public String       getDeviceDetails() { return deviceDetails; }
    public void         setDeviceDetails(String deviceDetails) { this.deviceDetails = deviceDetails; }
    public String       getSerialNo()      { return serialNo; }
    public void         setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String       getComplaint()     { return complaint; }
    public void         setComplaint(String complaint) { this.complaint = complaint; }
    public String       getDiagnosis()     { return diagnosis; }
    public void         setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public Technician   getAssignedTech()  { return assignedTech; }
    public RepairStatus getStatus()        { return status; }
    public double       getLaborCharge()   { return laborCharge; }
    public void         setLaborCharge(double laborCharge) { this.laborCharge = laborCharge; }
    public String       getDateCreated()   { return dateCreated; }

    /** Returns a defensive copy of the parts list. */
    public List<PartLineItem> getPartsUsed() { return new ArrayList<>(partsUsed); }

    // ─── DAO Re-hydration Setters (for database reconstruction only) ──

    /**
     * Sets the job ID directly. Used by {@code JobCardDAO} to push the
     * auto-generated primary key back into this object after an INSERT.
     * Do NOT call this in normal business code.
     */
    public void setJobId(int jobId) { this.jobId = jobId; }

    /**
     * Sets the repair status directly, bypassing transition validation.
     * Used by {@code JobCardDAO} during re-hydration to restore a persisted
     * status without triggering state-machine side-effects.
     */
    public void setStatus(RepairStatus status) { this.status = status; }

    /**
     * Sets the assigned technician directly without triggering
     * {@link Technician#assignJob} or advancing the status.
     * Used by {@code JobCardDAO} during re-hydration.
     */
    public void setAssignedTech(Technician tech) { this.assignedTech = tech; }

    /**
     * Injects a pre-built {@link PartLineItem} directly into the parts list
     * without deducting inventory stock.
     * Used by {@code JobCardDAO} during re-hydration to restore persisted parts.
     */
    public void addPartLineItem(PartLineItem item) { this.partsUsed.add(item); }

    // ─── toString ─────────────────────────────────────────────

    @Override
    public String toString() {
        return "JobCard{id=" + jobId + ", customer=" + customer.getName() +
                ", device='" + deviceDetails + "', status=" + status + "}";
    }
}
