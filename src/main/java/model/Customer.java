package model;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  ENTITY LAYER — Customer.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  Represents a customer registered in the repair shop system.
 *  Stores personal contact info and maintains a history of
 *  all job card IDs associated with this customer.
 *
 *  OOP Principles Applied:
 *  - Encapsulation : all fields are private; access via getters/setters
 *  - Abstraction   : internal job-history list is managed internally
 */
public class Customer {

    // ─── Attributes ───────────────────────────────────────────
    private int    customerId;   // Unique identifier (PK)
    private String name;         // Full name of the customer
    private String phone;        // Contact phone number
    private int    totalJobs;    // Running count of total jobs created

    /** Internal list of Job IDs associated with this customer (repair history). */
    private List<Integer> jobHistory;

    // ─── Constructors ─────────────────────────────────────────

    /**
     * Full constructor — used when creating a new customer record.
     *
     * @param customerId Auto-generated unique ID.
     * @param name       Full name of the customer.
     * @param phone      Contact phone number.
     */
    public Customer(int customerId, String name, String phone) {
        this.customerId = customerId;
        this.name       = name;
        this.phone      = phone;
        this.totalJobs  = 0;
        this.jobHistory = new ArrayList<>();
    }

    // ─── Business Methods ─────────────────────────────────────

    /**
     * Adds a new job card ID to this customer's repair history
     * and increments the total job counter.
     *
     * @param jobId The unique ID of the newly created job card.
     */
    public void addJob(int jobId) {
        jobHistory.add(jobId);
        totalJobs++;
    }

    /**
     * Returns a formatted string of this customer's complete job history.
     * Displays all job IDs associated with this customer.
     *
     * @return Formatted history string, or a "no history" message if empty.
     */
    public String getHistory() {
        if (jobHistory.isEmpty()) {
            return "No repair history found for " + name + ".";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Repair History for ").append(name).append(":\n");
        for (int i = 0; i < jobHistory.size(); i++) {
            sb.append("  ").append(i + 1).append(". Job ID #").append(jobHistory.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns a concise one-line display string for list/table views.
     *
     * @return Formatted display string.
     */
    public String toDisplayString() {
        return String.format("| ID: %-4d | Name: %-20s | Phone: %-15s | Total Jobs: %d |",
                customerId, name, phone, totalJobs);
    }

    // ─── Getters & Setters ────────────────────────────────────

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getTotalJobs() { return totalJobs; }
    public void setTotalJobs(int totalJobs) { this.totalJobs = totalJobs; }

    /** Returns a defensive copy to protect internal list integrity. */
    public List<Integer> getJobHistory() { return new ArrayList<>(jobHistory); }

    // ─── toString ─────────────────────────────────────────────

    @Override
    public String toString() {
        return "Customer{id=" + customerId + ", name='" + name + "', phone='" + phone +
                "', totalJobs=" + totalJobs + "}";
    }
}
