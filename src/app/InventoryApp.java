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

    private final StackPane contentArea = new StackPane();

    private final TextField nameField = new TextField();
    private final TextField priceField = new TextField();
    private final TextField qtyField = new TextField();
    private final TextField searchField = new TextField();
    private final TextField maxStockField = new TextField();
    private final TextArea descriptionArea = new TextArea();

    private final Button productsButton = new Button();
    private final Button criticalButton = new Button();
    private final Button warningButton = new Button();
    private final Button outOfStockButton = new Button();

    @Override
    public void start(Stage stage) {

        manager.loadFromDatabase();

        BorderPane root = new BorderPane();

        root.setLeft(createSidebar());

        contentArea.getChildren().setAll(createProductsPage());

        root.setCenter(contentArea);

        Scene scene = new Scene(root, 1200, 650);

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

    /* ================= SIDEBAR ================= */

    private VBox createSidebar() {

        Button products = new Button("Products");
        Button sales = new Button("Sales");
        Button dashboard = new Button("Dashboard");

        products.setMaxWidth(Double.MAX_VALUE);
        sales.setMaxWidth(Double.MAX_VALUE);
        dashboard.setMaxWidth(Double.MAX_VALUE);

        products.setOnAction(e ->
                contentArea.getChildren().setAll(createProductsPage())
        );

        sales.setOnAction(e ->
                contentArea.getChildren().setAll(new SalesPage(manager))
        );

        dashboard.setOnAction(e ->
                contentArea.getChildren().setAll(new DashboardPage(manager))
        );

        VBox sidebar = new VBox(15, products, sales, dashboard);

        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color:#2c3e50;");

        return sidebar;
    }

    /* ================= PRODUCTS PAGE ================= */

    private BorderPane createProductsPage() {

        BorderPane page = new BorderPane();

        page.setTop(createToolbar());
        page.setCenter(createTable());
        page.setRight(createForm());
        page.setBottom(createStatusBar());

        return page;
    }

    /* ================= TOOLBAR ================= */

    private ToolBar createToolbar() {

        Button export = new Button("Export CSV");
        Button delete = new Button("Delete");

        delete.setOnAction(e -> deleteProduct());
        export.setOnAction(e -> exportCSV());

        searchField.setPromptText("Search product...");
        searchField.textProperty().addListener((a,b,c) -> search());

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

        table.setEditable(false);

        TableColumn<Product,Integer> id = new TableColumn<>("ID");

        id.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleIntegerProperty(
                        d.getValue().id()).asObject());

        id.setPrefWidth(60);



        TableColumn<Product,String> name = new TableColumn<>("Name");

        name.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleStringProperty(
                        d.getValue().name()));

        name.setPrefWidth(350);



        TableColumn<Product,Double> price = new TableColumn<>("Price");

        price.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleDoubleProperty(
                        d.getValue().price()).asObject());

        price.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {

                super.updateItem(value, empty);

                if(empty || value == null){
                    setText(null);
                    return;
                }

                setText(String.format("%.3f DT", value));
            }
        });

        price.setPrefWidth(120);



        TableColumn<Product,Integer> qty = new TableColumn<>("Quantity");

        qty.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleIntegerProperty(
                        manager.getQuantity(d.getValue()))
                        .asObject());

        qty.setCellFactory(
                TextFieldTableCell.forTableColumn(
                        new IntegerStringConverter()));

        qty.setOnEditCommit(event -> {

            Product product = event.getRowValue();
            int newQty = event.getNewValue();

            if(newQty < 0){
                alert("Quantity cannot be negative.");
                refreshTable();
                return;
            }

            manager.updateProduct(product.id(), newQty);
            refreshTable();
        });

        qty.setPrefWidth(120);



        TableColumn<Product, Product> stock = new TableColumn<>("Stock Level");

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

                double progress = manager.getStockRatio(product);
                int quantity = manager.getQuantity(product);

                ProgressBar bar = new ProgressBar(progress);

                bar.setPrefWidth(140);

                if(quantity == 0)
                    bar.setStyle("-fx-accent:#5a5a5a;");
                else if(progress <= 0.20)
                    bar.setStyle("-fx-accent:#e74c3c;");
                else if(progress <= 0.50)
                    bar.setStyle("-fx-accent:#f1c40f;");
                else
                    bar.setStyle("-fx-accent:#3498db;");

                setGraphic(bar);
            }
        });

        stock.setPrefWidth(180);



        table.getColumns().setAll(id, name, price, qty, stock);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);



        table.setRowFactory(tv -> {

            TableRow<Product> row = new TableRow<>() {

                @Override
                protected void updateItem(Product product, boolean empty) {

                    super.updateItem(product, empty);

                    getStyleClass().removeAll("critical","warning","out");

                    if(product == null || empty)
                        return;

                    int qty = manager.getQuantity(product);

                    if(qty == 0){
                        getStyleClass().add("out");
                        return;
                    }

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

                    contentArea.getChildren().setAll(
                            new ProductDetailsView(
                                    manager,
                                    product,
                                    () -> contentArea.getChildren().setAll(createProductsPage())
                            ).createView()
                    );
                }
            });

            return row;
        });

        refreshTable();

        VBox box = new VBox(table);
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

        Button save = new Button("Save Product");
        save.setMaxWidth(Double.MAX_VALUE);

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

        return form;
    }

    /* ================= STATUS BAR ================= */

    private HBox createStatusBar() {

        VBox productsCard = createStatusCard(productsButton, "Products", "status-products");
        VBox criticalCard = createStatusCard(criticalButton, "Critical", "status-critical");
        VBox warningCard = createStatusCard(warningButton, "Warning", "status-warning");
        VBox outCard = createStatusCard(outOfStockButton, "Out of Stock", "status-out");

        productsButton.setOnAction(e ->
                table.setItems(manager.getObservableProducts())
        );

        criticalButton.setOnAction(e ->
                table.setItems(
                        manager.getObservableProducts()
                                .filtered(p -> {
                                    double r = manager.getStockRatio(p);
                                    return r > 0 && r <= 0.20;
                                })
                )
        );

        warningButton.setOnAction(e ->
                table.setItems(
                        manager.getObservableProducts()
                                .filtered(p -> {
                                    double r = manager.getStockRatio(p);
                                    return r > 0.20 && r <= 0.50;
                                }))
        );

        outOfStockButton.setOnAction(e ->
                table.setItems(
                        manager.getObservableProducts()
                                .filtered(p ->
                                        manager.getQuantity(p) == 0))
        );

        HBox bar = new HBox(3,
                productsCard,
                criticalCard,
                warningCard,
                outCard
        );
        bar.setAlignment(Pos.CENTER_LEFT);

        bar.setPadding(new Insets(10));

        return bar;
    }
    private VBox createStatusCard(Button button, String title, String style) {

        Label label = new Label(title);
        label.getStyleClass().add("status-title");

        button.getStyleClass().add(style);
        button.getStyleClass().add("status-number");

        VBox box = new VBox(3, label, button);

        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("status-card");

        box.setPadding(new Insets(3,6,3,6));

        /* make entire card clickable */
        box.setOnMouseClicked(e -> button.fire());

        return box;
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

        confirm.setHeaderText(
                "Are you sure you want to delete this product?"
        );

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

        String text = searchField.getText().toLowerCase();

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

    private void clear(){

        nameField.clear();
        priceField.clear();
        qtyField.clear();
        maxStockField.clear();
        descriptionArea.clear();
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

    private void updateStatusBar() {

        var products = manager.getObservableProducts();

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

        long out =
                products.stream()
                        .filter(p -> manager.getQuantity(p) == 0)
                        .count();

        productsButton.setText(String.valueOf(total));
        criticalButton.setText(String.valueOf(critical));
        warningButton.setText(String.valueOf(warning));
        outOfStockButton.setText(String.valueOf(out));
    }
    static void main(String... args){
        launch(args);
    }
}