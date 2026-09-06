package model;

/**
 * ============================================================
 *  ENTITY LAYER — Part.java
 *  Repair Shop Job Card System
 * ============================================================
 *
 *  Represents a spare part or consumable item stored in the
 *  shop's inventory. Tracks stock quantity and price, and
 *  provides utility methods for stock management.
 *
 *  OOP Principles Applied:
 *  - Encapsulation : all fields are private; access via getters/setters
 *  - Cohesion      : focused solely on inventory part management
 */
public class Part {

    // ─── Constants ────────────────────────────────────────────
    /** Stock quantity threshold below which a low-stock alert is triggered. */
    private static final int LOW_STOCK_THRESHOLD = 5;

    // ─── Attributes ───────────────────────────────────────────
    private int    partId;        // Unique identifier (PK)
    private String partName;      // Descriptive name of the part
    private int    stockQuantity; // Current units available in inventory
    private double price;         // Unit price (in shop currency)

    // ─── Constructors ─────────────────────────────────────────

    /**
     * Full constructor — used when adding a new part to the inventory.
     *
     * @param partId        Auto-generated unique ID.
     * @param partName      Name/description of the part.
     * @param stockQuantity Initial quantity in stock.
     * @param price         Unit selling price.
     */
    public Part(int partId, String partName, int stockQuantity, double price) {
        this.partId        = partId;
        this.partName      = partName;
        this.stockQuantity = stockQuantity;
        this.price         = price;
    }

    // ─── Business Methods ─────────────────────────────────────

    /**
     * Updates the stock level by the given quantity delta.
     * Use a positive value to add stock, negative to deduct.
     * Prevents stock from going below zero.
     *
     * @param quantityDelta The amount to add (positive) or deduct (negative).
     * @return {@code true} if the update succeeded; {@code false} if insufficient stock.
     */
    public boolean updateStock(int quantityDelta) {
        if (stockQuantity + quantityDelta < 0) {
            System.out.println("[ERROR] Insufficient stock for: " + partName +
                    " (Available: " + stockQuantity + ", Requested: " + Math.abs(quantityDelta) + ")");
            return false;
        }
        stockQuantity += quantityDelta;
        System.out.println("[INFO] Stock updated for '" + partName + "'. New quantity: " + stockQuantity);

        // Automatically alert if stock falls below threshold after update
        if (checkLowStock()) {
            System.out.println("[ALERT] Low stock warning: '" + partName +
                    "' has only " + stockQuantity + " unit(s) remaining!");
        }
        return true;
    }

    /**
     * Checks whether the current stock level is at or below the low-stock threshold.
     *
     * @return {@code true} if stock is low; {@code false} otherwise.
     */
    public boolean checkLowStock() {
        return stockQuantity <= LOW_STOCK_THRESHOLD;
    }

    /**
     * Returns a concise one-line display string for list/table views.
     *
     * @return Formatted display string with low-stock indicator.
     */
    public String toDisplayString() {
        String alert = checkLowStock() ? " [!LOW STOCK]" : "";
        return String.format("| ID: %-4d | Part: %-25s | Stock: %-5d | Price: $%-8.2f |%s",
                partId, partName, stockQuantity, price, alert);
    }

    // ─── Getters & Setters ────────────────────────────────────

    public int getPartId() { return partId; }
    public void setPartId(int partId) { this.partId = partId; }

    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    // ─── toString ─────────────────────────────────────────────

    @Override
    public String toString() {
        return "Part{id=" + partId + ", name='" + partName + "', stock=" + stockQuantity +
                ", price=" + price + "}";
    }
}
