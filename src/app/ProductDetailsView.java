package app;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import model.Product;
import service.StockManager;

public record ProductDetailsView(
        StockManager manager,
        Product product,
        Runnable refreshCallback
) {

    public void show() {

        Stage stage = new Stage();

        Label name = new Label("Name: " + product.name());
        Label price = new Label("Price: " + product.formattedPrice());

        Label quantity = new Label(
                "Current Stock: " + manager.getQuantity(product)
        );

        Label capacity = new Label(
                "Max Capacity: " + product.maxStock()
        );

        Label description = new Label(
                "Description: " + product.description()
        );

        Button sellButton = new Button("Sell Product");

        sellButton.setOnAction(e -> openSellForm());

        VBox layout = new VBox(
                10,
                name,
                price,
                quantity,
                capacity,
                description,
                new Separator(),
                sellButton
        );

        layout.setPadding(new Insets(20));

        Scene scene = new Scene(layout, 350, 260);

        stage.setTitle("Product Details");
        stage.setScene(scene);
        stage.show();
    }

    /* ===============================
       SELL FORM WINDOW
       =============================== */

    private void openSellForm() {

        Stage stage = new Stage();

        Label productLabel =
                new Label("Product: " + product.name());

        TextField clientNameField =
                new TextField();
        clientNameField.setPromptText("Client Name");

        TextField clientPhoneField =
                new TextField();
        clientPhoneField.setPromptText("Client Phone");

        TextField quantityField =
                new TextField();
        quantityField.setPromptText("Quantity");

        Button confirmButton =
                new Button("Confirm Sale");

        confirmButton.setMaxWidth(Double.MAX_VALUE);

        confirmButton.setOnAction(e -> {

            String clientName =
                    clientNameField.getText().trim();

            String clientPhone =
                    clientPhoneField.getText().trim();

            String qtyText =
                    quantityField.getText().trim();

            if(clientName.isEmpty()){
                alert("Client name required");
                return;
            }

            if(clientPhone.isEmpty()){
                alert("Client phone required");
                return;
            }

            int qty;

            try{
                qty = Integer.parseInt(qtyText);
            }
            catch(NumberFormatException ex){
                alert("Quantity must be a number");
                return;
            }

            boolean success =
                    manager.sellProduct(
                            product.id(),
                            qty,
                            clientName,
                            clientPhone
                    );

            if(success){

                new Alert(
                        Alert.AlertType.INFORMATION,
                        "Sale recorded successfully"
                ).showAndWait();

                /* refresh main table */
                refreshCallback.run();

                stage.close();

            }else{

                new Alert(
                        Alert.AlertType.ERROR,
                        "Not enough stock"
                ).showAndWait();
            }

        });

        VBox layout = new VBox(
                12,
                productLabel,
                clientNameField,
                clientPhoneField,
                quantityField,
                confirmButton
        );

        layout.setPadding(new Insets(20));

        Scene scene = new Scene(layout, 320, 220);

        stage.setTitle("Sell Product");
        stage.setScene(scene);
        stage.show();
    }

    private void alert(String msg){

        new Alert(
                Alert.AlertType.WARNING,
                msg
        ).showAndWait();
    }
}