package model;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  ENTITY LAYER — Technician.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  Represents a repair technician on the shop's staff roster.
 *  Tracks their specialty and manages a list of actively
 *  assigned job card IDs.
 *
 *  OOP Principles Applied:
 *  - Encapsulation  : all fields are private; access via getters/setters
 *  - Cohesion       : class is focused solely on technician state
 */
public class Technician {

    // ─── Attributes ───────────────────────────────────────────
    private int    techId;           // Unique identifier (PK)
    private String name;             // Full name of the technician
    private String specialty;        // Area of expertise (e.g., "Mobile Phones", "Laptops")
    private int    activeJobsCount;  // Number of jobs currently assigned

    /** List of Job IDs currently assigned to this technician. */
    private List<Integer> activeJobs;

    // ─── Constructors ─────────────────────────────────────────

    /**
     * Full constructor — used when registering a new technician.
     *
     * @param techId    Auto-generated unique ID.
     * @param name      Full name.
     * @param specialty Area of technical expertise.
     */
    public Technician(int techId, String name, String specialty) {
        this.techId          = techId;
        this.name            = name;
        this.specialty       = specialty;
        this.activeJobsCount = 0;
        this.activeJobs      = new ArrayList<>();
    }

    // ─── Business Methods ─────────────────────────────────────

    /**
     * Assigns a new job card to this technician.
     * Adds the job ID to the active jobs list and increments the counter.
     *
     * @param jobId The unique ID of the job card being assigned.
     */
    public void assignJob(int jobId) {
        activeJobs.add(jobId);
        activeJobsCount++;
        System.out.println("[INFO] Job #" + jobId + " assigned to Technician: " + name);
    }

    /**
     * Marks a job as complete and removes it from this technician's active workload.
     * If the job is not found, a warning is printed.
     *
     * @param jobId The unique ID of the job card being completed.
     */
    public void completeJob(int jobId) {
        if (activeJobs.remove(Integer.valueOf(jobId))) {
            activeJobsCount--;
            System.out.println("[INFO] Job #" + jobId + " completed by Technician: " + name);
        } else {
            System.out.println("[WARN] Job #" + jobId + " not found in active jobs for Technician: " + name);
        }
    }

    /**
     * Returns a concise one-line display string for list/table views.
     *
     * @return Formatted display string.
     */
    public String toDisplayString() {
        return String.format("| ID: %-4d | Name: %-20s | Specialty: %-20s | Active Jobs: %d |",
                techId, name, specialty, activeJobsCount);
    }

    // ─── Getters & Setters ────────────────────────────────────

    public int getTechId() { return techId; }
    public void setTechId(int techId) { this.techId = techId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public int getActiveJobsCount() { return activeJobsCount; }
    public void setActiveJobsCount(int activeJobsCount) { this.activeJobsCount = activeJobsCount; }

    /** Returns a defensive copy to protect internal list integrity. */
    public List<Integer> getActiveJobs() { return new ArrayList<>(activeJobs); }

    // ─── toString ─────────────────────────────────────────────

    @Override
    public String toString() {
        return "Technician{id=" + techId + ", name='" + name + "', specialty='" + specialty +
                "', activeJobs=" + activeJobsCount + "}";
    }
}
