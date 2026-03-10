package app;

import model.Product;
import service.StockManager;

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
    private final TextField maxStockField = new TextField();
    private final TextArea descriptionArea = new TextArea();



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

        Scene scene = new Scene(root, 1000, 600);

        scene.getStylesheets().add(
                getClass()
                        .getResource("/resources/style.css")
                        .toExternalForm()
        );

        stage.setTitle("Inventory Manager");
        stage.setScene(scene);
        stage.show();

        updateStatusBar();
    }

    /* ================= TOOLBAR ================= */

    private ToolBar createToolbar() {

        Button export = new Button("Export CSV");
        Button delete = new Button("Delete");

        delete.setOnAction(e -> deleteProduct());
        export.setOnAction(e -> exportCSV());

        searchField.setPromptText("Search product...");
        searchField.textProperty()
                .addListener((a,b,c) -> search());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new ToolBar(
                delete,
                export,
                new Separator(),
                spacer,
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

        id.setStyle("-fx-alignment:CENTER;");
        id.setPrefWidth(60);



        /* ================= NAME COLUMN ================= */

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
            table.refresh();
        });

        name.setPrefWidth(420);



        /* ================= PRICE COLUMN ================= */

        TableColumn<Product,Double> price =
                new TableColumn<>("Price");

        price.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleDoubleProperty(
                        d.getValue().price()).asObject());

        price.setStyle("-fx-alignment:CENTER;");
        price.setPrefWidth(160);

        price.setCellFactory(col ->
                new TextFieldTableCell<>(
                        new javafx.util.converter.DoubleStringConverter()) {

                    @Override
                    public void updateItem(Double value, boolean empty) {

                        super.updateItem(value, empty);

                        if(empty || value == null)
                            setText(null);
                        else
                            setText(String.format("%.3f DT", value));
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
            table.refresh();
        });



        /* ================= QUANTITY COLUMN ================= */

        TableColumn<Product,Integer> qty =
                new TableColumn<>("Quantity");

        qty.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleIntegerProperty(
                        manager.getQuantity(d.getValue()))
                        .asObject());

        qty.setStyle("-fx-alignment:CENTER;");
        qty.setPrefWidth(120);

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

            manager.updateProduct(product.id(), newQty);

            refreshTable();
            table.refresh();
        });



        /* ================= STOCK LEVEL COLUMN ================= */

        TableColumn<Product, Product> stock =
                new TableColumn<>("Stock Level");

        stock.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));

        stock.setCellFactory(col -> new TableCell<>() {

            @Override
            protected void updateItem(Product product, boolean empty) {

                super.updateItem(product, empty);

                if(empty || product == null){
                    setGraphic(null);
                    return;
                }

                double progress =
                        manager.getStockRatio(product);

                ProgressBar bar =
                        new ProgressBar(progress);

                bar.setPrefWidth(140);

                double ratio = manager.getStockRatio(product);

                if(ratio <= 0.20)
                    bar.setStyle("-fx-accent:#e74c3c;");
                else if(ratio <= 0.50)
                    bar.setStyle("-fx-accent:#f1c40f;");
                else
                    bar.setStyle("-fx-accent:#3498db;");

                setGraphic(bar);
            }
        });

        stock.setPrefWidth(200);



        /* ================= TABLE CONFIG ================= */

        table.getColumns().setAll(
                id,
                name,
                price,
                qty,
                stock
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);



        /* =================Enable Double-Click Opening + ROW COLORING ================= */

        table.setRowFactory(tv -> {

            TableRow<Product> row = new TableRow<>() {

                @Override
                protected void updateItem(Product product, boolean empty) {

                    super.updateItem(product, empty);

                    getStyleClass().removeAll("critical", "warning");

                    if(product == null || empty)
                        return;

                    double ratio = manager.getStockRatio(product);

                    if(ratio <= 0.20)
                        getStyleClass().add("critical");
                    else if(ratio <= 0.50)
                        getStyleClass().add("warning");
                }
            };

            row.setOnMouseClicked(event -> {

                if(event.getClickCount() == 2 && !row.isEmpty()) {

                    Product product = row.getItem();

                    new ProductDetailsView(
                            manager,
                            product,
                            this::refreshTable
                    ).show();
                }
            });

            return row;
        });

        refreshTable();

        VBox box = new VBox(table);
        box.setPadding(new Insets(10));

        VBox.setVgrow(table, Priority.ALWAYS);

        return box;
    }

    /* ================= FORM ================= */

    private VBox createForm() {

        Label title = new Label("Add New Product");
        title.getStyleClass().add("form-title");

        nameField.setPromptText("Product Name");
        priceField.setPromptText("Price");
        qtyField.setPromptText("Initial Quantity");
        maxStockField.setPromptText("Max Capacity");

        descriptionArea.setPromptText("Product Description");
        descriptionArea.setPrefRowCount(4);

        nameField.setMaxWidth(Double.MAX_VALUE);
        priceField.setMaxWidth(Double.MAX_VALUE);
        qtyField.setMaxWidth(Double.MAX_VALUE);
        maxStockField.setMaxWidth(Double.MAX_VALUE);
        descriptionArea.setMaxWidth(Double.MAX_VALUE);

        Button save = new Button("Save Product");
        save.setMaxWidth(Double.MAX_VALUE);
        save.getStyleClass().add("save-button");

        save.setOnAction(e -> addProduct());

        VBox form = new VBox(
                12,
                title,
                nameField,
                priceField,
                qtyField,
                maxStockField,
                descriptionArea,
                save
        );

        form.setPadding(new Insets(20));
        form.setPrefWidth(260);
        form.getStyleClass().add("form-card");

        return form;
    }
    /* ================= STATUS BAR ================= */

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

        label.getStyleClass().add("status-label");

        HBox box = new HBox(label);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8,16,8,16));

        box.getStyleClass().add("status-card");

        box.setStyle(
                "-fx-background-color:" + bgColor + ";"
        );

        return box;
    }

    /* ================= STATUS UPDATE ================= */

    private void updateStatusBar() {

        var products =
                manager.getObservableProducts();

        int total = products.size();

        long critical =
                products.stream()
                        .filter(p -> manager.getStockRatio(p) <= 0.20)
                        .count();

        long warning =
                products.stream()
                        .filter(p -> {
                            double r = manager.getStockRatio(p);
                            return r > 0.20 && r <= 0.50;
                        })
                        .count();

        productsLabel.setText("Products: " + total);
        criticalLabel.setText("Critical: " + critical);
        warningLabel.setText("Warning: " + warning);
    }

    /* ================= LOGIC ================= */

    private void refreshTable() {
        table.setItems(manager.getObservableProducts());
        updateStatusBar();
        table.refresh();
    }

    private void addProduct() {

        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();
        String qtyText = qtyField.getText().trim();
        String maxText = maxStockField.getText().trim();
        String description = descriptionArea.getText().trim();

        if(name.isEmpty()){
            alert("Product name cannot be empty.");
            return;
        }

        if(name.length() < 3){
            alert("Product name must contain at least 3 characters.");
            return;
        }

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

        int maxStock;

        try {
            maxStock = Integer.parseInt(maxText);
        } catch (NumberFormatException e){
            alert("Max capacity must be a valid integer.");
            return;
        }

        if(maxStock <= 0){
            alert("Max capacity must be greater than 0.");
            return;
        }

        if(quantity > maxStock){
            alert("Initial quantity cannot exceed max capacity.");
            return;
        }

        int id = manager.generateProductId();

        manager.addProduct(
                new Product(id, name, price, maxStock, description),
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
        maxStockField.clear();
        descriptionArea.clear();
    }

    static void main(String... args){
        launch(args);
    }
}