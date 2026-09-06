package business;

/**
 * ============================================================
 *  BUSINESS LOGIC LAYER — Invoice.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  Generated at the end of a repair cycle (when a JobCard reaches
 *  READY status). Calculates the grand total including discounts,
 *  handles payment processing, and prints a formatted receipt.
 *
 *  OOP Principles Applied:
 *  - Encapsulation  : all fields are private; access via getters/setters
 *  - Composition    : holds a reference to the completed JobCard
 *  - Single Responsibility : only handles billing and payment
 */
public class Invoice {

    // ─── Inner Enum: Payment Status ───────────────────────────

    /**
     * Represents the current payment state of this invoice.
     */
    public enum PaymentStatus {
        PENDING,    // Invoice generated; payment not yet received
        PAID_CASH,  // Paid in full with cash
        PAID_CARD   // Paid in full by card
    }

    // ─── Attributes ───────────────────────────────────────────
    private int           invoiceId;     // Unique identifier (PK)
    private JobCard       jobCard;       // Associated job card (FK)
    private double        laborCharge;   // Labour cost pulled from job card
    private double        partsTotal;    // Parts cost pulled from job card
    private double        discount;      // Flat discount amount in currency units
    private double        grandTotal;    // Final amount after discount
    private PaymentStatus paymentStatus; // Current payment state

    // ─── Constructors ─────────────────────────────────────────

    /**
     * Creates a new invoice for a completed job card with a given discount.
     * Grand total is automatically calculated on construction.
     *
     * @param invoiceId The auto-generated unique invoice ID.
     * @param jobCard   The completed job card this invoice belongs to.
     * @param discount  A flat discount amount to apply (e.g., 50.00 for $50 off).
     */
    public Invoice(int invoiceId, JobCard jobCard, double discount) {
        this.invoiceId     = invoiceId;
        this.jobCard       = jobCard;
        this.laborCharge   = jobCard.getLaborCharge();
        this.partsTotal    = jobCard.getPartsTotal();
        this.discount      = Math.max(0, discount); // Ensure non-negative discount
        this.paymentStatus = PaymentStatus.PENDING;
        this.grandTotal    = generateInvoice();     // Calculate on creation
    }

    // ─── Business Methods ─────────────────────────────────────

    /**
     * Calculates and stores the grand total for this invoice.
     * Formula: grandTotal = laborCharge + partsTotal - discount
     * Grand total cannot go below zero.
     *
     * @return The computed grand total.
     */
    public double generateInvoice() {
        double subtotal = laborCharge + partsTotal;
        double computed = subtotal - discount;
        this.grandTotal = Math.max(0, computed); // Can't be negative
        return this.grandTotal;
    }

    /**
     * Marks the invoice as paid and updates the job card status to COMPLETED.
     * Prints a confirmation message. Rejects if already paid.
     *
     * @param method The payment method used (e.g., PaymentStatus.PAID_CASH).
     * @return {@code true} if payment was processed successfully.
     */
    public boolean processPayment(PaymentStatus method) {
        // Guard: Can't pay if already paid
        if (paymentStatus == PaymentStatus.PAID_CASH || paymentStatus == PaymentStatus.PAID_CARD) {
            System.out.println("[WARN] Invoice #" + invoiceId + " has already been paid.");
            return false;
        }
        // Guard: Only accept valid payment methods
        if (method == PaymentStatus.PENDING) {
            System.out.println("[ERROR] Invalid payment method. Choose PAID_CASH or PAID_CARD.");
            return false;
        }

        this.paymentStatus = method;
        System.out.println("[INFO] Payment of $" + String.format("%.2f", grandTotal) +
                " received for Invoice #" + invoiceId + " via " + method);

        // Advance the job card to COMPLETED state
        jobCard.updateStatus(JobCard.RepairStatus.COMPLETED);
        return true;
    }

    /**
     * Prints a fully formatted invoice receipt to the console.
     * This is the customer-facing billing summary.
     */
    public void printInvoice() {
        String border = "═══════════════════════════════════════════════════";
        System.out.println("\n╔" + border + "╗");
        System.out.println("║          REPAIR SHOP — INVOICE RECEIPT           ║");
        System.out.println("╠" + border + "╣");
        System.out.printf ("║  Invoice #  : %-35d║%n", invoiceId);
        System.out.printf ("║  Job Card # : %-35d║%n", jobCard.getJobId());
        System.out.printf ("║  Customer   : %-35s║%n", jobCard.getCustomer().getName());
        System.out.printf ("║  Phone      : %-35s║%n", jobCard.getCustomer().getPhone());
        System.out.printf ("║  Device     : %-35s║%n", jobCard.getDeviceDetails());
        System.out.println("╠" + border + "╣");
        System.out.println("║                  BILLING SUMMARY                 ║");
        System.out.println("╠" + border + "╣");

        // Print each part line item
        if (!jobCard.getPartsUsed().isEmpty()) {
            System.out.println("║  Parts Used:                                      ║");
            for (JobCard.PartLineItem item : jobCard.getPartsUsed()) {
                System.out.printf("║    %-46s║%n", item.toString().trim());
            }
        }

        System.out.printf ("║  Parts Total : %33s║%n", String.format("$%.2f", partsTotal));
        System.out.printf ("║  Labour      : %33s║%n", String.format("$%.2f", laborCharge));
        System.out.printf ("║  Subtotal    : %33s║%n", String.format("$%.2f", laborCharge + partsTotal));
        System.out.printf ("║  Discount    : %33s║%n", String.format("-$%.2f", discount));
        System.out.println("╠" + border + "╣");
        System.out.printf ("║  GRAND TOTAL : %33s║%n", String.format("$%.2f", grandTotal));
        System.out.printf ("║  Payment     : %-35s║%n", paymentStatus);
        System.out.println("╚" + border + "╝\n");
    }

    // ─── Getters & Setters ────────────────────────────────────

    public int           getInvoiceId()     { return invoiceId; }
    public JobCard       getJobCard()       { return jobCard; }
    public double        getLaborCharge()   { return laborCharge; }
    public double        getPartsTotal()    { return partsTotal; }
    public double        getDiscount()      { return discount; }
    public void          setDiscount(double discount) {
        this.discount = Math.max(0, discount);
        generateInvoice(); // Recalculate on discount change
    }
    public double        getGrandTotal()    { return grandTotal; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    // ─── DAO Re-hydration Setters (for database reconstruction only) ──

    /**
     * Sets the invoice ID directly. Used by {@code InvoiceDAO} to push
     * the auto-generated primary key back into this object after an INSERT.
     * Do NOT call this in normal business code.
     */
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    /**
     * Sets the payment status directly without triggering payment processing.
     * Used by {@code InvoiceDAO} during re-hydration to restore the
     * persisted status (e.g. PAID_CASH) without re-running business logic.
     */
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    // ─── toString ─────────────────────────────────────────────

    @Override
    public String toString() {
        return "Invoice{id=" + invoiceId + ", jobId=" + jobCard.getJobId() +
                ", grandTotal=" + String.format("%.2f", grandTotal) +
                ", status=" + paymentStatus + "}";
    }
}
