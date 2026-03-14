package app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DatabaseTest {

    public static void main(String[] args) {

        try {

            Class.forName("org.sqlite.JDBC");

            Connection conn =
                    DriverManager.getConnection("jdbc:sqlite:inventory.db");

            Statement stmt = conn.createStatement();

            /* CREATE TABLES IF THEY DON'T EXIST */

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS products(
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        price REAL NOT NULL,
                        quantity INTEGER NOT NULL,
                        max_stock INTEGER,
                        description TEXT
                    );
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sales(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        product_id INTEGER,
                        product_name TEXT,
                        client_name TEXT,
                        client_phone TEXT,
                        quantity INTEGER,
                        price REAL,
                        date TEXT
                    );
                    """);

            /* IMPORT DATA FROM stock.txt */

            BufferedReader br = new BufferedReader(new FileReader("stock.txt"));

            String line;

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO products VALUES (?,?,?,?,?,?)"
            );

            while ((line = br.readLine()) != null) {

                String[] data = line.split(",");

                int id = Integer.parseInt(data[0]);
                String name = data[1];
                double price = Double.parseDouble(data[2]);
                int quantity = Integer.parseInt(data[3]);
                int maxStock = Integer.parseInt(data[4]);

                String description = "";

                if (data.length > 5) {
                    description = data[5];
                }

                ps.setInt(1, id);
                ps.setString(2, name);
                ps.setDouble(3, price);
                ps.setInt(4, quantity);
                ps.setInt(5, maxStock);
                ps.setString(6, description);

                ps.executeUpdate();
            }

            br.close();

            System.out.println("Products imported successfully.");

            conn.close();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}