import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import java.io.File;

@SuppressWarnings("ALL")
public class InventoryApp extends Application {

    private final StockManager manager = new StockManager();
    private final TableView<Product> table = new TableView<>();

    private final TextField nameField = new TextField();
    private final TextField priceField = new TextField();
    private final TextField qtyField = new TextField();
    private final TextField searchField = new TextField();

    /* ===== COLORED STATUS LABELS ===== */

    private final Label productsLabel = new Label();
    private final Label criticalLabel = new Label();
    private final Label warningLabel = new Label();

    @Override
    public void start(Stage stage) {

        manager.loadFromFile();

        BorderPane root = new BorderPane();

        root.setTop(createToolbar());
        root.setCenter(createTable());
        root.setRight(createForm());
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root,1000,600);
        scene.getStylesheets().add("style.css");

        stage.setTitle("Inventory Manager");
        stage.setScene(scene);
        stage.show();

        updateStatusBar();
    }

    /* ================= TOOLBAR ================= */

    private ToolBar createToolbar() {

        Button export = new Button("Export CSV");
        Button delete = new Button("Delete");
        Button refresh = new Button("Refresh");

        delete.setOnAction(e -> deleteProduct());
        refresh.setOnAction(e -> refreshTable());
        export.setOnAction(e -> exportCSV());


        searchField.setPromptText("Search product...");
        searchField.textProperty()
                .addListener((a,b,c)->search());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new ToolBar(
                delete,
                export,
                new Separator(),
                searchField
        );
    }

    /* ================= TABLE ================= */

    private VBox createTable() {

        table.setEditable(true);

        /* ================= ID COLUMN ================= */

        TableColumn<Product,Integer> id =
                new TableColumn<>("ID");

        id.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleIntegerProperty(
                        d.getValue().id()).asObject());

        /* ================= NAME COLUMN (EDITABLE) ================= */

        TableColumn<Product,String> name =
                new TableColumn<>("Name");

        name.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleStringProperty(
                        d.getValue().name()));

        name.setCellFactory(TextFieldTableCell.forTableColumn());

        name.setOnEditCommit(event -> {

            Product product = event.getRowValue();
            String newName = event.getNewValue();

            if(newName == null || newName.trim().length() < 3){
                alert("Name must contain at least 3 characters.");
                refreshTable();
                return;
            }

            manager.updateProductName(product.id(), newName.trim());
            refreshTable();
        });

        /* ================= PRICE COLUMN (EDITABLE) ================= */

        TableColumn<Product, Double> price =
                new TableColumn<>("Price");

        price.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleDoubleProperty(
                        d.getValue().price()).asObject());

        price.setCellFactory(col -> new TextFieldTableCell<>(
                new javafx.util.converter.DoubleStringConverter()) {

            @Override
            public void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%.3f DT", value));
                }
            }
        });price.setCellFactory(col -> new TextFieldTableCell<>(
                new javafx.util.converter.DoubleStringConverter()) {

            @Override
            public void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%.3f DT", value));
                }
            }
        });

        price.setOnEditCommit(event -> {

            Product product = event.getRowValue();
            double newPrice = event.getNewValue();

            if(newPrice < 0){
                alert("Price cannot be negative.");
                refreshTable();
                return;
            }

            manager.updateProductPrice(product.id(), newPrice);
            refreshTable();
        });

        /* ================= QUANTITY COLUMN (EDITABLE) ================= */

        TableColumn<Product,Integer> qty =
                new TableColumn<>("Quantity");

        qty.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleIntegerProperty(
                        manager.getQuantity(
                                d.getValue()))
                        .asObject());

        qty.setCellFactory(
                TextFieldTableCell.forTableColumn(
                        new IntegerStringConverter()));

        qty.setOnEditCommit(event -> {

            Product product = event.getRowValue();
            int newQty = event.getNewValue();

            if(newQty < 0){
                alert("Quantity cannot be negative");
                refreshTable();
                return;
            }

            manager.updateProduct(
                    product.id(),
                    newQty);

            refreshTable();
        });

        /* ================= TABLE CONFIG ================= */

        table.getColumns().setAll(id,name,price,qty);

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);

        /* ================= ROW COLORING ================= */

        table.setRowFactory(tv -> new TableRow<>() {

            @Override
            protected void updateItem(Product product,
                                      boolean empty) {

                super.updateItem(product, empty);

                if(product == null || empty){
                    setStyle("");
                    return;
                }

                int q = manager.getQuantity(product);

                if(q < 10)
                    setStyle("-fx-background-color:#ffb3b3;");
                else if(q < 20)
                    setStyle("-fx-background-color:#ffe0b3;");
                else
                    setStyle("");
            }
        });

        refreshTable();

        VBox box = new VBox(table);
        box.setPadding(new Insets(10));
        return box;
    }

    /* ================= FORM ================= */

    private VBox createForm() {

        Label title = new Label("Add New Product");
        title.setStyle(
                "-fx-font-size:16px;" +
                        "-fx-font-weight:bold;"
        );

        nameField.setPromptText("Product Name");
        priceField.setPromptText("Price");
        qtyField.setPromptText("Quantity");

        Button save = new Button("Save Product");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> addProduct());

        VBox form = new VBox(12,
                title,
                nameField,
                priceField,
                qtyField,
                save);

        form.setPadding(new Insets(15));
        form.setPrefWidth(250);
        form.getStyleClass().add("form");

        return form;
    }

    /* ================= COLORED STATUS BAR/ BOX ================= */

    private HBox createStatusBar() {

        HBox productsBox =
                createStatusBox(productsLabel,
                        "#e3f2fd", "#1565c0");

        HBox criticalBox =
                createStatusBox(criticalLabel,
                        "#ffcdd2", "#b71c1c");

        HBox warningBox =
                createStatusBox(warningLabel,
                        "#fff3cd", "#ff8f00");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(
                15,
                productsBox,
                criticalBox,
                warningBox,
                spacer
        );

        bar.setPadding(new Insets(8));
        bar.getStyleClass().add("status");

        return bar;
    }

    private HBox createStatusBox(
            Label label,
            String bgColor,
            String textColor) {

        label.setStyle(
                "-fx-text-fill:" + textColor +
                        "; -fx-font-weight:bold;"
        );

        HBox box = new HBox(label);
        box.setPadding(
                new Insets(5,12,5,12));

        box.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                        "-fx-background-radius:8;" +
                        "-fx-border-radius:8;"
        );

        return box;
    }


    /* ================= LIVE COUNTER ================= */

    private void updateStatusBar() {

        var products =
                manager.getObservableProducts();

        int total = products.size();

        long critical =
                products.stream()
                        .filter(p ->
                                manager.getQuantity(p) < 10)
                        .count();

        long warning =
                products.stream()
                        .filter(p -> {
                            int q =
                                    manager.getQuantity(p);
                            return q >= 10 && q < 20;
                        })
                        .count();

        productsLabel.setText("Products: " + total);
        criticalLabel.setText("Critical: " + critical);
        warningLabel.setText("Warning: " + warning);
    }

    /* ================= LOGIC ================= */

    private void refreshTable() {

        table.setItems(
                manager.getObservableProducts());

        updateStatusBar();
    }
    private void addProduct() {

        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();
        String qtyText = qtyField.getText().trim();

        /* ===== NAME VALIDATION ===== */
        if(name.isEmpty()){
            alert("Product name cannot be empty.");
            return;
        }

        if(name.length() < 3){
            alert("Product name must contain at least 3 characters.");
            return;
        }

        /* ===== PRICE VALIDATION ===== */
        double price;

        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e){
            alert("Price must be a valid number.");
            return;
        }

        if(price <= 0){
            alert("Price must be greater than 0.");
            return;
        }

        /* ===== QUANTITY VALIDATION ===== */
        int quantity;

        try {
            quantity = Integer.parseInt(qtyText);
        } catch (NumberFormatException e){
            alert("Quantity must be a valid integer.");
            return;
        }

        if(quantity < 0){
            alert("Quantity cannot be negative.");
            return;
        }

        /* ===== ADD PRODUCT ===== */
        int id = manager.generateProductId();

        manager.addProduct(
                new Product(id, name, price),
                quantity
        );

        refreshTable();
        clear();
    }
    private void deleteProduct() {

        Product p = table.getSelectionModel().getSelectedItem();

        if(p == null){
            alert("Select a product to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Confirmation");
        confirm.setHeaderText("Are you sure you want to delete this product?");
        confirm.setContentText(
                "Product: " + p.name() +
                        "\nID: " + p.id()
        );

        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("Cancel");

        confirm.getButtonTypes().setAll(yes, no);

        confirm.showAndWait().ifPresent(response -> {
            if(response == yes){
                manager.deleteProduct(p.id());
                refreshTable();
            }
        });
    }
    private void search() {

        String text =
                searchField.getText().toLowerCase();

        table.setItems(
                manager.getObservableProducts()
                        .filtered(p ->
                                p.name()
                                        .toLowerCase()
                                        .contains(text)));
    }
    private void alert(String msg){
        new Alert(Alert.AlertType.INFORMATION,msg)
                .showAndWait();
    }
    private void exportCSV() {

        javafx.stage.FileChooser fileChooser =
                new javafx.stage.FileChooser();

        fileChooser.setTitle("Save CSV File");

        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter(
                        "CSV Files", "*.csv")
        );

        fileChooser.setInitialFileName("export_stock.csv");

        File file = fileChooser.showSaveDialog(
                table.getScene().getWindow());

        if(file != null) {

            manager.exportToCSV(file);
            alert("Stock exported successfully.");
        }
    }

    private void clear(){
        nameField.clear();
        priceField.clear();
        qtyField.clear();
    }












    @SuppressWarnings("unused")
    public static void main(String... args){
        launch(args);
    }
}