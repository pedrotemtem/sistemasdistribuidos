package productService.gRPC;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

public class GrpcServer {
    public static void main(String[] args) throws IOException, InterruptedException, SQLException {

        try
        {
            Server server = ServerBuilder.forPort(8080)
                    .addService(new ProductServiceImpl())
                    .build();

            System.out.println("Server started, listening on 8080");
            server.start();
            server.awaitTermination();
        }
        catch (Exception e)
        {
            System.out.println("Server interrupted"+e.toString() + "\n" + Arrays.toString(e.getStackTrace()) );
        }
    }
}