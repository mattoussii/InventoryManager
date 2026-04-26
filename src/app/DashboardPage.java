package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import model.Product;
import service.StockManager;

import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

public class DashboardPage extends VBox {

    public DashboardPage(StockManager manager) {

        setSpacing(25);
        setPadding(new Insets(25));

        Label title = new Label("Inventory Dashboard");
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold;");

        HBox cards = createMetricCards(manager);

        PieChart stockChart = createStockChart(manager);
        BarChart<String,Number> salesChart = createSalesBarChart();
        LineChart<String,Number> trendChart = createSalesTrendChart();

        GridPane charts = new GridPane();
        charts.setHgap(20);
        charts.setVgap(20);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);

        charts.getColumnConstraints().addAll(col1, col2);

        charts.add(stockChart,0,0);
        charts.add(salesChart,1,0);
        charts.add(trendChart,0,1,2,1);

        getChildren().addAll(title,cards,charts);
    }

    /* ================= METRIC CARDS ================= */

    private HBox createMetricCards(StockManager manager){

        int total = manager.getObservableProducts().size();

        long critical =
                manager.getObservableProducts()
                        .stream()
                        .filter(p -> manager.getStockRatio(p) <= 0.20)
                        .count();

        long warning =
                manager.getObservableProducts()
                        .stream()
                        .filter(p -> {
                            double r = manager.getStockRatio(p);
                            return r > 0.20 && r <= 0.50;
                        })
                        .count();

        long out =
                manager.getObservableProducts()
                        .stream()
                        .filter(p -> manager.getQuantity(p) == 0)
                        .count();

        double revenue = calculateRevenue();

        HBox box = new HBox(
                createCard("Products", String.valueOf(total)),
                createCard("Critical", String.valueOf(critical)),
                createCard("Warning", String.valueOf(warning)),
                createCard("Out of Stock", String.valueOf(out)),
                createCard("Revenue", String.format("%.2f DT", revenue))
        );

        box.setSpacing(20);

        return box;
    }

    private VBox createCard(String title,String value){

        Label t = new Label(title);
        t.setStyle("-fx-font-size:12px; -fx-text-fill:#777;");

        Label v = new Label(value);
        v.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        VBox box = new VBox(4,t,v);
        box.setAlignment(Pos.CENTER);

        box.setPadding(new Insets(10,18,10,18));

        box.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-radius:8;" +
                        "-fx-border-color:#e0e0e0;"
        );

        return box;
    }

    /* ================= PIE CHART ================= */

    private PieChart createStockChart(StockManager manager){

        int healthy = 0;
        int warning = 0;
        int critical = 0;
        int out = 0;

        for(Product p : manager.getObservableProducts()){

            int qty = manager.getQuantity(p);

            if(qty == 0){
                out++;
                continue;
            }

            double r = manager.getStockRatio(p);

            if(r <= 0.20)
                critical++;
            else if(r <= 0.50)
                warning++;
            else
                healthy++;
        }

        PieChart chart = new PieChart();

        chart.getData().addAll(
                new PieChart.Data("Healthy",healthy),
                new PieChart.Data("Warning",warning),
                new PieChart.Data("Critical",critical),
                new PieChart.Data("Out of Stock",out)
        );

        chart.getData().get(0).getNode().setStyle("-fx-pie-color: #3498db;");
        chart.getData().get(1).getNode().setStyle("-fx-pie-color: #f1c40f;");
        chart.getData().get(2).getNode().setStyle("-fx-pie-color: #e74c3c;");
        chart.getData().get(3).getNode().setStyle("-fx-pie-color: #2c3e50;");



        chart.setTitle("Stock Health");

        return chart;
    }

    /* ================= SALES BAR CHART ================= */

    private BarChart<String,Number> createSalesBarChart(){

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();

        BarChart<String,Number> chart = new BarChart<>(x,y);

        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setTitle("Top Selling Products");

        XYChart.Series<String,Number> series = new XYChart.Series<>();

        Map<String,Integer> sales = readSales();

        sales.entrySet()
                .stream()
                .sorted((a,b)->b.getValue()-a.getValue())
                .limit(5)
                .forEach(e->
                        series.getData().add(
                                new XYChart.Data<>(e.getKey(),e.getValue())
                        )
                );

        chart.getData().add(series);

        return chart;
    }

    /* ================= SALES TREND ================= */

    private LineChart<String,Number> createSalesTrendChart(){

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();

        LineChart<String,Number> chart = new LineChart<>(x,y);

        chart.setTitle("Sales Trend");
        chart.setLegendVisible(false);

        XYChart.Series<String,Number> series = new XYChart.Series<>();

        try (var conn = DriverManager.getConnection("JDBC:sqlite:inventory.db");
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                     "SELECT date, SUM(quantity) as qty FROM sales GROUP BY date")) {

            while(rs.next()){

                String date = rs.getString("date").split(" ")[0];
                int qty = rs.getInt("qty");

                series.getData().add(new XYChart.Data<>(date, qty));
            }

        } catch (Exception ignored){}

        chart.getData().add(series);

        return chart;
    }

    /* ================= SALES DATA ================= */

    private Map<String,Integer> readSales(){

        Map<String,Integer> sales = new HashMap<>();

        try (var conn = DriverManager.getConnection("jdbc:sqlite:inventory.db");
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                     "SELECT product_name, quantity FROM sales")) {

            while(rs.next()){

                String product = rs.getString("product_name");
                int qty = rs.getInt("quantity");

                sales.put(product,
                        sales.getOrDefault(product,0)+qty);
            }

        } catch (Exception ignored){}

        return sales;
    }

    /* ================= REVENUE ================= */

    private double calculateRevenue(){

        double revenue = 0;

        try (var conn = DriverManager.getConnection("jdbc:sqlite:inventory.db");
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT quantity, price FROM sales")) {

            while(rs.next()){
                revenue += rs.getInt("quantity") * rs.getDouble("price");
            }

        } catch (Exception ignored){}

        return revenue;
    }
}