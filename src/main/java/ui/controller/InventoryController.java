package ui.controller;

import dao.InventoryDAO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.Part;
import ui.context.AppContext;

import java.util.List;
import java.util.Optional;

public class InventoryController {

    @FXML private TextField txtSearch;
    @FXML private CheckBox chkLowStockOnly;
    @FXML private TableView<Part> tblInventory;

    @FXML private TableColumn<Part, Integer> colPartId;
    @FXML private TableColumn<Part, String> colPartName;
    @FXML private TableColumn<Part, Integer> colStock;
    @FXML private TableColumn<Part, Double> colPrice;
    @FXML private TableColumn<Part, Double> colTotalValue;
    @FXML private TableColumn<Part, String> colStatus;

    @FXML private Button btnRestock;
    @FXML private Button btnEditPrice;

    private final ObservableList<Part> partList = FXCollections.observableArrayList();
    private FilteredList<Part> filteredParts;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadInventory();

        tblInventory.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            boolean disabled = (selected == null);
            btnRestock.setDisable(disabled);
            btnEditPrice.setDisable(disabled);
        });
    }

    private void setupTable() {
        colPartId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPartId()).asObject());
        colPartName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPartName()));
        colStock.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStockQuantity()).asObject());
        colPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrice()).asObject());
        colTotalValue.setCellValueFactory(data -> new SimpleDoubleProperty(
                data.getValue().getStockQuantity() * data.getValue().getPrice()).asObject());
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().checkLowStock() ? "LOW STOCK" : "IN STOCK"));

        colPrice.setCellFactory(col -> new TableCell<Part, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("$%.2f", item));
            }
        });

        colTotalValue.setCellFactory(col -> new TableCell<Part, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("$%.2f", item));
            }
        });

        colStatus.setCellFactory(col -> new TableCell<Part, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("LOW STOCK".equals(item)) {
                        badge.getStyleClass().add("badge-low-stock");
                    } else {
                        badge.getStyleClass().add("badge-in-stock");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private void setupFilters() {
        filteredParts = new FilteredList<>(partList, p -> true);

        Runnable applyFilter = () -> {
            String query = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
            boolean lowStockOnly = chkLowStockOnly.isSelected();

            filteredParts.setPredicate(part -> {
                if (lowStockOnly && !part.checkLowStock()) {
                    return false;
                }
                if (query.isEmpty()) {
                    return true;
                }
                return part.getPartName().toLowerCase().contains(query) ||
                        String.valueOf(part.getPartId()).contains(query);
            });
        };

        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter.run());
        chkLowStockOnly.selectedProperty().addListener((obs, o, n) -> applyFilter.run());

        tblInventory.setItems(filteredParts);
    }

    public void loadInventory() {
        InventoryDAO dao = AppContext.getInstance().getInventoryDAO();
        List<Part> parts = dao.getAllParts();
        partList.setAll(parts);

        if (!partList.isEmpty()) {
            tblInventory.getSelectionModel().selectFirst();
        } else {
            btnRestock.setDisable(true);
            btnEditPrice.setDisable(true);
        }
    }

    @FXML
    private void handleAddPart() {
        Dialog<Part> dialog = new Dialog<>();
        dialog.setTitle("New Inventory Part");
        dialog.setHeaderText("Add New Spare Part / Consumable");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextField txtName = new TextField();
        txtName.setPromptText("e.g. iPhone 14 Battery 3200mAh");

        TextField txtStock = new TextField("10");
        TextField txtPrice = new TextField("45.00");

        grid.add(new Label("Part Name:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Initial Stock:"), 0, 1);
        grid.add(txtStock, 1, 1);
        grid.add(new Label("Unit Price ($):"), 0, 2);
        grid.add(txtPrice, 1, 2);

        pane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String name = txtName.getText() == null ? "" : txtName.getText().trim();
                try {
                    int stock = Integer.parseInt(txtStock.getText().trim());
                    double price = Double.parseDouble(txtPrice.getText().trim());
                    if (!name.isEmpty()) {
                        return new Part(0, name, stock, price);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return null;
        });

        Optional<Part> result = dialog.showAndWait();
        result.ifPresent(newPart -> {
            AppContext.getInstance().getInventoryDAO().addPart(newPart);
            loadInventory();
        });
    }

    @FXML
    private void handleRestock() {
        Part selected = tblInventory.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog("10");
        dialog.setTitle("Restock Part");
        dialog.setHeaderText("Restock: " + selected.getPartName());
        dialog.setContentText("Enter number of units to add:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                int delta = Integer.parseInt(input.trim());
                if (delta > 0) {
                    selected.updateStock(delta);
                    AppContext.getInstance().getInventoryDAO().updatePart(selected);
                    tblInventory.refresh();
                }
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Please enter a valid integer quantity.", ButtonType.OK).showAndWait();
            }
        });
    }

    @FXML
    private void handleEditPrice() {
        Part selected = tblInventory.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(String.format("%.2f", selected.getPrice()));
        dialog.setTitle("Update Part Price");
        dialog.setHeaderText("Change unit selling price for: " + selected.getPartName());
        dialog.setContentText("New Unit Price ($):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                double newPrice = Double.parseDouble(input.trim());
                if (newPrice >= 0) {
                    selected.setPrice(newPrice);
                    AppContext.getInstance().getInventoryDAO().updatePart(selected);
                    tblInventory.refresh();
                }
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Please enter a valid monetary amount.", ButtonType.OK).showAndWait();
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadInventory();
    }
}

