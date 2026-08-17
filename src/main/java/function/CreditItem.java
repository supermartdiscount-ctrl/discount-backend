package function;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One cart line (product/qty/price) belonging to a Credit. */
@Entity
@Table(name = "credit_items")
public class CreditItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id", nullable = false)
    private Credit credit;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "barcode", length = 100)
    private String barcode;

    public CreditItem() {}

    public CreditItem(String itemName, String category, String unit, double price, int quantity, String barcode) {
        this.itemName = itemName;
        this.category = category;
        this.unit = unit;
        this.price = price;
        this.quantity = quantity;
        this.barcode = barcode;
    }

    public double getSubtotal() {
        return price * quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Credit getCredit() { return credit; }
    public void setCredit(Credit credit) { this.credit = credit; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
}