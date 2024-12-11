package productService.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.prometheus.client.Histogram;
import productService.Metrics;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.sql.Statement;

public
class DatabaseManager {
    private static final String JDBC_URL = System.getenv("JDBC_URL");
    private static final String USER = System.getenv("JDBC_USER");
    private static final String PASSWORD = System.getenv("JDBC_PASSWORD");

    private final HikariDataSource dataSource;

    public DatabaseManager() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        // Adjust as needed
        config.setMinimumIdle(5);
        config.setConnectionTimeout(10000); // 10 seconds

        dataSource = new HikariDataSource(config);

        /*try {
            initializeDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing database", e);
        }*/
    }

    public Connection getConnection() throws SQLException {
        Metrics.activeConnections.inc();
        return dataSource.getConnection();
    }

    private void initializeDatabase() throws SQLException {
        Histogram.Timer timer = Metrics.queryDuration.startTimer();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create the products table if it doesn't exist
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "price DECIMAL, " +
                    "description VARCHAR(255), " +
                    "image VARCHAR(255)" +
                    ");");

            // Insert all the products
            stmt.execute(
                    "INSERT INTO products (id, name, price, description, image)\n" +
                    "VALUES \n" +
                    "    (1, 'Contoso Catnip''s Friend', 9.99, \n" +
                    "     'Watch your feline friend embark on a fishing adventure with Contoso Catnip''s Friend toy. Packed with irresistible catnip and dangling fish lure.', \n" +
                    "     '/catnip.jpg'),\n" +
                    "    (2, 'Salty Sailor''s Squeaky Squid', 6.99, \n" +
                    "     'Let your dog set sail with the Salty Sailor''s Squeaky Squid. This interactive toy provides hours of fun, featuring multiple squeakers and crinkle tentacles.', \n" +
                    "     '/squid.jpg'),\n" +
                    "    (3, 'Mermaid''s Mice Trio', 12.99, \n" +
                    "     'Entertain your kitty with the Mermaid''s Mice Trio. These adorable plush mice are dressed as mermaids and filled with catnip to captivate their curiosity.', \n" +
                    "     '/mermaid.jpg'),\n" +
                    "    (4, 'Ocean Explorer''s Puzzle Ball', 11.99, \n" +
                    "     'Challenge your pet''s problem-solving skills with the Ocean Explorer''s Puzzle Ball. This interactive toy features hidden compartments and treats, providing mental stimulation and entertainment.', \n" +
                    "     '/ocean.jpg'),\n" +
                    "    (5, 'Pirate Parrot Teaser Wand', 8.99, \n" +
                    "     'Engage your cat in a playful pursuit with the Pirate Parrot Teaser Wand. The colorful feathers and jingling bells mimic the mischievous charm of a pirate''s parrot.', \n" +
                    "     '/pirate.jpg'),\n" +
                    "    (6, 'Seafarer''s Tug Rope', 14.99, \n" +
                    "     'Tug-of-war meets nautical adventure with the Seafarer''s Tug Rope. Made from marine-grade rope, it''s perfect for interactive play and promoting dental health in dogs.', \n" +
                    "     '/tug.jpg'),\n" +
                    "    (7, 'Seashell Snuggle Bed', 19.99, \n" +
                    "     'Give your furry friend a cozy spot to curl up with the Seashell Snuggle Bed. Shaped like a seashell, this plush bed provides comfort and relaxation for cats and small dogs.', \n" +
                    "     '/bed.jpg'),\n" +
                    "    (8, 'Nautical Knot Ball', 7.99, \n" +
                    "     'Unleash your dog''s inner sailor with the Nautical Knot Ball. Made from sturdy ropes, it''s perfect for fetching, tugging, and satisfying their chewing needs.', \n" +
                    "     '/knot.jpg'),\n" +
                    "    (9, 'Contoso Claw''s Crabby Cat Toy', 3.99, \n" +
                    "     'Watch your cat go crazy for Contoso Claw''s Crabby Cat Toy. This crinkly and catnip-filled toy will awaken their hunting instincts and provide endless entertainment.', \n" +
                    "     '/crabby.jpg'),\n" +
                    "    (10, 'Ahoy Doggy Life Jacket', 5.99, \n" +
                    "     'Ensure your furry friend stays safe during water adventures with the Ahoy Doggy Life Jacket. Designed for dogs, this flotation device offers buoyancy and visibility in style.', \n" +
                    "     '/lifejacket.jpg')\n" +
                    "ON DUPLICATE KEY UPDATE \n" +
                    "    name = VALUES(name),\n" +
                    "    price = VALUES(price),\n" +
                    "    description = VALUES(description),\n" +
                    "    image = VALUES(image);\n");
        } finally {
            timer.observeDuration();
            Metrics.activeConnections.dec();
        }
    }

    public ResultSet executeQuery(String query) throws SQLException {
        Histogram.Timer timer = Metrics.queryDuration.startTimer();
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        try {
            return stmt.executeQuery(query);
        } finally {
            timer.close();
            Metrics.activeConnections.dec();
        }
    }
}