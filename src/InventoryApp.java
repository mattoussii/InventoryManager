import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

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


        Button delete = new Button("Delete");
        Button refresh = new Button("Refresh");

        delete.setOnAction(e -> deleteProduct());
        refresh.setOnAction(e -> refreshTable());

        searchField.setPromptText("Search product...");
        searchField.textProperty()
                .addListener((a,b,c)->search());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new ToolBar(
                 delete,
                new Separator(),

                searchField
        );
    }

    /* ================= TABLE ================= */

    private VBox createTable() {

        table.setEditable(true);

        TableColumn<Product,Integer> id =
                new TableColumn<>("ID");

        id.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleIntegerProperty(
                        d.getValue().id()).asObject());

        TableColumn<Product,String> name =
                new TableColumn<>("Name");

        name.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleStringProperty(
                        d.getValue().name()));

        TableColumn<Product,String> price =
                new TableColumn<>("Price");

        price.setCellValueFactory(d ->
                new javafx.beans.property
                        .SimpleStringProperty(
                        d.getValue().formattedPrice()));

        TableColumn<Product,Integer> qty =
                new TableColumn<>("Qty");

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

        table.getColumns().setAll(id,name,price,qty);

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);

        /* ===== ROW COLORING ===== */

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

        try {

            int id = manager.generateProductId();

            manager.addProduct(
                    new Product(
                            id,
                            nameField.getText(),
                            Double.parseDouble(
                                    priceField.getText())),
                    Integer.parseInt(
                            qtyField.getText()));

            refreshTable();
            clear();

        } catch(Exception e){
            alert("Invalid input");
        }
    }

    private void deleteProduct() {

        Product p =
                table.getSelectionModel()
                        .getSelectedItem();

        if(p==null){
            alert("Select product");
            return;
        }

        manager.deleteProduct(p.id());
        refreshTable();
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