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

The project is organized to separate documentation, source code, and database resources utilizing a standard Maven layout:

```text
FixTrack/
│
├── README.md
├── pom.xml
├── docs/
│   ├── project-overview.txt
│   ├── graphical-workflow.txt
│   ├── system-modules.txt
│   ├── database-design.txt
│   ├── oop-design.txt
│   └── future-features.txt
│
├── src/main/java/
│   ├── business/      # Domain logic and processing
│   ├── dao/           # Data Access Objects (Database interaction)
│   ├── database/      # Database connection managers
│   ├── model/         # Data entities (Customer, JobCard, etc.)
│   └── Main.java      # Application entry point
│
├── repair_shop.db     # SQLite Database
│
└── screenshots/
    └── (CLI execution screenshots later)
```
