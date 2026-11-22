package ui.context;

import business.JobCard;
import dao.CustomerDAO;
import dao.InventoryDAO;
import dao.InvoiceDAO;
import dao.JobCardDAO;
import dao.TechnicianDAO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.stage.Stage;
import model.Customer;
import model.Part;
import model.Technician;
import ui.controller.MainLayoutController;

import java.util.List;

/**
 * Shared application context holding DAO singletons, user session info,
 * theme configuration, and navigation callbacks across controllers.
 */
public class AppContext {
    private static final AppContext INSTANCE = new AppContext();

    private final CustomerDAO customerDAO;
    private final TechnicianDAO technicianDAO;
    private final InventoryDAO inventoryDAO;
    private final JobCardDAO jobCardDAO;
    private final InvoiceDAO invoiceDAO;

    private Stage primaryStage;
    private MainLayoutController mainLayoutController;
    private String currentUser = "Admin";
    private final BooleanProperty darkMode = new SimpleBooleanProperty(true);

    private AppContext() {
        this.customerDAO = new CustomerDAO();
        this.technicianDAO = new TechnicianDAO();
        this.inventoryDAO = new InventoryDAO();
        this.jobCardDAO = new JobCardDAO(customerDAO, technicianDAO, inventoryDAO);
        this.invoiceDAO = new InvoiceDAO(jobCardDAO);
    }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public CustomerDAO getCustomerDAO() {
        return customerDAO;
    }

    public TechnicianDAO getTechnicianDAO() {
        return technicianDAO;
    }

    public InventoryDAO getInventoryDAO() {
        return inventoryDAO;
    }

    public JobCardDAO getJobCardDAO() {
        return jobCardDAO;
    }

    public InvoiceDAO getInvoiceDAO() {
        return invoiceDAO;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public MainLayoutController getMainLayoutController() {
        return mainLayoutController;
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public BooleanProperty darkModeProperty() {
        return darkMode;
    }

    public boolean isDarkMode() {
        return darkMode.get();
    }

    public void setDarkMode(boolean dark) {
        this.darkMode.set(dark);
    }

    /**
     * Seeds initial demo data if the database is currently empty,
     * maintaining parity with Main.java CLI.
     */
    public void seedDemoDataIfEmpty() {
        if (customerDAO.getAllCustomers().isEmpty()) {
            customerDAO.addCustomer(new Customer(0, "Ahmed Al-Rashid", "050-1234567"));
            customerDAO.addCustomer(new Customer(0, "Sara Mohammed", "055-9876543"));
            customerDAO.addCustomer(new Customer(0, "Khalid Farooq", "052-4567890"));
        }

        if (technicianDAO.getAllTechnicians().isEmpty()) {
            technicianDAO.addTechnician(new Technician(0, "Omar Hassan", "Mobile Phones & Tablets"));
            technicianDAO.addTechnician(new Technician(0, "Layla Ibrahim", "Laptops & Desktops"));
            technicianDAO.addTechnician(new Technician(0, "Tariq Nasser", "General Electronics"));
        }

        if (inventoryDAO.getAllParts().isEmpty()) {
            inventoryDAO.addPart(new Part(0, "iPhone 13 Screen", 15, 120.00));
            inventoryDAO.addPart(new Part(0, "Samsung Battery 4000mAh", 8, 35.00));
            inventoryDAO.addPart(new Part(0, "Laptop Keyboard", 4, 75.00));
            inventoryDAO.addPart(new Part(0, "Charging Port USB-C", 20, 18.00));
            inventoryDAO.addPart(new Part(0, "Thermal Paste 4g", 3, 8.50));
        }

        if (jobCardDAO.getAllJobCards().isEmpty()) {
            List<Customer> customers = customerDAO.getAllCustomers();
            List<Technician> techs = technicianDAO.getAllTechnicians();
            List<Part> parts = inventoryDAO.getAllParts();

            if (!customers.isEmpty() && !techs.isEmpty()) {
                JobCard job1 = new JobCard(0, customers.get(0), "iPhone 13 Pro", "SN-APPLE-001XY", "Screen cracked, touch not responding");
                job1.assignTechnician(techs.get(0));
                jobCardDAO.addJobCard(job1);
            }

            if (customers.size() >= 2 && techs.size() >= 2 && parts.size() >= 3) {
                Part laptopKeyboard = parts.get(2);
                JobCard job2 = new JobCard(0, customers.get(1),
                        "Dell Laptop Inspiron 15", "SN-DELL-87654",
                        "Keyboard keys not working, some keys stuck");
                job2.assignTechnician(techs.get(1));
                job2.setDiagnosis("Keyboard membrane damaged; needs full replacement");
                job2.updateStatus(JobCard.RepairStatus.AWAITING_APPROVAL);
                job2.updateStatus(JobCard.RepairStatus.REPAIRING);
                job2.addPart(laptopKeyboard, 1);
                jobCardDAO.addJobCard(job2);
                jobCardDAO.saveJobParts(job2);
                inventoryDAO.updatePart(laptopKeyboard);
            }

            if (customers.size() >= 3 && !techs.isEmpty() && parts.size() >= 2) {
                Part samsungBattery = parts.get(1);
                JobCard job3 = new JobCard(0, customers.get(2),
                        "Samsung Galaxy S22", "SN-SAM-112233",
                        "Battery draining too fast");
                job3.assignTechnician(techs.get(0));
                job3.setDiagnosis("Battery degraded to 68% capacity; replacement required");
                job3.updateStatus(JobCard.RepairStatus.AWAITING_APPROVAL);
                job3.updateStatus(JobCard.RepairStatus.REPAIRING);
                job3.addPart(samsungBattery, 1);
                job3.setLaborCharge(40.00);
                job3.updateStatus(JobCard.RepairStatus.QUALITY_CHECK);
                job3.updateStatus(JobCard.RepairStatus.READY);
                jobCardDAO.addJobCard(job3);
                jobCardDAO.saveJobParts(job3);
                inventoryDAO.updatePart(samsungBattery);
            }
        }
    }
}

