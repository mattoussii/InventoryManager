import java.io.*;
import java.util.TreeMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class StockManager {

    /* ===============================
       SORTED STORAGE (AUTO BY ID)
       =============================== */
    private final TreeMap<Integer, StockItem> stock =
            new TreeMap<>();

    private int nextId = 1;

    /* ===============================
       OBSERVABLE PRODUCTS
       =============================== */
    public ObservableList<Product> getObservableProducts() {

        return FXCollections.observableArrayList(
                stock.values()
                        .stream()
                        .map(StockItem::getProduct)
                        .toList()
        );
    }

    /* ===============================
       AUTO PRODUCT ID
       =============================== */
    public int generateProductId() {
        return nextId++;
    }

    /* ===============================
       ADD PRODUCT
       =============================== */
    public void addProduct(Product product, int quantity) {

        if (stock.containsKey(product.id())) {
            System.out.println("Product already exists.");
            return;
        }

        stock.put(product.id(),
                new StockItem(product, quantity));

        saveToFile();
    }

    /* ===============================
       FIND PRODUCT
       =============================== */
    @SuppressWarnings("unused")
    public Product findProductById(int id) {

        StockItem item = stock.get(id);
        return item == null ? null : item.getProduct();
    }

    /* ===============================
       DELETE PRODUCT
       =============================== */
    public void deleteProduct(int id) {

        if (stock.remove(id) != null)
            saveToFile();
    }

    /* ===============================
       GET QUANTITY
       =============================== */
    public int getQuantity(Product product) {

        StockItem item = stock.get(product.id());
        return item == null ? 0 : item.getQuantity();
    }

    /* ===============================
       UPDATE NAME
       =============================== */
    @SuppressWarnings("unused")
    public void updateProductName(int id, String newName) {

        if(newName == null || newName.trim().length() < 3)
            return;

        StockItem item = stock.get(id);
        if(item == null) return;

        Product old = item.getProduct();

        item.setProduct(
                new Product(
                        old.id(),
                        newName,
                        old.price()));

        saveToFile();
    }

    /* ===============================
       UPDATE PRICE
       =============================== */
    @SuppressWarnings("unused")
    public void updateProductPrice(int id, double newPrice) {

        if(newPrice < 0) return;

        StockItem item = stock.get(id);
        if(item == null) return;

        Product old = item.getProduct();

        item.setProduct(
                new Product(
                        old.id(),
                        old.name(),
                        newPrice));

        saveToFile();
    }

    /* ===============================
       UPDATE QUANTITY
       =============================== */
    public void updateProduct(int id, int newQuantity) {

        if(newQuantity < 0) return;

        StockItem item = stock.get(id);

        if (item == null) {
            System.out.println("Product not found.");
            return;
        }

        item.setQuantity(newQuantity);
        saveToFile();
    }

    /* ===============================
       LOW STOCK ALERT
       =============================== */
    @SuppressWarnings("unused")
    public void lowStockAlert() {

        boolean found = false;

        for (StockItem item : stock.values()) {

            if (item.getQuantity() < 5) {

                System.out.println(
                        "Low stock: "
                                + item.getProduct().name()
                                + " (Qty: "
                                + item.getQuantity() + ")");

                found = true;
            }
        }

        if (!found)
            System.out.println("No low stock products.");
    }

    /* ===============================
       SAVE FILE
       =============================== */
    public void saveToFile() {

        try (FileWriter fw = new FileWriter("stock.txt")) {

            for (StockItem item : stock.values()) {

                Product p = item.getProduct();

                fw.write(
                        p.id() + "," +
                                p.name() + "," +
                                p.price() + "," +
                                item.getQuantity() + "\n");
            }

        } catch (IOException e) {
            System.out.println("Error saving file.");
        }
    }

    /* ===============================
       LOAD FILE
       =============================== */
    public void loadFromFile() {

        File file = new File("stock.txt");

        if (!file.exists()) {
            System.out.println("No previous stock found.");
            return;
        }

        try (BufferedReader br =
                     new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = br.readLine()) != null) {

                String[] data = line.split(",");

                int id = Integer.parseInt(data[0]);
                String name = data[1];
                double price = Double.parseDouble(data[2]);
                int quantity = Integer.parseInt(data[3]);

                stock.put(
                        id,
                        new StockItem(
                                new Product(id,name,price),
                                quantity));

                if (id >= nextId)
                    nextId = id + 1;
            }

            System.out.println("Stock loaded successfully.");

        } catch (IOException e) {
            System.out.println("Error loading stock.");
        }
    }
}