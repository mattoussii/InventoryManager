package service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Product;
import model.Sale;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StockManager {

    private final ObservableList<Product> products =
            FXCollections.observableArrayList();

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:inventory.db");
    }

    public ObservableList<Product> getObservableProducts() {
        return products;
    }

    public int generateProductId() {

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM products")) {

            if (rs.next())
                return rs.getInt(1) + 1;

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return 1;
    }

    /* ===============================
       LOAD PRODUCTS FROM DATABASE
       =============================== */

    public void loadFromDatabase() {

        products.clear();

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {

            while (rs.next()) {

                Product p = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("max_stock"),
                        rs.getString("description")
                );

                products.add(p);
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /* ===============================
       ADD PRODUCT
       =============================== */

    public void addProduct(Product product, int quantity) {

        try (Connection conn = connect();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "INSERT INTO products VALUES (?,?,?,?,?,?,?)")) {

            ps.setInt(1, product.id());
            ps.setString(2, product.name());
            ps.setDouble(3, product.price());
            ps.setInt(4, quantity);
            ps.setInt(5, product.maxStock());
            ps.setString(6, product.description());
            ps.setString(7, "");

            ps.executeUpdate();

            products.add(product);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /* ===============================
       DELETE PRODUCT
       =============================== */

    public void deleteProduct(int id) {

        try (Connection conn = connect();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "DELETE FROM products WHERE id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

            products.removeIf(p -> p.id() == id);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /* ===============================
       GET QUANTITY
       =============================== */

    public int getQuantity(Product product) {

        try (Connection conn = connect();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT quantity FROM products WHERE id=?")) {

            ps.setInt(1, product.id());

            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return rs.getInt("quantity");

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return 0;
    }

    /* ===============================
       UPDATE PRODUCT
       =============================== */

    public void updateProduct(int id, int newQuantity) {

        try (Connection conn = connect();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "UPDATE products SET quantity=? WHERE id=?")) {

            ps.setInt(1, newQuantity);
            ps.setInt(2, id);

            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void updateProductFull( int id, String name, double price, int maxStock, String description){

        try (var conn = connect();
             var ps = conn.prepareStatement(
                     "UPDATE products SET name=?, price=?, max_stock=?, description=? WHERE id=?")) {

            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setInt(3, maxStock);
            ps.setString(4, description);
            ps.setInt(5, id);

            ps.executeUpdate();

            // refresh product list in UI
            loadFromDatabase();

        } catch (Exception e) {
            System.err.println("Product update failed: " + e.getMessage());
        }
    }

    /* ===============================
       STOCK ADJUSTMENT
       =============================== */

    public void adjustStock(int productId, int change) {

        int current = 0;

        try (Connection conn = connect();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT quantity FROM products WHERE id=?")) {

            ps.setInt(1, productId);

            ResultSet rs = ps.executeQuery();

            if (rs.next())
                current = rs.getInt("quantity");

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        int newQty = current + change;

        if (newQty < 0)
            throw new IllegalArgumentException("Stock cannot be negative");

        updateProduct(productId, newQty);
    }

    /* ===============================
       SELL PRODUCT
       =============================== */

    public boolean sellProduct(
            int productId,
            int qty,
            String clientName,
            String clientPhone
    ) {

        int currentQty = 0;
        double price = 0;
        String name = "";

        try (Connection conn = connect();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT * FROM products WHERE id=?")) {

            ps.setInt(1, productId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                currentQty = rs.getInt("quantity");
                price = rs.getDouble("price");
                name = rs.getString("name");

            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        if (qty <= 0 || currentQty < qty)
            return false;

        updateProduct(productId, currentQty - qty);

        logSale(new Sale(
                productId,
                name,
                clientName,
                clientPhone,
                qty,
                price,
                System.currentTimeMillis()
        ));

        return true;
    }

    /* ===============================
       LOG SALE
       =============================== */

    private void logSale(Sale sale) {

        try (Connection conn = connect();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "INSERT INTO sales(product_id,product_name,client_name,client_phone,quantity,price,date) VALUES (?,?,?,?,?,?,?)")) {

            LocalDateTime date =
                    LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(sale.timestamp()),
                            ZoneId.systemDefault()
                    );

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            ps.setInt(1, sale.productId());
            ps.setString(2, sale.productName());
            ps.setString(3, sale.clientName());
            ps.setString(4, sale.clientPhone());
            ps.setInt(5, sale.quantity());
            ps.setDouble(6, sale.price());
            ps.setString(7, date.format(formatter));

            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /* ===============================
       STOCK RATIO
       =============================== */

    public double getStockRatio(Product product) {

        int qty = getQuantity(product);

        int max = product.maxStock();

        if (max == 0)
            return 0;

        return (double) qty / max;
    }

    public void exportToCSV(java.io.File file) {

        try (
                Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM products");
                java.io.FileWriter fw = new java.io.FileWriter(file)
        ) {

            fw.write("ID;Name;Price;Quantity\n");

            while (rs.next()) {

                fw.write(
                        rs.getInt("id") + ";" +
                                rs.getString("name") + ";" +
                                rs.getDouble("price") + ";" +
                                rs.getInt("quantity") + "\n"
                );
            }

            System.out.println("CSV export successful.");

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void loadProductsFromCSV(String filePath) {

        try (
                BufferedReader reader = new BufferedReader(new FileReader(filePath));
                Connection conn = connect();
                PreparedStatement check =
                        conn.prepareStatement(
                                "SELECT id FROM products WHERE id=?");
                PreparedStatement insert =
                        conn.prepareStatement(
                                "INSERT INTO products (id,name,price,quantity,max_stock,description) VALUES (?,?,?,?,?,?)")
        ) {

            String line;

            reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {

                String[] data = line.split(";");

                int id = Integer.parseInt(data[0]);
                String name = data[1];
                double price = Double.parseDouble(data[2]);
                int quantity = Integer.parseInt(data[3]);

                int maxStock = 100;
                String description = "";

                /* check if product already exists */

                check.setInt(1, id);

                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    continue; // skip duplicate product
                }

                insert.setInt(1, id);
                insert.setString(2, name);
                insert.setDouble(3, price);
                insert.setInt(4, quantity);
                insert.setInt(5, maxStock);
                insert.setString(6, description);

                insert.executeUpdate();

                products.add(new Product(id, name, price, maxStock, description));
            }

            System.out.println("CSV import finished. Only new products were added.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}