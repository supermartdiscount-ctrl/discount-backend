package Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Repo.BranchRepository;
import Repo.ItemRepo;
import Request.ItemRequest;
import function.Branch;
import function.Item;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemRepo itemRepo;

    @Autowired
    private BranchRepository branchRepo;

    // ===================== CREATE =====================

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addItem(@RequestBody ItemRequest request) {
        Map<String, Object> body = new HashMap<>();

        Optional<Branch> branchOpt = resolveBranch(request.getBranchId(), request.getBranchName());
        if (branchOpt.isEmpty()) {
            return badRequest(body, "Branch not found. Provide a valid branchId or branchName.");
        }
        Branch branch = branchOpt.get();

        String barcode = trimOrNull(request.getBarcode());
        String itemName = trimOrNull(request.getItemName());
        if (barcode == null || barcode.isEmpty()) {
            return badRequest(body, "Barcode is required.");
        }
        if (itemName == null || itemName.isEmpty()) {
            return badRequest(body, "Item name is required.");
        }

        if (itemRepo.existsByBranch_IdAndBarcode(branch.getId(), barcode)) {
            return badRequest(body, "Item with barcode " + barcode + " already exists in " + branch.getBranchName() + ".");
        }

        BigDecimal costPrice;
        BigDecimal sellingPrice;
        try {
            costPrice = cleanPrice(request.getCostPrice());
            sellingPrice = cleanPrice(request.getSellingPrice());
        } catch (NumberFormatException e) {
            return badRequest(body, "Cost price / selling price must be valid numbers.");
        }
        if (costPrice.signum() < 0 || sellingPrice.signum() < 0) {
            return badRequest(body, "Prices cannot be negative.");
        }

        Item item = new Item();
        item.setBranch(branch);
        item.setBarcode(barcode);
        item.setItemName(itemName);
        item.setCategory(nonEmptyOrDefault(request.getCategory(), "Uncategorized"));
        item.setType(request.getType() != null ? request.getType().trim() : "");
        item.setUnit(nonEmptyOrDefault(request.getUnit(), "pcs"));
        item.setQuantity(cleanQuantity(request.getQuantity()));
        item.setCostPrice(costPrice);
        item.setSellingPrice(sellingPrice);
        item.setPromoPrice(cleanPromo(request.getPromoPrice()));

        LocalDate expiration = parseExpiration(request.getExpiration());
        item.setExpiration(expiration);

        Item saved = itemRepo.save(item);

        body.put("success", true);
        body.put("message", "Item added successfully.");
        body.put("id", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
    
    
    
    // ===================== SEARCH =====================
    @GetMapping("/search")
    public ResponseEntity<List<Item>> searchItems(
            @RequestParam("branchId") Long branchId,
            @RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(itemRepo.searchByBranchAndQuery(branchId, query.trim()));
    }

    // ===================== READ =====================

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<Item>> getItemsByBranch(@PathVariable("branchId") Long branchId) {
        return ResponseEntity.ok(itemRepo.findByBranch_Id(branchId));
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getItem(@PathVariable("id") Long id) {
        Optional<Item> item = itemRepo.findById(id);
        if (item.isEmpty()) {
            Map<String, Object> body = new HashMap<>();
            return badRequest(body, "Item not found.");
        }
        return ResponseEntity.ok(item.get());
    }

    // ===================== UPDATE =====================

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateItem(@PathVariable("id") Long id, @RequestBody ItemRequest request){
        Map<String, Object> body = new HashMap<>();

        Optional<Item> existingOpt = itemRepo.findById(id);
        if (existingOpt.isEmpty()) {
            return badRequest(body, "Item not found.");
        }
        Item item = existingOpt.get();

        String barcode = trimOrNull(request.getBarcode());
        String itemName = trimOrNull(request.getItemName());
        if (barcode == null || barcode.isEmpty()) {
            return badRequest(body, "Barcode is required.");
        }
        if (itemName == null || itemName.isEmpty()) {
            return badRequest(body, "Item name is required.");
        }

        // If barcode changed, make sure the new one isn't already used in this branch
        if (!barcode.equals(item.getBarcode())
                && itemRepo.existsByBranch_IdAndBarcode(item.getBranch().getId(), barcode)) {
            return badRequest(body, "Item with barcode " + barcode + " already exists in this branch.");
        }

        BigDecimal costPrice;
        BigDecimal sellingPrice;
        try {
            costPrice = cleanPrice(request.getCostPrice());
            sellingPrice = cleanPrice(request.getSellingPrice());
        } catch (NumberFormatException e) {
            return badRequest(body, "Cost price / selling price must be valid numbers.");
        }

        item.setBarcode(barcode);
        item.setItemName(itemName);
        item.setCategory(nonEmptyOrDefault(request.getCategory(), "Uncategorized"));
        item.setType(request.getType() != null ? request.getType().trim() : "");
        item.setUnit(nonEmptyOrDefault(request.getUnit(), "pcs"));
        item.setQuantity(cleanQuantity(request.getQuantity()));
        item.setCostPrice(costPrice);
        item.setSellingPrice(sellingPrice);
        item.setPromoPrice(cleanPromo(request.getPromoPrice()));
        item.setExpiration(parseExpiration(request.getExpiration()));
        item.setUpdatedAt(java.time.LocalDateTime.now());

        itemRepo.save(item);

        body.put("success", true);
        body.put("message", "Item updated successfully.");
        return ResponseEntity.ok(body);
    }

    // ===================== DELETE =====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable("id") Long id){
        Map<String, Object> body = new HashMap<>();
        if (!itemRepo.existsById(id)) {
            return badRequest(body, "Item not found.");
        }
        itemRepo.deleteById(id);
        body.put("success", true);
        body.put("message", "Item deleted successfully.");
        return ResponseEntity.ok(body);
    }

    // ===================== Helpers =====================

    private Optional<Branch> resolveBranch(Long branchId, String branchName) {
        if (branchId != null) {
            return branchRepo.findById(branchId);
        }
        if (branchName != null && !branchName.trim().isEmpty()) {
            return branchRepo.findByBranchName(branchName.trim());
        }
        return Optional.empty();
    }

    private ResponseEntity<Map<String, Object>> badRequest(Map<String, Object> body, String message) {
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }

    private String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }

    private String nonEmptyOrDefault(String s, String def) {
        return (s != null && !s.trim().isEmpty()) ? s.trim() : def;
    }

    private BigDecimal cleanPrice(String price) {
        if (price == null || price.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String cleaned = price.replace("₱", "").replace("$", "").replace(",", "").trim();
        if (cleaned.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cleaned).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal cleanQuantity(String quantity) {
        if (quantity == null || quantity.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(quantity.trim()).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String cleanPromo(String promo) {
        if (promo == null || promo.trim().isEmpty()) {
            return "";
        }
        promo = promo.trim();

        if (promo.matches("\\d+(\\.\\d+)?%.*")) {
            return promo;
        }
        if (promo.matches("(?i)B\\d+T\\d+")) {
            return promo.toUpperCase();
        }
        if (promo.startsWith("₱")) {
            String priceStr = promo.substring(1).replace(",", "").trim();
            try {
                double value = Double.parseDouble(priceStr);
                return String.format("₱%.2f", value);
            } catch (NumberFormatException e) {
                return "";
            }
        }
        try {
            double value = Double.parseDouble(promo.replace(",", ""));
            return String.format("₱%.2f", value);
        } catch (NumberFormatException e) {
            return promo;
        }
    }

    private LocalDate parseExpiration(String expiration) {
        if (expiration == null || expiration.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(expiration.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}