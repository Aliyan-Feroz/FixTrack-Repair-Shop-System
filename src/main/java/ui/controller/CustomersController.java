package ui.controller;

import business.JobCard;
import dao.CustomerDAO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import model.Customer;
import ui.context.AppContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomersController {

    @FXML private TextField txtSearch;
    @FXML private TableView<Customer> tblCustomers;
    @FXML private TableColumn<Customer, Integer> colId;
    @FXML private TableColumn<Customer, String> colName;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, Integer> colTotalJobs;

    // Customer Detail Panel
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailPhone;
    @FXML private Label lblDetailTotalJobs;
    @FXML private TextArea txtRepairHistory;
    @FXML private Button btnNewJobForCustomer;

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private FilteredList<Customer> filteredCustomers;

    @FXML
    public void initialize() {
        setupTable();
        loadCustomers();
        setupSearchFilter();

        tblCustomers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            displayCustomerDetails(selected);
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCustomerId()).asObject());
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));

        // Calculate live job count from jobCardDAO
        colTotalJobs.setCellValueFactory(data -> {
            int cid = data.getValue().getCustomerId();
            long count = AppContext.getInstance().getJobCardDAO().getAllJobCards().stream()
                    .filter(j -> j.getCustomer() != null && j.getCustomer().getCustomerId() == cid)
                    .count();
            return new SimpleIntegerProperty((int) count).asObject();
        });
    }

    public void loadCustomers() {
        CustomerDAO dao = AppContext.getInstance().getCustomerDAO();
        List<Customer> list = dao.getAllCustomers();
        customerList.setAll(list);

        if (!customerList.isEmpty()) {
            tblCustomers.getSelectionModel().selectFirst();
        } else {
            displayCustomerDetails(null);
        }
    }

    private void setupSearchFilter() {
        filteredCustomers = new FilteredList<>(customerList, p -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredCustomers.setPredicate(customer -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String lower = newVal.toLowerCase().trim();
                return customer.getName().toLowerCase().contains(lower) ||
                       customer.getPhone().toLowerCase().contains(lower) ||
                       String.valueOf(customer.getCustomerId()).contains(lower);
            });
        });
        tblCustomers.setItems(filteredCustomers);
    }

    private void displayCustomerDetails(Customer c) {
        if (c == null) {
            lblDetailName.setText("No customer selected");
            lblDetailPhone.setText("-");
            lblDetailTotalJobs.setText("0");
            txtRepairHistory.setText("Select a customer from the directory to review repair history.");
            if (btnNewJobForCustomer != null) btnNewJobForCustomer.setDisable(true);
            return;
        }

        lblDetailName.setText(c.getName());
        lblDetailPhone.setText(c.getPhone());

        List<JobCard> jobs = AppContext.getInstance().getJobCardDAO().getAllJobCards().stream()
                .filter(j -> j.getCustomer() != null && j.getCustomer().getCustomerId() == c.getCustomerId())
                .collect(Collectors.toList());

        lblDetailTotalJobs.setText(String.valueOf(jobs.size()));

        StringBuilder sb = new StringBuilder();
        sb.append("Customer ID: #").append(c.getCustomerId()).append("\n");
        sb.append("Phone Contact: ").append(c.getPhone()).append("\n\n");
        sb.append("── Repair History Records (").append(jobs.size()).append(") ──\n");

        if (jobs.isEmpty()) {
            sb.append("No job cards recorded for this customer yet.");
        } else {
            for (JobCard job : jobs) {
                sb.append(String.format("• Job #%-3d | %-20s | Status: %-15s | Total: $%.2f%n",
                        job.getJobId(),
                        job.getDeviceDetails(),
                        job.getStatus(),
                        job.calculateTotal()));
                if (job.getComplaint() != null && !job.getComplaint().isEmpty()) {
                    sb.append("   Complaint: ").append(job.getComplaint()).append("\n");
                }
                if (job.getDiagnosis() != null && !job.getDiagnosis().isEmpty()) {
                    sb.append("   Diagnosis: ").append(job.getDiagnosis()).append("\n");
                }
                sb.append("\n");
            }
        }

        txtRepairHistory.setText(sb.toString());
        if (btnNewJobForCustomer != null) btnNewJobForCustomer.setDisable(false);
    }

    @FXML
    private void handleAddCustomer() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Register New Customer");
        dialog.setHeaderText("Enter Customer Profile Details");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. John Doe");
        TextField phoneField = new TextField();
        phoneField.setPromptText("e.g. 050-1234567");

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone Number:"), 0, 1);
        grid.add(phoneField, 1, 1);

        dialogPane.setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String name = nameField.getText() == null ? "" : nameField.getText().trim();
                String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
                if (!name.isEmpty() && !phone.isEmpty()) {
                    return new Customer(0, name, phone);
                }
            }
            return null;
        });

        Optional<Customer> result = dialog.showAndWait();
        result.ifPresent(newCustomer -> {
            AppContext.getInstance().getCustomerDAO().addCustomer(newCustomer);
            loadCustomers();
            // Select newly added customer
            tblCustomers.getItems().stream()
                    .filter(c -> c.getCustomerId() == newCustomer.getCustomerId())
                    .findFirst()
                    .ifPresent(c -> tblCustomers.getSelectionModel().select(c));
        });
    }

    @FXML
    private void handleRefresh() {
        loadCustomers();
    }

    @FXML
    private void handleCreateJobForCustomer() {
        Customer selected = tblCustomers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            AppContext.getInstance().getMainLayoutController().navigateTo("JobCards");
        }
    }
}

