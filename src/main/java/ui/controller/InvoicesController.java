package ui.controller;

import business.Invoice;
import business.JobCard;
import dao.InvoiceDAO;
import dao.JobCardDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import ui.context.AppContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InvoicesController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbPaymentFilter;
    @FXML private TableView<Invoice> tblInvoices;

    @FXML private TableColumn<Invoice, Integer> colInvoiceId;
    @FXML private TableColumn<Invoice, Integer> colJobId;
    @FXML private TableColumn<Invoice, String> colCustomer;
    @FXML private TableColumn<Invoice, String> colDevice;
    @FXML private TableColumn<Invoice, Double> colPartsTotal;
    @FXML private TableColumn<Invoice, Double> colLabor;
    @FXML private TableColumn<Invoice, Double> colDiscount;
    @FXML private TableColumn<Invoice, Double> colGrandTotal;
    @FXML private TableColumn<Invoice, String> colPaymentStatus;

    @FXML private Button btnProcessPayment;
    @FXML private Button btnViewReceipt;

    private final ObservableList<Invoice> invoiceList = FXCollections.observableArrayList();
    private FilteredList<Invoice> filteredInvoices;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadInvoices();

        tblInvoices.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            boolean hasSelection = (selected != null);
            btnViewReceipt.setDisable(!hasSelection);
            btnProcessPayment.setDisable(!hasSelection || selected.getPaymentStatus() != Invoice.PaymentStatus.PENDING);
        });
    }

    private void setupTable() {
        colInvoiceId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getInvoiceId()).asObject());
        colJobId.setCellValueFactory(data -> new SimpleIntegerProperty(
                data.getValue().getJobCard() != null ? data.getValue().getJobCard().getJobId() : 0).asObject());
        colCustomer.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getJobCard() != null && data.getValue().getJobCard().getCustomer() != null ?
                        data.getValue().getJobCard().getCustomer().getName() : "Unknown"));
        colDevice.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getJobCard() != null ? data.getValue().getJobCard().getDeviceDetails() : "-"));
        colPartsTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPartsTotal()).asObject());
        colLabor.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getLaborCharge()).asObject());
        colDiscount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getDiscount()).asObject());
        colGrandTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getGrandTotal()).asObject());
        colPaymentStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentStatus().name()));

        colPartsTotal.setCellFactory(col -> formatCurrencyCell());
        colLabor.setCellFactory(col -> formatCurrencyCell());
        colDiscount.setCellFactory(col -> formatCurrencyCell());
        colGrandTotal.setCellFactory(col -> formatCurrencyCell());

        colPaymentStatus.setCellFactory(col -> new TableCell<Invoice, String>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("badge");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    badge.setText(item.replace("_", " "));
                    badge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                    switch (item) {
                        case "PENDING":
                            badge.getStyleClass().add("badge-pending");
                            break;
                        case "PAID_CASH":
                            badge.getStyleClass().add("badge-paid");
                            break;
                        case "PAID_CARD":
                            badge.getStyleClass().add("badge-repairing");
                            break;
                        default:
                            badge.getStyleClass().add("badge-intake");
                            break;
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private TableCell<Invoice, Double> formatCurrencyCell() {
        return new TableCell<Invoice, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("$%.2f", item));
            }
        };
    }

    private void setupFilters() {
        cmbPaymentFilter.getItems().addAll("ALL", "PENDING", "PAID_CASH", "PAID_CARD");
        cmbPaymentFilter.setValue("ALL");

        filteredInvoices = new FilteredList<>(invoiceList, p -> true);

        Runnable applyFilter = () -> {
            String query = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
            String status = cmbPaymentFilter.getValue();

            filteredInvoices.setPredicate(inv -> {
                boolean matchStatus = "ALL".equalsIgnoreCase(status) || inv.getPaymentStatus().name().equalsIgnoreCase(status);
                if (!matchStatus) return false;

                if (query.isEmpty()) return true;

                String invId = String.valueOf(inv.getInvoiceId());
                String jobId = (inv.getJobCard() != null) ? String.valueOf(inv.getJobCard().getJobId()) : "";
                String cust = (inv.getJobCard() != null && inv.getJobCard().getCustomer() != null) ?
                        inv.getJobCard().getCustomer().getName().toLowerCase() : "";
                String dev = (inv.getJobCard() != null) ? inv.getJobCard().getDeviceDetails().toLowerCase() : "";

                return invId.contains(query) || jobId.contains(query) || cust.contains(query) || dev.contains(query);
            });
        };

        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter.run());
        cmbPaymentFilter.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        tblInvoices.setItems(filteredInvoices);
    }

    public void loadInvoices() {
        CompletableFuture.supplyAsync(() -> AppContext.getInstance().getInvoiceDAO().getAllInvoices())
                .thenAccept(list -> Platform.runLater(() -> {
                    invoiceList.setAll(list);
                    if (!invoiceList.isEmpty()) {
                        tblInvoices.getSelectionModel().selectFirst();
                    } else {
                        btnProcessPayment.setDisable(true);
                        btnViewReceipt.setDisable(true);
                    }
                }));
    }

    @FXML
    private void handleGenerateInvoice() {
        JobCardDAO jobCardDAO = AppContext.getInstance().getJobCardDAO();
        InvoiceDAO invoiceDAO = AppContext.getInstance().getInvoiceDAO();

        List<JobCard> unbilledJobs = jobCardDAO.getAllJobCards().stream()
                .filter(j -> !invoiceDAO.existsForJob(j.getJobId()))
                .collect(Collectors.toList());

        if (unbilledJobs.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "All existing job cards have invoices generated.", ButtonType.OK).showAndWait();
            return;
        }

        Dialog<Invoice> dialog = new Dialog<>();
        dialog.setTitle("Generate Invoice");
        dialog.setHeaderText("Create Billing Record");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        ComboBox<JobCard> cmbJob = new ComboBox<>(FXCollections.observableArrayList(unbilledJobs));
        cmbJob.setConverter(new StringConverter<JobCard>() {
            @Override public String toString(JobCard j) {
                return j != null ? "Job #" + j.getJobId() + " — " + j.getCustomer().getName() + " (" + j.getDeviceDetails() + ")" : "";
            }
            @Override public JobCard fromString(String string) { return null; }
        });
        cmbJob.getSelectionModel().selectFirst();

        Label lblSubtotal = new Label("$0.00");
        lblSubtotal.getStyleClass().add("user-name");
        TextField txtDiscount = new TextField("0.00");
        Label lblGrandTotalPreview = new Label("$0.00");
        lblGrandTotalPreview.getStyleClass().add("header-title");

        Runnable updateTotals = () -> {
            JobCard jc = cmbJob.getValue();
            if (jc != null) {
                double subtotal = jc.getLaborCharge() + jc.getPartsTotal();
                lblSubtotal.setText(String.format("$%.2f", subtotal));
                try {
                    double disc = Double.parseDouble(txtDiscount.getText().trim());
                    double grand = Math.max(0, subtotal - disc);
                    lblGrandTotalPreview.setText(String.format("$%.2f", grand));
                } catch (NumberFormatException e) {
                    lblGrandTotalPreview.setText(String.format("$%.2f", subtotal));
                }
            }
        };

        cmbJob.valueProperty().addListener((obs, o, n) -> updateTotals.run());
        txtDiscount.textProperty().addListener((obs, o, n) -> updateTotals.run());
        updateTotals.run();

        grid.add(new Label("Job Card:"), 0, 0);
        grid.add(cmbJob, 1, 0);
        grid.add(new Label("Subtotal:"), 0, 1);
        grid.add(lblSubtotal, 1, 1);
        grid.add(new Label("Discount ($):"), 0, 2);
        grid.add(txtDiscount, 1, 2);
        grid.add(new Label("Grand Total:"), 0, 3);
        grid.add(lblGrandTotalPreview, 1, 3);

        pane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                JobCard jc = cmbJob.getValue();
                double disc = 0.0;
                try {
                    disc = Double.parseDouble(txtDiscount.getText().trim());
                } catch (NumberFormatException ignored) {}

                if (jc != null) {
                    return new Invoice(0, jc, disc);
                }
            }
            return null;
        });

        Optional<Invoice> res = dialog.showAndWait();
        res.ifPresent(invoice -> {
            AppContext.getInstance().getInvoiceDAO().addInvoice(invoice);
            loadInvoices();
        });
    }

    @FXML
    private void handleProcessPayment() {
        Invoice selected = tblInvoices.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getPaymentStatus() != Invoice.PaymentStatus.PENDING) return;

        ChoiceDialog<String> dialog = new ChoiceDialog<>("Cash", "Cash", "Card");
        dialog.setTitle("Process Payment");
        dialog.setHeaderText("Invoice #" + selected.getInvoiceId() + "  •  $" + String.format("%.2f", selected.getGrandTotal()));
        dialog.setContentText("Payment Method:");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(method -> {
            Invoice.PaymentStatus status = "Cash".equals(method) ? Invoice.PaymentStatus.PAID_CASH : Invoice.PaymentStatus.PAID_CARD;
            boolean success = selected.processPayment(status);
            if (success) {
                AppContext.getInstance().getInvoiceDAO().updateInvoice(selected);
                AppContext.getInstance().getJobCardDAO().updateJobCard(selected.getJobCard());
                tblInvoices.refresh();
                btnProcessPayment.setDisable(true);
            }
        });
    }

    @FXML
    private void handleViewReceipt() {
        Invoice selected = tblInvoices.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Invoice Receipt");
        alert.setHeaderText("Invoice #" + selected.getInvoiceId());

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Invoice ID   : #%d%n", selected.getInvoiceId()));
        if (selected.getJobCard() != null) {
            sb.append(String.format("Job Card     : #%d%n", selected.getJobCard().getJobId()));
            if (selected.getJobCard().getCustomer() != null) {
                sb.append(String.format("Customer     : %s (%s)%n", selected.getJobCard().getCustomer().getName(), selected.getJobCard().getCustomer().getPhone()));
            }
            sb.append(String.format("Device       : %s%n", selected.getJobCard().getDeviceDetails()));
        }
        sb.append("──────────────────────────────────────────\n");
        sb.append(String.format("Parts Total  : $%.2f%n", selected.getPartsTotal()));
        sb.append(String.format("Labor Charge : $%.2f%n", selected.getLaborCharge()));
        sb.append(String.format("Discount     : -$%.2f%n", selected.getDiscount()));
        sb.append("──────────────────────────────────────────\n");
        sb.append(String.format("Grand Total  : $%.2f%n", selected.getGrandTotal()));
        sb.append(String.format("Status       : %s%n", selected.getPaymentStatus()));

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setPrefWidth(420);
        textArea.setPrefHeight(260);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        loadInvoices();
    }
}
