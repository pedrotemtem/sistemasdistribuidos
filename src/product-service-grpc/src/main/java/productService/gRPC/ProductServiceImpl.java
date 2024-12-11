package productService.gRPC;

import io.prometheus.client.Histogram;
import org.slf4j.LoggerFactory;
import productService.Metrics;
import productService.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;


import io.grpc.stub.StreamObserver;
import productService.gRPC.ProductServiceGrpc.ProductServiceImplBase;

public class ProductServiceImpl extends ProductServiceImplBase {

    private static final Logger logger = Logger.getLogger(ProductServiceImpl.class.getName());
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final DatabaseManager dbManager;


    public ProductServiceImpl() throws SQLException {
        dbManager = new DatabaseManager();
    }

    @Override
    public void healthCheck(Empty request, StreamObserver<HealthResponse> responseObserver) {
        Metrics.totalRequests.inc();
        Metrics.healthChecks.inc();
        HealthResponse response = HealthResponse.newBuilder()
                .setHealthy(true)
                .setMessage("Product service is healthy")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getProducts(Empty request, StreamObserver<ProductList> responseObserver) {
        Metrics.totalRequests.inc();

        logger.info("Total requests: " + Metrics.totalRequests.get());
        Histogram.Timer timer = Metrics.queryDuration.startTimer();

        try (
            Connection conn = dbManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM products")
        ) {
            logger.info("Received request to fetch all products");

            try (ResultSet rs = stmt.executeQuery()) {
                ProductList.Builder productList = ProductList.newBuilder();
                while (rs.next()) {
                    Product product = Product.newBuilder()
                            .setId(rs.getInt("id"))
                            .setName(rs.getString("name"))
                            .setPrice(rs.getFloat("price"))
                            .setDescription(rs.getString("description"))
                            .setImage(rs.getString("image"))
                            .build();

                    productList.addProducts(product);
                }
                Metrics.productsFetched.inc();
                responseObserver.onNext(productList.build());
                responseObserver.onCompleted();
            }
        } catch (SQLException e) {
            logger.severe("Error fetching products: " + e.getMessage());
            Metrics.queryErrors.inc();
            responseObserver.onError(e);
        } finally {
            timer.close();
            Metrics.activeConnections.dec();
        }
    }

    @Override
    public void addProduct(Product request, StreamObserver<ProductResponse> responseObserver) {
        Metrics.totalRequests.inc();
        Histogram.Timer timer = Metrics.queryDuration.startTimer();

        try (
            Connection conn = dbManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO products (name, price, description, image) VALUES (?, ?, ?, ?)")
        ) {
            logger.info("Attempting to add product: " + request);

            stmt.setString(1, request.getName());
            stmt.setFloat(2, request.getPrice());
            stmt.setString(3, request.getDescription());
            stmt.setString(4, request.getImage());
            int rowsInserted = stmt.executeUpdate();

            if (rowsInserted > 0) {
                logger.info("Product added successfully: " + request);
                ProductResponse response = ProductResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Product added successfully")
                        .build();
                responseObserver.onNext(response);
                Metrics.productsAdded.inc();
            } else {
                logger.warning("Failed to add product: " + request);
                ProductResponse response = ProductResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Failed to add product")
                        .build();
                Metrics.requestFailures.inc();
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
        } catch (SQLException e) {
            logger.severe("Error adding product: " + e.getMessage());
            Metrics.queryErrors.inc();
            responseObserver.onError(e);
        } finally {
            timer.close();
            Metrics.activeConnections.dec();
        }
    }

    @Override
public void updateProduct(Product request, StreamObserver<ProductResponse> responseObserver) {
    Metrics.totalRequests.inc();
    Histogram.Timer timer = Metrics.queryDuration.startTimer();

    log.info("Received request to update product: {}", request);

    try (
        Connection conn = dbManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement("UPDATE products SET name = ?, price = ?, description = ?, image = ? WHERE id = ?")
    ) {
        stmt.setString(1, request.getName());
        stmt.setFloat(2, request.getPrice());
        stmt.setString(3, request.getDescription());
        stmt.setString(4, request.getImage());
        stmt.setInt(5, request.getId());

        int rowsUpdated = stmt.executeUpdate();
        if (rowsUpdated > 0) {
            log.info("Product updated successfully: {}", request);
            ProductResponse response = ProductResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Product updated successfully")
                    .build();
            Metrics.productsUpdated.inc();
            responseObserver.onNext(response);
        } else {
            log.warn("Product not found: {}", request);
            ProductResponse response = ProductResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Product not found")
                    .build();
            Metrics.requestFailures.inc();
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    } catch (SQLException e) {
        log.error("Error updating product: {}", request, e);
        Metrics.queryErrors.inc();
        responseObserver.onError(e);
    } finally {
        timer.close();
        Metrics.activeConnections.dec();
    }
}

    @Override
    public void getProductById(ProductRequest request, StreamObserver<Product> responseObserver) {
        Metrics.totalRequests.inc();
        Histogram.Timer timer = Metrics.queryDuration.startTimer();

        log.info("Received request to get product by ID: {}", request.getId());
        try (
            Connection conn = dbManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM products WHERE id = ?")
        ) {
            stmt.setInt(1, request.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Product product = Product.newBuilder()
                            .setId(rs.getInt("id"))
                            .setName(rs.getString("name"))
                            .setPrice(rs.getFloat("price"))
                            .setDescription(rs.getString("description"))
                            .setImage(rs.getString("image"))
                            .build();
                    log.info("Product found: {}", product);
                    responseObserver.onNext(product);
                    Metrics.productsFetched.inc();
                } else {
                    log.warn("Product not found with ID: {}", request.getId());
                    Metrics.requestFailures.inc();
                    responseObserver.onError(new RuntimeException("Product not found"));
                }
            }
            responseObserver.onCompleted();
        } catch (SQLException e) {
            log.error("Error fetching product by ID: {}", request.getId(), e);
            Metrics.queryErrors.inc();
            responseObserver.onError(e);
        } finally {
            timer.close();
            Metrics.activeConnections.dec();
        }
    }

    @Override
    public void deleteProduct(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        Metrics.totalRequests.inc();
        Histogram.Timer timer = Metrics.queryDuration.startTimer();

        try (
            Connection conn = dbManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")
        ) {
            stmt.setInt(1, request.getId());

            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                ProductResponse response = ProductResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Product deleted successfully")
                        .build();
                responseObserver.onNext(response);
            } else {
                ProductResponse response = ProductResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Product not found")
                        .build();
                Metrics.requestFailures.inc();
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
            Metrics.productsDeleted.inc();
        } catch (SQLException e) {
            Metrics.queryErrors.inc();
            responseObserver.onError(e);
        } finally {
            timer.close();
            Metrics.activeConnections.dec();
        }
    }
}
