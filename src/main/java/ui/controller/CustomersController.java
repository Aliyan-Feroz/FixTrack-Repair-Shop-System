package ui.controller;

import business.JobCard;
import dao.CustomerDAO;
import javafx.application.Platform;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CustomersController {

    @FXML private TextField txtSearch;
    @FXML private TableView<Customer> tblCustomers;
    @FXML private TableColumn<Customer, Integer> colId;
    @FXML private TableColumn<Customer, String> colName;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, Integer> colTotalJobs;

    // Contextual Detail Drawer
    @FXML private VBox drawerEmptyState;
    @FXML private VBox drawerContentState;
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailPhone;
    @FXML private Label lblDetailTotalJobs;
    @FXML private TextArea txtRepairHistory;
    @FXML private Button btnNewJobForCustomer;

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private final Map<Integer, Integer> customerJobCounts = new HashMap<>();
    private FilteredList<Customer> filteredCustomers;

    private static class CustomerSnapshot {
        final List<Customer> customers;
        final Map<Integer, Integer> counts;

        CustomerSnapshot(List<Customer> customers, Map<Integer, Integer> counts) {
            this.customers = customers;
            this.counts = counts;
        }
    }

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

        colTotalJobs.setCellValueFactory(data -> {
            int cid = data.getValue().getCustomerId();
            int count = customerJobCounts.getOrDefault(cid, 0);
            return new SimpleIntegerProperty(count).asObject();
        });
    }

    public void loadCustomers() {
        CompletableFuture.supplyAsync(() -> {
            CustomerDAO dao = AppContext.getInstance().getCustomerDAO();
            List<Customer> list = dao.getAllCustomers();

            Map<Integer, Integer> counts = new HashMap<>();
            List<JobCard> allJobs = AppContext.getInstance().getJobCardDAO().getAllJobCards();
            for (JobCard job : allJobs) {
                if (job.getCustomer() != null) {
                    counts.merge(job.getCustomer().getCustomerId(), 1, Integer::sum);
                }
            }
            return new CustomerSnapshot(list, counts);
        }).thenAccept(snap -> Platform.runLater(() -> {
            customerJobCounts.clear();
            customerJobCounts.putAll(snap.counts);
            customerList.setAll(snap.customers);

            if (!customerList.isEmpty()) {
                tblCustomers.getSelectionModel().selectFirst();
            } else {
                displayCustomerDetails(null);
            }
        }));
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
            if (drawerEmptyState != null) drawerEmptyState.setVisible(true);
            if (drawerContentState != null) drawerContentState.setVisible(false);
            return;
        }

        if (drawerEmptyState != null) drawerEmptyState.setVisible(false);
        if (drawerContentState != null) drawerContentState.setVisible(true);

        lblDetailName.setText(c.getName());
        lblDetailPhone.setText(c.getPhone());

        List<JobCard> jobs = AppContext.getInstance().getJobCardDAO().getAllJobCards().stream()
                .filter(j -> j.getCustomer() != null && j.getCustomer().getCustomerId() == c.getCustomerId())
                .collect(Collectors.toList());

        lblDetailTotalJobs.setText(String.valueOf(jobs.size()));

        StringBuilder sb = new StringBuilder();
        if (jobs.isEmpty()) {
            sb.append("No recorded repair history for this customer.");
        } else {
            for (JobCard job : jobs) {
                sb.append(String.format("Job #%-3d  •  %-18s  •  %s  •  $%.2f%n",
                        job.getJobId(),
                        job.getDeviceDetails(),
                        job.getStatus(),
                        job.calculateTotal()));
                if (job.getComplaint() != null && !job.getComplaint().isBlank()) {
                    sb.append("  Issue: ").append(job.getComplaint()).append("\n");
                }
                sb.append("\n");
            }
        }

        txtRepairHistory.setText(sb.toString().trim());
    }

    @FXML
    private void handleAddCustomer() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Register Customer");
        dialog.setHeaderText("New Customer Profile");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextField nameField = new TextField();
        nameField.setPromptText("Customer name");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone number");

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
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
