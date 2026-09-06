import business.Invoice;
import business.JobCard;
import model.Customer;
import model.Part;
import model.Technician;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * ============================================================
 *  PRESENTATION LAYER — Main.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  Entry point and CLI controller for the Repair Shop Job Card System.
 *  Manages all in-memory data collections (acting as a temporary database)
 *  and drives the full menu-based user interface.
 *
 *  ── Data Persistence Layer (In-Memory) ──────────────────────
 *  All data is stored in ArrayList collections during the session.
 *  These act as our in-memory database until a real DB is integrated.
 *
 *  ── Presentation Layer (CLI) ─────────────────────────────────
 *  A while loop + switch statement replicates the dashboard menu
 *  flow defined in system_modules.txt. Each menu module is
 *  handled by a dedicated private method for clean separation.
 *
 *  Menu Structure:
 *   [LOGIN] → [DASHBOARD]
 *                ├── 1. Customers
 *                ├── 2. Job Cards
 *                ├── 3. Inventory (Parts)
 *                ├── 4. Technicians
 *                ├── 5. Invoicing & Payments
 *                ├── 6. Reports
 *                └── 0. Logout
 */
public class Main {

    // ═══════════════════════════════════════════════════════════
    //  DATA PERSISTENCE LAYER — In-Memory Collections
    // ═══════════════════════════════════════════════════════════

    /** In-memory store for all registered customers. */
    private static List<Customer>   customers   = new ArrayList<>();

    /** In-memory store for all job cards (open and closed). */
    private static List<JobCard>    jobCards    = new ArrayList<>();

    /** In-memory store for all technician staff profiles. */
    private static List<Technician> technicians = new ArrayList<>();

    /** In-memory store for the parts/inventory catalogue. */
    private static List<Part>       inventory   = new ArrayList<>();

    /** In-memory store for all generated invoices. */
    private static List<Invoice>    invoices    = new ArrayList<>();

    // ─── ID Counters (simulate auto-increment PKs) ────────────
    private static int customerIdCounter   = 1001;
    private static int jobCardIdCounter    = 5001;
    private static int technicianIdCounter = 2001;
    private static int partIdCounter       = 3001;
    private static int invoiceIdCounter    = 4001;

    // ─── Shared Input Scanner ─────────────────────────────────
    private static Scanner scanner = new Scanner(System.in);

    // ─── Hardcoded Admin Credentials (placeholder for auth) ───
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "1234";

    // ═══════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═══════════════════════════════════════════════════════════

    public static void main(String[] args) {
        seedDemoData(); // Pre-populate with sample data for demonstration
        showBanner();

        // ── Authentication Loop ──────────────────────────────
        boolean loggedIn = false;
        while (!loggedIn) {
            loggedIn = showLoginScreen();
            if (!loggedIn) {
                System.out.println("\n  [!] Invalid credentials. Please try again.");
            }
        }

        // ── Dashboard Loop ───────────────────────────────────
        boolean running = true;
        while (running) {
            showDashboard();
            int choice = readInt("  Enter your choice: ");
            switch (choice) {
                case 1: moduleCustomers();    break;
                case 2: moduleJobCards();     break;
                case 3: moduleInventory();    break;
                case 4: moduleTechnicians();  break;
                case 5: moduleInvoicing();    break;
                case 6: moduleReports();      break;
                case 0:
                    System.out.println("\n  Logging out... Goodbye!\n");
                    running = false;
                    break;
                default:
                    System.out.println("  [!] Invalid option. Please enter a number from 0-6.");
            }
        }
        scanner.close();
    }

    // ═══════════════════════════════════════════════════════════
    //  PRESENTATION: BANNER & LOGIN
    // ═══════════════════════════════════════════════════════════

    /** Prints the application startup banner. */
    private static void showBanner() {
        System.out.println("\n");
        System.out.println("  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║     ██████╗ ███████╗██████╗  █████╗ ██╗██████╗      ║");
        System.out.println("  ║     ██╔══██╗██╔════╝██╔══██╗██╔══██╗██║██╔══██╗     ║");
        System.out.println("  ║     ██████╔╝█████╗  ██████╔╝███████║██║██████╔╝     ║");
        System.out.println("  ║     ██╔══██╗██╔══╝  ██╔═══╝ ██╔══██║██║██╔══██╗     ║");
        System.out.println("  ║     ██║  ██║███████╗██║     ██║  ██║██║██║  ██║     ║");
        System.out.println("  ║     ╚═╝  ╚═╝╚══════╝╚═╝     ╚═╝  ╚═╝╚═╝╚═╝  ╚═╝   ║");
        System.out.println("  ╠══════════════════════════════════════════════════════╣");
        System.out.println("  ║       REPAIR SHOP JOB CARD MANAGEMENT SYSTEM        ║");
        System.out.println("  ║                    Version 1.0                      ║");
        System.out.println("  ╚══════════════════════════════════════════════════════╝\n");
    }

    /**
     * Presents the login screen and validates credentials against hardcoded admin values.
     *
     * @return {@code true} if credentials match; {@code false} otherwise.
     */
    private static boolean showLoginScreen() {
        System.out.println("  ┌──────────────────────────────┐");
        System.out.println("  │         USER LOGIN           │");
        System.out.println("  └──────────────────────────────┘");
        System.out.print("  Username: ");
        String user = scanner.nextLine().trim();
        System.out.print("  Password: ");
        String pass = scanner.nextLine().trim();
        return ADMIN_USER.equals(user) && ADMIN_PASS.equals(pass);
    }

    // ═══════════════════════════════════════════════════════════
    //  PRESENTATION: DASHBOARD
    // ═══════════════════════════════════════════════════════════

    /** Displays the main dashboard with a live overview of system stats. */
    private static void showDashboard() {
        long openJobs      = jobCards.stream().filter(j ->
                j.getStatus() != JobCard.RepairStatus.COMPLETED &&
                j.getStatus() != JobCard.RepairStatus.CANCELLED).count();
        long completedJobs = jobCards.stream().filter(j ->
                j.getStatus() == JobCard.RepairStatus.COMPLETED).count();
        long pendingPay    = invoices.stream().filter(i ->
                i.getPaymentStatus() == Invoice.PaymentStatus.PENDING).count();
        double dailyRev    = invoices.stream()
                .filter(i -> i.getPaymentStatus() != Invoice.PaymentStatus.PENDING)
                .mapToDouble(Invoice::getGrandTotal).sum();

        System.out.println("\n  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║                     DASHBOARD                       ║");
        System.out.println("  ╠══════════════════════════════════════════════════════╣");
        System.out.printf ("  ║   📋 Open Jobs      : %-30d║%n", openJobs);
        System.out.printf ("  ║   ✅ Completed Jobs  : %-30d║%n", completedJobs);
        System.out.printf ("  ║   ⏳ Pending Payments: %-30d║%n", pendingPay);
        System.out.printf ("  ║   💰 Total Revenue   : $%-29.2f║%n", dailyRev);
        System.out.printf ("  ║   👥 Customers       : %-30d║%n", customers.size());
        System.out.printf ("  ║   🔧 Technicians     : %-30d║%n", technicians.size());
        System.out.println("  ╠══════════════════════════════════════════════════════╣");
        System.out.println("  ║   1. Customers          4. Technicians              ║");
        System.out.println("  ║   2. Job Cards          5. Invoicing & Payments     ║");
        System.out.println("  ║   3. Inventory          6. Reports                  ║");
        System.out.println("  ║   0. Logout                                         ║");
        System.out.println("  ╚══════════════════════════════════════════════════════╝");
    }

    // ═══════════════════════════════════════════════════════════
    //  MODULE 1 — CUSTOMERS
    // ═══════════════════════════════════════════════════════════

    /** Customers sub-menu. */
    private static void moduleCustomers() {
        boolean back = false;
        while (!back) {
            System.out.println("\n  ┌─────────────────────────────┐");
            System.out.println("  │       CUSTOMER MODULE       │");
            System.out.println("  ├─────────────────────────────┤");
            System.out.println("  │  1. View All Customers      │");
            System.out.println("  │  2. Add New Customer        │");
            System.out.println("  │  3. View Customer Details   │");
            System.out.println("  │  0. Back to Dashboard       │");
            System.out.println("  └─────────────────────────────┘");
            int choice = readInt("  Choice: ");
            switch (choice) {
                case 1: viewAllCustomers(); break;
                case 2: addNewCustomer();   break;
                case 3: viewCustomerDetails(); break;
                case 0: back = true;        break;
                default: System.out.println("  [!] Invalid option.");
            }
        }
    }

    /** Prints the full list of registered customers. */
    private static void viewAllCustomers() {
        System.out.println("\n  ── ALL CUSTOMERS ─────────────────────────────────────");
        if (customers.isEmpty()) {
            System.out.println("  No customers registered yet.");
            return;
        }
        for (Customer c : customers) {
            System.out.println("  " + c.toDisplayString());
        }
    }

    /** Prompts the user to register a new customer. */
    private static void addNewCustomer() {
        System.out.println("\n  ── ADD NEW CUSTOMER ──────────────────────────────────");
        System.out.print("  Full Name : ");
        String name = scanner.nextLine().trim();
        System.out.print("  Phone No  : ");
        String phone = scanner.nextLine().trim();

        Customer newCustomer = new Customer(customerIdCounter++, name, phone);
        customers.add(newCustomer);
        System.out.println("  [✓] Customer registered successfully! ID: " + newCustomer.getCustomerId());
    }

    /** Displays the details and repair history of a specific customer. */
    private static void viewCustomerDetails() {
        System.out.println("\n  ── CUSTOMER DETAILS ──────────────────────────────────");
        int id = readInt("  Enter Customer ID: ");
        Customer c = findCustomerById(id);
        if (c == null) {
            System.out.println("  [!] Customer ID #" + id + " not found.");
            return;
        }
        System.out.println("  " + c.toDisplayString());
        System.out.println("  " + c.getHistory());
    }

    // ═══════════════════════════════════════════════════════════
    //  MODULE 2 — JOB CARDS
    // ═══════════════════════════════════════════════════════════

    /** Job Cards sub-menu. */
    private static void moduleJobCards() {
        boolean back = false;
        while (!back) {
            System.out.println("\n  ┌──────────────────────────────────┐");
            System.out.println("  │         JOB CARD MODULE          │");
            System.out.println("  ├──────────────────────────────────┤");
            System.out.println("  │  1. View All Job Cards           │");
            System.out.println("  │  2. Create New Job Card          │");
            System.out.println("  │  3. View Job Card Details        │");
            System.out.println("  │  4. Update Job Status            │");
            System.out.println("  │  5. Add Parts to Job             │");
            System.out.println("  │  0. Back to Dashboard            │");
            System.out.println("  └──────────────────────────────────┘");
            int choice = readInt("  Choice: ");
            switch (choice) {
                case 1: viewAllJobCards();   break;
                case 2: createNewJobCard();  break;
                case 3: viewJobCardDetails();break;
                case 4: updateJobStatus();   break;
                case 5: addPartsToJob();     break;
                case 0: back = true;         break;
                default: System.out.println("  [!] Invalid option.");
            }
        }
    }

    /** Prints a summary list of all job cards. */
    private static void viewAllJobCards() {
        System.out.println("\n  ── ALL JOB CARDS ─────────────────────────────────────");
        if (jobCards.isEmpty()) {
            System.out.println("  No job cards created yet.");
            return;
        }
        System.out.println("  " + String.format("%-8s %-22s %-20s %-20s", "Job ID", "Customer", "Device", "Status"));
        System.out.println("  " + "─".repeat(72));
        for (JobCard jc : jobCards) {
            System.out.printf("  %-8d %-22s %-20s %-20s%n",
                    jc.getJobId(),
                    jc.getCustomer().getName(),
                    jc.getDeviceDetails(),
                    jc.getStatus());
        }
    }

    /**
     * Walks the user through the job card creation workflow:
     * Select Customer → Enter Device → Record Complaint → Assign Technician
     */
    private static void createNewJobCard() {
        System.out.println("\n  ── CREATE NEW JOB CARD ────────────────────────────────");

        // STEP 1: Select Customer
        viewAllCustomers();
        int custId = readInt("\n  Enter Customer ID (or 0 to create new): ");
        Customer customer;
        if (custId == 0) {
            addNewCustomer();
            customer = customers.get(customers.size() - 1);
        } else {
            customer = findCustomerById(custId);
            if (customer == null) {
                System.out.println("  [!] Customer not found. Returning to menu.");
                return;
            }
        }

        // STEP 2: Enter Device Details
        System.out.print("  Device (Make/Model)  : ");
        String device = scanner.nextLine().trim();
        System.out.print("  Serial Number        : ");
        String serial = scanner.nextLine().trim();

        // STEP 3: Record Complaint
        System.out.print("  Customer Complaint   : ");
        String complaint = scanner.nextLine().trim();

        // STEP 4: Assign Technician
        Technician tech = null;
        if (!technicians.isEmpty()) {
            viewAllTechniciansList();
            int techId = readInt("  Assign Technician ID (or 0 to skip): ");
            if (techId != 0) {
                tech = findTechnicianById(techId);
                if (tech == null) {
                    System.out.println("  [!] Technician not found. Job will be unassigned.");
                }
            }
        } else {
            System.out.println("  [!] No technicians available. Job will be unassigned.");
        }

        // Create the JobCard
        JobCard newJob = new JobCard(jobCardIdCounter++, customer, device, serial, complaint);

        // Assign technician if selected
        if (tech != null) {
            newJob.assignTechnician(tech);
        }

        jobCards.add(newJob);
        System.out.println("\n  [✓] Job Card #" + newJob.getJobId() + " created successfully for " +
                customer.getName() + "!");
    }

    /** Displays the full formatted detail view for a single job card. */
    private static void viewJobCardDetails() {
        int id = readInt("  Enter Job Card ID: ");
        JobCard jc = findJobById(id);
        if (jc == null) {
            System.out.println("  [!] Job Card #" + id + " not found.");
            return;
        }
        System.out.println(jc.toDisplayString());
    }

    /**
     * Allows the user to manually advance a job card's status
     * through the defined state machine workflow.
     */
    private static void updateJobStatus() {
        System.out.println("\n  ── UPDATE JOB STATUS ─────────────────────────────────");
        int id = readInt("  Enter Job Card ID: ");
        JobCard jc = findJobById(id);
        if (jc == null) {
            System.out.println("  [!] Job Card #" + id + " not found.");
            return;
        }

        System.out.println("  Current Status: " + jc.getStatus());
        System.out.println("\n  Available Statuses:");
        System.out.println("  1. DIAGNOSIS         4. QUALITY_CHECK");
        System.out.println("  2. AWAITING_APPROVAL 5. READY");
        System.out.println("  3. REPAIRING         6. CANCELLED");

        // Special prompt: if awaiting approval, show approve/reject flow
        if (jc.getStatus() == JobCard.RepairStatus.AWAITING_APPROVAL) {
            System.out.println("\n  Customer Approval Required:");
            System.out.println("  1. APPROVE (→ REPAIRING)");
            System.out.println("  2. REJECT  (→ CANCELLED)");
            int decision = readInt("  Decision: ");
            if (decision == 1) {
                System.out.print("  Enter diagnosis notes: ");
                String diag = scanner.nextLine().trim();
                jc.setDiagnosis(diag);
                jc.updateStatus(JobCard.RepairStatus.REPAIRING);
            } else {
                jc.updateStatus(JobCard.RepairStatus.CANCELLED);
            }
            return;
        }

        // For other states, show next available status
        int statusChoice = readInt("  Select new status number: ");
        JobCard.RepairStatus[] statuses = {
            null, // 0 not used
            JobCard.RepairStatus.DIAGNOSIS,
            JobCard.RepairStatus.AWAITING_APPROVAL,
            JobCard.RepairStatus.REPAIRING,
            JobCard.RepairStatus.QUALITY_CHECK,
            JobCard.RepairStatus.READY,
            JobCard.RepairStatus.CANCELLED
        };

        if (statusChoice >= 1 && statusChoice <= 6) {
            // Prompt for diagnosis notes when advancing to AWAITING_APPROVAL
            if (statuses[statusChoice] == JobCard.RepairStatus.AWAITING_APPROVAL) {
                System.out.print("  Enter diagnosis notes: ");
                String diag = scanner.nextLine().trim();
                jc.setDiagnosis(diag);
            }
            // Prompt for labor charge when reaching QUALITY_CHECK
            if (statuses[statusChoice] == JobCard.RepairStatus.QUALITY_CHECK) {
                double labor = readDouble("  Enter Labour Charge ($): ");
                jc.setLaborCharge(labor);
            }
            jc.updateStatus(statuses[statusChoice]);
        } else {
            System.out.println("  [!] Invalid status selection.");
        }
    }

    /** Adds parts from the inventory to an active job card. */
    private static void addPartsToJob() {
        System.out.println("\n  ── ADD PARTS TO JOB ──────────────────────────────────");
        int jobId = readInt("  Enter Job Card ID: ");
        JobCard jc = findJobById(jobId);
        if (jc == null) {
            System.out.println("  [!] Job Card #" + jobId + " not found.");
            return;
        }

        viewAllInventoryList();
        int partId = readInt("  Enter Part ID to add: ");
        Part part = findPartById(partId);
        if (part == null) {
            System.out.println("  [!] Part not found.");
            return;
        }

        int qty = readInt("  Quantity to use: ");
        jc.addPart(part, qty);
    }

    // ═══════════════════════════════════════════════════════════
    //  MODULE 3 — INVENTORY (PARTS)
    // ═══════════════════════════════════════════════════════════

    /** Inventory sub-menu. */
    private static void moduleInventory() {
        boolean back = false;
        while (!back) {
            System.out.println("\n  ┌─────────────────────────────┐");
            System.out.println("  │       INVENTORY MODULE      │");
            System.out.println("  ├─────────────────────────────┤");
            System.out.println("  │  1. View All Parts          │");
            System.out.println("  │  2. Add New Part            │");
            System.out.println("  │  3. Restock a Part          │");
            System.out.println("  │  4. Low Stock Alerts        │");
            System.out.println("  │  0. Back to Dashboard       │");
            System.out.println("  └─────────────────────────────┘");
            int choice = readInt("  Choice: ");
            switch (choice) {
                case 1: viewAllInventoryList(); break;
                case 2: addNewPart();           break;
                case 3: restockPart();          break;
                case 4: showLowStockAlerts();   break;
                case 0: back = true;            break;
                default: System.out.println("  [!] Invalid option.");
            }
        }
    }

    /** Prints all parts in the inventory. */
    private static void viewAllInventoryList() {
        System.out.println("\n  ── INVENTORY / PARTS ─────────────────────────────────");
        if (inventory.isEmpty()) {
            System.out.println("  No parts in inventory.");
            return;
        }
        for (Part p : inventory) {
            System.out.println("  " + p.toDisplayString());
        }
    }

    /** Adds a new part to the inventory. */
    private static void addNewPart() {
        System.out.println("\n  ── ADD NEW PART ──────────────────────────────────────");
        System.out.print("  Part Name    : ");
        String name = scanner.nextLine().trim();
        int qty      = readInt("  Initial Stock: ");
        double price = readDouble("  Unit Price ($): ");

        Part newPart = new Part(partIdCounter++, name, qty, price);
        inventory.add(newPart);
        System.out.println("  [✓] Part '" + name + "' added to inventory with ID: " + newPart.getPartId());
    }

    /** Adds stock to an existing part. */
    private static void restockPart() {
        System.out.println("\n  ── RESTOCK PART ──────────────────────────────────────");
        viewAllInventoryList();
        int id = readInt("  Enter Part ID: ");
        Part part = findPartById(id);
        if (part == null) {
            System.out.println("  [!] Part not found.");
            return;
        }
        int qty = readInt("  Quantity to add: ");
        part.updateStock(qty);
    }

    /** Lists all parts that are below the low-stock threshold. */
    private static void showLowStockAlerts() {
        System.out.println("\n  ── LOW STOCK ALERTS ──────────────────────────────────");
        boolean found = false;
        for (Part p : inventory) {
            if (p.checkLowStock()) {
                System.out.println("  [!] " + p.toDisplayString());
                found = true;
            }
        }
        if (!found) {
            System.out.println("  [✓] All parts are sufficiently stocked.");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  MODULE 4 — TECHNICIANS
    // ═══════════════════════════════════════════════════════════

    /** Technicians sub-menu. */
    private static void moduleTechnicians() {
        boolean back = false;
        while (!back) {
            System.out.println("\n  ┌─────────────────────────────┐");
            System.out.println("  │     TECHNICIANS MODULE      │");
            System.out.println("  ├─────────────────────────────┤");
            System.out.println("  │  1. View All Technicians    │");
            System.out.println("  │  2. Add New Technician      │");
            System.out.println("  │  0. Back to Dashboard       │");
            System.out.println("  └─────────────────────────────┘");
            int choice = readInt("  Choice: ");
            switch (choice) {
                case 1: viewAllTechniciansList(); break;
                case 2: addNewTechnician();       break;
                case 0: back = true;              break;
                default: System.out.println("  [!] Invalid option.");
            }
        }
    }

    /** Prints the full technicians roster. */
    private static void viewAllTechniciansList() {
        System.out.println("\n  ── ALL TECHNICIANS ───────────────────────────────────");
        if (technicians.isEmpty()) {
            System.out.println("  No technicians registered yet.");
            return;
        }
        for (Technician t : technicians) {
            System.out.println("  " + t.toDisplayString());
        }
    }

    /** Registers a new technician in the system. */
    private static void addNewTechnician() {
        System.out.println("\n  ── ADD NEW TECHNICIAN ────────────────────────────────");
        System.out.print("  Full Name  : ");
        String name = scanner.nextLine().trim();
        System.out.print("  Specialty  : ");
        String specialty = scanner.nextLine().trim();

        Technician newTech = new Technician(technicianIdCounter++, name, specialty);
        technicians.add(newTech);
        System.out.println("  [✓] Technician '" + name + "' registered with ID: " + newTech.getTechId());
    }

    // ═══════════════════════════════════════════════════════════
    //  MODULE 5 — INVOICING & PAYMENTS
    // ═══════════════════════════════════════════════════════════

    /** Invoicing sub-menu. */
    private static void moduleInvoicing() {
        boolean back = false;
        while (!back) {
            System.out.println("\n  ┌─────────────────────────────────┐");
            System.out.println("  │    INVOICING & PAYMENTS MODULE  │");
            System.out.println("  ├─────────────────────────────────┤");
            System.out.println("  │  1. Generate Invoice for Job    │");
            System.out.println("  │  2. View All Invoices           │");
            System.out.println("  │  3. Process Payment             │");
            System.out.println("  │  0. Back to Dashboard           │");
            System.out.println("  └─────────────────────────────────┘");
            int choice = readInt("  Choice: ");
            switch (choice) {
                case 1: generateInvoice();  break;
                case 2: viewAllInvoices();  break;
                case 3: processPayment();   break;
                case 0: back = true;        break;
                default: System.out.println("  [!] Invalid option.");
            }
        }
    }

    /** Generates an invoice for a job card that is in READY status. */
    private static void generateInvoice() {
        System.out.println("\n  ── GENERATE INVOICE ──────────────────────────────────");
        int jobId = readInt("  Enter Job Card ID: ");
        JobCard jc = findJobById(jobId);
        if (jc == null) {
            System.out.println("  [!] Job Card not found.");
            return;
        }
        if (jc.getStatus() != JobCard.RepairStatus.READY) {
            System.out.println("  [!] Invoice can only be generated for jobs with status: READY");
            System.out.println("      Current status: " + jc.getStatus());
            return;
        }
        // Check if invoice already exists for this job
        for (Invoice inv : invoices) {
            if (inv.getJobCard().getJobId() == jobId) {
                System.out.println("  [!] Invoice already exists for Job #" + jobId + ". Use 'View All Invoices'.");
                inv.printInvoice();
                return;
            }
        }

        double discount = readDouble("  Apply Discount ($) [enter 0 for none]: ");
        Invoice invoice = new Invoice(invoiceIdCounter++, jc, discount);
        invoices.add(invoice);
        System.out.println("  [✓] Invoice #" + invoice.getInvoiceId() + " generated successfully!");
        invoice.printInvoice();
    }

    /** Prints a summary table of all invoices. */
    private static void viewAllInvoices() {
        System.out.println("\n  ── ALL INVOICES ──────────────────────────────────────");
        if (invoices.isEmpty()) {
            System.out.println("  No invoices generated yet.");
            return;
        }
        System.out.println("  " + String.format("%-12s %-10s %-22s %-12s %-15s",
                "Invoice ID", "Job ID", "Customer", "Total ($)", "Payment Status"));
        System.out.println("  " + "─".repeat(73));
        for (Invoice inv : invoices) {
            System.out.printf("  %-12d %-10d %-22s %-12.2f %-15s%n",
                    inv.getInvoiceId(),
                    inv.getJobCard().getJobId(),
                    inv.getJobCard().getCustomer().getName(),
                    inv.getGrandTotal(),
                    inv.getPaymentStatus());
        }
    }

    /** Processes payment for a pending invoice. */
    private static void processPayment() {
        System.out.println("\n  ── PROCESS PAYMENT ───────────────────────────────────");
        int invId = readInt("  Enter Invoice ID: ");
        Invoice invoice = findInvoiceById(invId);
        if (invoice == null) {
            System.out.println("  [!] Invoice #" + invId + " not found.");
            return;
        }

        System.out.println("  Invoice Total: $" + String.format("%.2f", invoice.getGrandTotal()));
        System.out.println("  Payment Method:");
        System.out.println("  1. Cash");
        System.out.println("  2. Card");
        int method = readInt("  Select method: ");

        Invoice.PaymentStatus payMethod = null;
        if (method == 1)      payMethod = Invoice.PaymentStatus.PAID_CASH;
        else if (method == 2) payMethod = Invoice.PaymentStatus.PAID_CARD;
        else {
            System.out.println("  [!] Invalid payment method selected.");
            return;
        }
        invoice.processPayment(payMethod);
        invoice.printInvoice();
    }

    // ═══════════════════════════════════════════════════════════
    //  MODULE 6 — REPORTS
    // ═══════════════════════════════════════════════════════════

    /** Reports module — shows aggregated system metrics. */
    private static void moduleReports() {
        System.out.println("\n  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║                  SYSTEM REPORTS                     ║");
        System.out.println("  ╠══════════════════════════════════════════════════════╣");

        // ── Job Status Breakdown ──
        int intake   = 0, diag  = 0, await = 0, repair = 0,
            quality  = 0, ready = 0, done  = 0, cancel = 0;
        for (JobCard jc : jobCards) {
            switch (jc.getStatus()) {
                case INTAKE:            intake++;  break;
                case DIAGNOSIS:         diag++;    break;
                case AWAITING_APPROVAL: await++;   break;
                case REPAIRING:         repair++;  break;
                case QUALITY_CHECK:     quality++; break;
                case READY:             ready++;   break;
                case COMPLETED:         done++;    break;
                case CANCELLED:         cancel++;  break;
            }
        }

        System.out.printf("  ║  Total Job Cards   : %-31d║%n", jobCards.size());
        System.out.printf("  ║  ├─ Intake         : %-31d║%n", intake);
        System.out.printf("  ║  ├─ Diagnosis      : %-31d║%n", diag);
        System.out.printf("  ║  ├─ Awaiting Approv: %-31d║%n", await);
        System.out.printf("  ║  ├─ Repairing      : %-31d║%n", repair);
        System.out.printf("  ║  ├─ Quality Check  : %-31d║%n", quality);
        System.out.printf("  ║  ├─ Ready          : %-31d║%n", ready);
        System.out.printf("  ║  ├─ Completed      : %-31d║%n", done);
        System.out.printf("  ║  └─ Cancelled      : %-31d║%n", cancel);
        System.out.println("  ╠══════════════════════════════════════════════════════╣");

        // ── Revenue ──
        double totalRevenue = invoices.stream()
                .filter(i -> i.getPaymentStatus() != Invoice.PaymentStatus.PENDING)
                .mapToDouble(Invoice::getGrandTotal).sum();
        double pendingRev   = invoices.stream()
                .filter(i -> i.getPaymentStatus() == Invoice.PaymentStatus.PENDING)
                .mapToDouble(Invoice::getGrandTotal).sum();

        System.out.printf("  ║  Total Revenue     : $%-30.2f║%n", totalRevenue);
        System.out.printf("  ║  Pending Payments  : $%-30.2f║%n", pendingRev);
        System.out.println("  ╠══════════════════════════════════════════════════════╣");
        System.out.printf("  ║  Customers Registered : %-28d║%n", customers.size());
        System.out.printf("  ║  Technicians on Staff : %-28d║%n", technicians.size());
        System.out.printf("  ║  Parts in Inventory   : %-28d║%n", inventory.size());
        System.out.println("  ╚══════════════════════════════════════════════════════╝\n");
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER: LOOKUP METHODS
    // ═══════════════════════════════════════════════════════════

    /** Finds a Customer by ID. Returns null if not found. */
    private static Customer findCustomerById(int id) {
        for (Customer c : customers) {
            if (c.getCustomerId() == id) return c;
        }
        return null;
    }

    /** Finds a JobCard by ID. Returns null if not found. */
    private static JobCard findJobById(int id) {
        for (JobCard jc : jobCards) {
            if (jc.getJobId() == id) return jc;
        }
        return null;
    }

    /** Finds a Technician by ID. Returns null if not found. */
    private static Technician findTechnicianById(int id) {
        for (Technician t : technicians) {
            if (t.getTechId() == id) return t;
        }
        return null;
    }

    /** Finds a Part by ID. Returns null if not found. */
    private static Part findPartById(int id) {
        for (Part p : inventory) {
            if (p.getPartId() == id) return p;
        }
        return null;
    }

    /** Finds an Invoice by ID. Returns null if not found. */
    private static Invoice findInvoiceById(int id) {
        for (Invoice inv : invoices) {
            if (inv.getInvoiceId() == id) return inv;
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER: INPUT METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Reads an integer from stdin. Loops until a valid integer is entered.
     *
     * @param prompt The prompt to display to the user.
     * @return The parsed integer value.
     */
    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = scanner.nextLine().trim();
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid whole number.");
            }
        }
    }

    /**
     * Reads a double from stdin. Loops until a valid number is entered.
     *
     * @param prompt The prompt to display to the user.
     * @return The parsed double value.
     */
    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = scanner.nextLine().trim();
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid number (e.g. 150.00).");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DATA SEEDER — Demo Data for Demonstration
    // ═══════════════════════════════════════════════════════════

    /**
     * Pre-populates the in-memory collections with realistic demo data
     * so the system is immediately usable for testing/demonstration.
     */
    private static void seedDemoData() {
        // ── Customers ────────────────────────────────────────
        customers.add(new Customer(customerIdCounter++, "Ahmed Al-Rashid", "050-1234567"));
        customers.add(new Customer(customerIdCounter++, "Sara Mohammed",   "055-9876543"));
        customers.add(new Customer(customerIdCounter++, "Khalid Farooq",   "052-4567890"));

        // ── Technicians ───────────────────────────────────────
        technicians.add(new Technician(technicianIdCounter++, "Omar Hassan",   "Mobile Phones & Tablets"));
        technicians.add(new Technician(technicianIdCounter++, "Layla Ibrahim", "Laptops & Desktops"));
        technicians.add(new Technician(technicianIdCounter++, "Tariq Nasser",  "General Electronics"));

        // ── Inventory (Parts) ─────────────────────────────────
        inventory.add(new Part(partIdCounter++, "iPhone 13 Screen",     15,  120.00));
        inventory.add(new Part(partIdCounter++, "Samsung Battery 4000mAh", 8, 35.00));
        inventory.add(new Part(partIdCounter++, "Laptop Keyboard",       4,   75.00));
        inventory.add(new Part(partIdCounter++, "Charging Port USB-C",  20,   18.00));
        inventory.add(new Part(partIdCounter++, "Thermal Paste 4g",      3,    8.50));

        // ── Job Cards (Sample Open Jobs) ──────────────────────
        // Job 1: In DIAGNOSIS stage
        JobCard job1 = new JobCard(
                jobCardIdCounter++,
                customers.get(0),
                "iPhone 13 Pro",
                "SN-APPLE-001XY",
                "Screen cracked, touch not responding");
        job1.assignTechnician(technicians.get(0)); // Omar Hassan
        jobCards.add(job1);

        // Job 2: In REPAIRING stage (fully walked through)
        JobCard job2 = new JobCard(
                jobCardIdCounter++,
                customers.get(1),
                "Dell Laptop Inspiron 15",
                "SN-DELL-87654",
                "Keyboard keys not working, some keys stuck");
        job2.assignTechnician(technicians.get(1)); // Layla Ibrahim
        job2.setDiagnosis("Keyboard membrane damaged; needs full replacement");
        job2.updateStatus(JobCard.RepairStatus.AWAITING_APPROVAL);
        job2.updateStatus(JobCard.RepairStatus.REPAIRING);
        job2.addPart(inventory.get(2), 1); // Laptop Keyboard x1
        jobCards.add(job2);

        // Job 3: READY for invoice
        JobCard job3 = new JobCard(
                jobCardIdCounter++,
                customers.get(2),
                "Samsung Galaxy S22",
                "SN-SAM-112233",
                "Battery draining too fast");
        job3.assignTechnician(technicians.get(0)); // Omar Hassan
        job3.setDiagnosis("Battery degraded to 68% capacity; replacement required");
        job3.updateStatus(JobCard.RepairStatus.AWAITING_APPROVAL);
        job3.updateStatus(JobCard.RepairStatus.REPAIRING);
        job3.addPart(inventory.get(1), 1); // Samsung Battery x1
        job3.setLaborCharge(40.00);
        job3.updateStatus(JobCard.RepairStatus.QUALITY_CHECK);
        job3.updateStatus(JobCard.RepairStatus.READY);
        jobCards.add(job3);

        System.out.println("[SEED] Demo data loaded successfully.\n");
    }
}
