package productService;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

public class Metrics {
    public static final Counter totalRequests = Counter.build()
            .name("product_service_requests_total")
            .help("Total number of requests to the product service")
            .register();

    public static final Counter productsAdded = Counter.build()
            .name("product_service_products_added_total")
            .help("Total number of products added to the product service")
            .register();

    public static final Counter productsFetched = Counter.build()
            .name("product_service_products_fetched_total")
            .help("Total number of products fetched from the product service")
            .register();

    public static final Counter productsUpdated = Counter.build()
            .name("product_service_products_updated_total")
            .help("Total number of products updated in the product service")
            .register();

    public static final Counter productsDeleted = Counter.build()
            .name("product_service_products_deleted_total")
            .help("Total number of products deleted from the product service")
            .register();

    public static final Counter healthChecks = Counter.build()
            .name("product_service_health_checks_total")
            .help("Total number of health checks to the product service")
            .register();

    public static final Histogram queryDuration = Histogram.build()
            .name("product_service_query_duration_seconds")
            .help("Duration of queries to the product service")
            .register();

    public static final Gauge activeConnections = Gauge.build()
            .name("database_active_connections")
            .help("Number of active database connections")
            .register();

    public static final Counter queryErrors = Counter.build()
            .name("database_query_errors")
            .help("Total number of database query errors")
            .register();

    public static final Counter requestFailures = Counter.build()
            .name("product_service_request_failures_total")
            .help("Total number of failed requests to the product service")
            .register();
}
