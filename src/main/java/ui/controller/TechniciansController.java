package ui.controller;

import business.JobCard;
import dao.TechnicianDAO;
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
import model.Technician;
import ui.context.AppContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TechniciansController {

    @FXML private TextField txtSearch;
    @FXML private TableView<Technician> tblTechnicians;

    @FXML private TableColumn<Technician, Integer> colTechId;
    @FXML private TableColumn<Technician, String> colName;
    @FXML private TableColumn<Technician, String> colSpecialty;
    @FXML private TableColumn<Technician, Integer> colActiveJobs;
    @FXML private TableColumn<Technician, String> colWorkloadStatus;

    // Contextual Detail Drawer
    @FXML private VBox drawerEmptyState;
    @FXML private VBox drawerContentState;
    @FXML private Label lblSelectedTechName;
    @FXML private Label lblSelectedTechSpecialty;
    @FXML private Label lblSelectedTechActiveJobs;
    @FXML private TextArea txtAssignedJobs;

    private final ObservableList<Technician> techList = FXCollections.observableArrayList();
    private FilteredList<Technician> filteredTechs;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadTechnicians();

        tblTechnicians.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            displayTechnicianDetails(selected);
        });
    }

    private void setupTable() {
        colTechId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTechId()).asObject());
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colSpecialty.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSpecialty()));
        colActiveJobs.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getActiveJobsCount()).asObject());

        colWorkloadStatus.setCellValueFactory(data -> {
            int active = data.getValue().getActiveJobsCount();
            String status;
            if (active == 0) status = "AVAILABLE";
            else if (active <= 2) status = "MODERATE";
            else status = "HEAVY";
            return new SimpleStringProperty(status);
        });

        colWorkloadStatus.setCellFactory(col -> new TableCell<Technician, String>() {
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
                    badge.setText(item);
                    badge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                    if ("AVAILABLE".equals(item)) {
                        badge.getStyleClass().add("badge-completed");
                    } else if ("MODERATE".equals(item)) {
                        badge.getStyleClass().add("badge-repairing");
                    } else {
                        badge.getStyleClass().add("badge-awaiting");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private void setupFilters() {
        filteredTechs = new FilteredList<>(techList, p -> true);

        txtSearch.textProperty().addListener((obs, o, n) -> {
            String query = (n == null) ? "" : n.toLowerCase().trim();
            filteredTechs.setPredicate(tech -> {
                if (query.isEmpty()) return true;
                return tech.getName().toLowerCase().contains(query) ||
                        tech.getSpecialty().toLowerCase().contains(query) ||
                        String.valueOf(tech.getTechId()).contains(query);
            });
        });

        tblTechnicians.setItems(filteredTechs);
    }

    public void loadTechnicians() {
        CompletableFuture.supplyAsync(() -> AppContext.getInstance().getTechnicianDAO().getAllTechnicians())
                .thenAccept(techs -> Platform.runLater(() -> {
                    techList.setAll(techs);
                    if (!techList.isEmpty()) {
                        tblTechnicians.getSelectionModel().selectFirst();
                    } else {
                        displayTechnicianDetails(null);
                    }
                }));
    }

    private void displayTechnicianDetails(Technician t) {
        if (t == null) {
            if (drawerEmptyState != null) drawerEmptyState.setVisible(true);
            if (drawerContentState != null) drawerContentState.setVisible(false);
            return;
        }

        if (drawerEmptyState != null) drawerEmptyState.setVisible(false);
        if (drawerContentState != null) drawerContentState.setVisible(true);

        lblSelectedTechName.setText(t.getName());
        lblSelectedTechSpecialty.setText(t.getSpecialty());
        lblSelectedTechActiveJobs.setText(String.valueOf(t.getActiveJobsCount()));

        List<JobCard> jobs = AppContext.getInstance().getJobCardDAO().getAllJobCards().stream()
                .filter(j -> j.getAssignedTech() != null && j.getAssignedTech().getTechId() == t.getTechId())
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        if (jobs.isEmpty()) {
            sb.append("No repair tasks assigned to this technician.");
        } else {
            for (JobCard job : jobs) {
                sb.append(String.format("Job #%-3d  •  %-18s  •  %s  •  $%.2f%n",
                        job.getJobId(),
                        job.getDeviceDetails(),
                        job.getStatus(),
                        job.getLaborCharge()));
                if (job.getDiagnosis() != null && !job.getDiagnosis().isBlank()) {
                    sb.append("  Notes: ").append(job.getDiagnosis()).append("\n");
                }
                sb.append("\n");
            }
        }

        txtAssignedJobs.setText(sb.toString().trim());
    }

    @FXML
    private void handleAddTechnician() {
        Dialog<Technician> dialog = new Dialog<>();
        dialog.setTitle("Add Technician");
        dialog.setHeaderText("Register Specialist");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextField txtName = new TextField();
        txtName.setPromptText("Full name");

        TextField txtSpecialty = new TextField();
        txtSpecialty.setPromptText("Technical specialty");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Specialty:"), 0, 1);
        grid.add(txtSpecialty, 1, 1);

        pane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String name = txtName.getText() == null ? "" : txtName.getText().trim();
                String spec = txtSpecialty.getText() == null ? "" : txtSpecialty.getText().trim();
                if (!name.isEmpty() && !spec.isEmpty()) {
                    return new Technician(0, name, spec);
                }
            }
            return null;
        });

        Optional<Technician> result = dialog.showAndWait();
        result.ifPresent(tech -> {
            AppContext.getInstance().getTechnicianDAO().addTechnician(tech);
            loadTechnicians();
        });
    }

    @FXML
    private void handleRefresh() {
        loadTechnicians();
    }
}
