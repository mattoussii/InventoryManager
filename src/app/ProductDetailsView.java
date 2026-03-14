package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;

import model.Product;
import service.StockManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public record ProductDetailsView(
        StockManager manager,
        Product product,
        Runnable refreshCallback
) {

    private static final String ADMIN_PASSWORD = "123456789";

    private static Label quantity;

    private static ImageView imageView;

    public VBox createView() {

        /* ---------- BACK BUTTON ---------- */

        Button back = new Button("← Back");

        back.setOnAction(e -> refreshCallback.run());

        back.setStyle("""
                -fx-background-color:#34495e;
                -fx-text-fill:white;
                -fx-background-radius:6;
                """);

        /* ---------- PRODUCT IMAGE ---------- */

        imageView = new ImageView();

        imageView.setFitWidth(110);
        imageView.setFitHeight(110);
        imageView.setPreserveRatio(true);

        loadProductImage();

        StackPane imageBox = new StackPane(imageView);

        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPrefSize(120,120);

        imageBox.setStyle("""
                -fx-background-color:#f4f6f8;
                -fx-background-radius:10;
                """);

        Label dropText = new Label("Drop Image");

        VBox imageContainer = new VBox(6, imageBox, dropText);
        imageContainer.setAlignment(Pos.CENTER);

        enableDragAndDrop(imageContainer);

        /* ---------- PRODUCT INFO ---------- */

        Label name = new Label("Name: " + product.name());

        Label price = new Label(
                String.format("Price: %.3f DT", product.price())
        );

        quantity = new Label(
                "Stock: " + manager.getQuantity(product)
        );

        Label capacity = new Label(
                "Max Capacity: " + product.maxStock()
        );

        Label description = new Label(
                product.description() == null || product.description().isBlank()
                        ? "Description: -"
                        : "Description: " + product.description()
        );

        VBox info = new VBox(
                8,
                name,
                price,
                quantity,
                capacity,
                description
        );

        /* ---------- PRODUCT CARD ---------- */

        HBox card = new HBox(20, imageContainer, info);

        card.setPadding(new Insets(20));

        card.setStyle("""
                -fx-background-color:white;
                -fx-background-radius:10;
                -fx-border-color:#e0e0e0;
                -fx-border-radius:10;
                """);

        /* ---------- ACTION BUTTONS ---------- */

        Button addStock = new Button("+ Add Stock");
        Button removeStock = new Button("- Remove Stock");
        Button editProduct = new Button("Edit Product");

        addStock.setPrefWidth(180);
        removeStock.setPrefWidth(180);
        editProduct.setPrefWidth(180);

        addStock.setStyle("""
                -fx-background-color:#34495e;
                -fx-text-fill:white;
                """);

        removeStock.setStyle("""
                -fx-background-color:#c0392b;
                -fx-text-fill:white;
                """);

        editProduct.setStyle("""
                -fx-background-color:#2980b9;
                -fx-text-fill:white;
                """);

        addStock.setOnAction(e -> addStock());
        removeStock.setOnAction(e -> removeStock());
        editProduct.setOnAction(e -> openEditDialog());

        VBox actions = new VBox(
                12,
                addStock,
                removeStock,
                editProduct
        );

        actions.setPadding(new Insets(10));

        /* ---------- MAIN LAYOUT ---------- */

        VBox layout = new VBox(
                20,
                back,
                card,
                new Separator(),
                actions
        );

        layout.setPadding(new Insets(20));

        return layout;
    }

    /* ===============================
       IMAGE LOADING
       =============================== */

    private void loadProductImage() {

        try {

            File file = new File("src/resources/images/" + product.id() + ".png");

            if(file.exists()){

                Image img = new Image(
                        file.toURI().toString(),
                        false
                );

                imageView.setImage(img);

            } else {

                imageView.setImage(null);

            }

        } catch (Exception ignored) {}

    }

    /* ===============================
       DRAG & DROP IMAGE
       =============================== */

    private void enableDragAndDrop(VBox box){

        box.setOnDragOver(event -> {

            if(event.getDragboard().hasFiles()){
                event.acceptTransferModes(TransferMode.COPY);
            }

            event.consume();
        });

        box.setOnDragDropped(event -> {

            Dragboard db = event.getDragboard();

            if(db.hasFiles()){

                try{

                    File file = db.getFiles().get(0);

                    Path target = Path.of(
                            "src/resources/images/" + product.id() + ".png"
                    );

                    Files.copy(
                            file.toPath(),
                            target,
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    loadProductImage();

                }catch(Exception e){
                    showError("Failed to save image.");
                }

            }

            event.setDropCompleted(true);
            event.consume();
        });
    }

    /* ===============================
       PASSWORD CHECK
       =============================== */

    private boolean verifyPassword(){

        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Authorization Required");
        dialog.setHeaderText("Enter admin password");

        var result = dialog.showAndWait();

        return result.map(s -> s.equals(ADMIN_PASSWORD)).orElse(false);
    }

    /* ===============================
       EDIT PRODUCT
       =============================== */

    private void openEditDialog(){

        if(!verifyPassword()){
            showError("Wrong password.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();

        dialog.setTitle("Edit Product");

        TextField nameField = new TextField(product.name());
        TextField priceField = new TextField(String.valueOf(product.price()));
        TextField maxField = new TextField(String.valueOf(product.maxStock()));
        TextArea descArea = new TextArea(product.description());

        GridPane grid = new GridPane();

        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Name"),0,0);
        grid.add(nameField,1,0);

        grid.add(new Label("Price"),0,1);
        grid.add(priceField,1,1);

        grid.add(new Label("Max Stock"),0,2);
        grid.add(maxField,1,2);

        grid.add(new Label("Description"),0,3);
        grid.add(descArea,1,3);

        dialog.getDialogPane().setContent(grid);

        ButtonType save = new ButtonType(
                "Save",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                save,
                ButtonType.CANCEL
        );

        dialog.setResultConverter(btn -> {

            if(btn == save){

                try{

                    manager.updateProductFull(
                            product.id(),
                            nameField.getText(),
                            Double.parseDouble(priceField.getText()),
                            Integer.parseInt(maxField.getText()),
                            descArea.getText()
                    );

                    refreshCallback.run();

                }catch(Exception ex){
                    showError("Invalid values.");
                }

            }

            return null;
        });

        dialog.showAndWait();
    }

    /* ===============================
       ADD STOCK
       =============================== */

    private void addStock(){

        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Add Stock");

        dialog.showAndWait().ifPresent(v -> {

            try{

                int amount = Integer.parseInt(v);

                manager.adjustStock(product.id(), amount);

                quantity.setText(
                        "Stock: " + manager.getQuantity(product)
                );

                refreshCallback.run();

            }catch(Exception e){
                showError("Invalid number.");
            }

        });
    }

    /* ===============================
       REMOVE STOCK
       =============================== */

    private void removeStock(){

        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Remove Stock");

        dialog.showAndWait().ifPresent(v -> {

            try{

                int amount = Integer.parseInt(v);

                manager.adjustStock(product.id(), -amount);

                quantity.setText(
                        "Stock: " + manager.getQuantity(product)
                );

                refreshCallback.run();

            }catch(Exception e){
                showError("Invalid number.");
            }

        });
    }

    /* ===============================
       ERROR
       =============================== */

    private void showError(String msg){

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setHeaderText("Error");
        alert.setContentText(msg);

        alert.showAndWait();
    }
}