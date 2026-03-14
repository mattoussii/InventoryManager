package app;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import javafx.stage.FileChooser;
import model.Product;
import service.StockManager;

import java.io.File;
import java.io.FileWriter;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class SalesPage extends BorderPane {

    private final StockManager manager;

    private final TableView<String[]> table = new TableView<>();

    private final ComboBox<Product> productBox = new ComboBox<>();
    private final TextField clientField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField quantityField = new TextField();
    private final TextField searchField = new TextField();

    private final Label totalPriceLabel = new Label("Total: 0 DT");

    private final List<String[]> allSales = new ArrayList<>();

    public SalesPage(StockManager manager) {

        this.manager = manager;

        createColumns();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadSales();
        createSalesForm();

        setPadding(new Insets(10));
        setCenter(table);
    }

    /* ================= SALES FORM ================= */

    private void createSalesForm() {

        ObservableList<Product> products = manager.getObservableProducts();

        productBox.setItems(products);
        productBox.setPromptText("Select Product");

        clientField.setPromptText("Client Name");
        phoneField.setPromptText("Phone Number");
        quantityField.setPromptText("Quantity");

        searchField.setPromptText("Search sales...");

        Button sellButton = new Button("Sell");
        Button exportButton = new Button("Export Sales");

        sellButton.setOnAction(e -> sellProduct());
        exportButton.setOnAction(e -> exportSales());

        quantityField.textProperty().addListener((obs, oldV, newV) -> updateTotal());
        productBox.valueProperty().addListener((obs, oldV, newV) -> updateTotal());

        searchField.textProperty().addListener((obs, oldV, newV) -> filterSales());

        GridPane form = new GridPane();

        form.setHgap(10);
        form.setVgap(10);

        form.add(new Label("Product"),0,0);
        form.add(productBox,1,0);

        form.add(new Label("Client"),0,1);
        form.add(clientField,1,1);

        form.add(new Label("Phone"),0,2);
        form.add(phoneField,1,2);

        form.add(new Label("Quantity"),0,3);
        form.add(quantityField,1,3);

        form.add(totalPriceLabel,1,4);
        form.add(sellButton,1,5);

        form.add(new Label("Search"),0,6);
        form.add(searchField,1,6);

        form.add(exportButton,1,7);

        form.setPadding(new Insets(10));

        setTop(form);
    }

    /* ================= AUTO PRICE ================= */

    private void updateTotal() {

        Product product = productBox.getValue();

        if(product == null){
            totalPriceLabel.setText("Total: 0 DT");
            return;
        }

        try{

            int qty = Integer.parseInt(quantityField.getText());

            if(qty <= 0){
                totalPriceLabel.setText("Total: 0 DT");
                return;
            }

            double total = product.price() * qty;

            totalPriceLabel.setText(
                    String.format("Total: %.3f DT", total)
            );

        }catch(Exception e){
            totalPriceLabel.setText("Total: 0 DT");
        }
    }

    /* ================= SELL PRODUCT ================= */

    private void sellProduct() {

        Product product = productBox.getValue();

        if(product == null){
            showError("Please select a product.");
            return;
        }

        String client = clientField.getText().trim();
        String phone = phoneField.getText().trim();
        String qtyText = quantityField.getText().trim();

        if(client.length() < 2){
            showError("Client name must contain at least 2 characters.");
            return;
        }

        if(!phone.matches("\\d{8,15}")){
            showError("Phone must contain 8–15 digits.");
            return;
        }

        int qty;

        try{
            qty = Integer.parseInt(qtyText);
        }catch(Exception e){
            showError("Quantity must be a number.");
            return;
        }

        if(qty <= 0){
            showError("Quantity must be greater than zero.");
            return;
        }

        int stock = manager.getQuantity(product);

        if(qty > stock){
            showError("Not enough stock. Available: " + stock);
            return;
        }

        boolean success =
                manager.sellProduct(
                        product.id(),
                        qty,
                        client,
                        phone
                );

        if(!success){
            showError("Sale failed.");
            return;
        }

        reloadSales();

        clientField.clear();
        phoneField.clear();
        quantityField.clear();

        totalPriceLabel.setText("Total: 0 DT");
    }

    /* ================= ERROR DIALOG ================= */

    private void showError(String message){

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Invalid Input");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* ================= SALES TABLE ================= */

    private void createColumns(){

        String[] columns = {
                "Product ID",
                "Product Name",
                "Client",
                "Phone",
                "Quantity",
                "Price",
                "Date"
        };

        for(int i = 0; i < columns.length; i++){

            final int index = i;

            TableColumn<String[],String> col =
                    new TableColumn<>(columns[i]);

            col.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            data.getValue()[index]
                    ));

            if(index == 5){

                col.setCellFactory(column -> new TableCell<>(){

                    @Override
                    protected void updateItem(String value, boolean empty){

                        super.updateItem(value, empty);

                        if(empty || value == null){
                            setText(null);
                            return;
                        }

                        try{

                            double price = Double.parseDouble(value);

                            setText(String.format("%.3f DT", price));

                        }catch(Exception e){
                            setText(value);
                        }
                    }
                });
            }

            col.setPrefWidth(150);

            table.getColumns().add(col);
        }
    }

    /* ================= LOAD SALES ================= */

    private void loadSales(){

        allSales.clear();

        try (var conn = DriverManager.getConnection("jdbc:sqlite:inventory.db");
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM sales ORDER BY date DESC")) {

            while(rs.next()){

                String[] row = {
                        String.valueOf(rs.getInt("product_id")),
                        rs.getString("product_name"),
                        rs.getString("client_name"),
                        rs.getString("client_phone"),
                        String.valueOf(rs.getInt("quantity")),
                        String.valueOf(rs.getDouble("price")),
                        rs.getString("date")
                };

                allSales.add(row);
            }

            table.getItems().setAll(allSales);

        } catch (Exception e){
            System.out.println("No sales history found.");
        }
    }

    private void reloadSales(){

        table.getItems().clear();
        loadSales();
    }

    /* ================= SALES SEARCH ================= */

    private void filterSales(){

        String text = searchField.getText().toLowerCase();

        if(text.isEmpty()){
            table.getItems().setAll(allSales);
            return;
        }

        List<String[]> filtered = new ArrayList<>();

        for(String[] sale : allSales){

            if(sale[1].toLowerCase().contains(text) ||
                    sale[2].toLowerCase().contains(text) ||
                    sale[3].toLowerCase().contains(text)){

                filtered.add(sale);
            }
        }

        table.getItems().setAll(filtered);
    }

    /* ================= EXPORT SALES ================= */

    private void exportSales(){

        FileChooser chooser = new FileChooser();

        chooser.setTitle("Export Sales Report");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files","*.csv")
        );

        chooser.setInitialFileName("sales_report.csv");

        File file = chooser.showSaveDialog(getScene().getWindow());

        if(file == null)
            return;

        try(FileWriter writer = new FileWriter(file)){

            writer.write("Product ID;Product Name;Client;Phone;Quantity;Price;Date\n");

            for(String[] sale : table.getItems()){

                writer.write(String.join(";",sale) + "\n");
            }

        }catch(Exception e){

            new Alert(Alert.AlertType.ERROR,"Export failed").showAndWait();
        }
    }
}