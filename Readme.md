# FixTrack — Repair Shop Job Card System

An Object-Oriented Programming (OOP) project designed to streamline and manage the daily operations of a repair shop. This system provides an end-to-end workflow from customer intake to final invoicing.

## 🚀 Features

- **Dashboard:** High-level overview of open, in-repair, and completed jobs, alongside pending payments and daily revenue.
- **Customer & Device Management:** Track customer details, device specifics, and historical records.
- **Job Card Workflow:** Comprehensive tracking from complaint recording, technician assignment, diagnosis, customer approval, and active repair status.
- **Inventory Tracking:** Manage repair parts, monitor stock levels, and alert on low inventory.
- **Technician Management:** Monitor active jobs grouped by technician specialty (e.g., Hardware, Software, Electronics).
- **Invoicing & Payments:** Generate detailed invoices calculating parts, labor, and discounts, while tracking cash payments.
- **Reporting & Administration:** Daily/monthly metrics, performance reports, and overall system configuration.

## 📂 Repository Structure

The project follows a standard Maven directory layout separating source code, documentation, database configurations, and assets:

```text
FixTrack/
│
├── README.md
├── pom.xml
├── repair_shop.db
├── docs/
│   ├── database_design.txt
│   ├── future_features.txt
│   ├── graphical_workflow.txt
│   ├── oop_design.txt
│   ├── project_overview.txt
│   └── system_modules.txt
├── screenshots/
└── src/
    └── main/
        └── java/
            ├── business/   # Domain logic and workflow processing
            ├── dao/        # Data Access Objects for database operations
            ├── database/   # Database connection and schema manager
            ├── model/      # Core entities (Customer, JobCard, Part, Technician)
            └── Main.java   # Application entry point & CLI runner
```
