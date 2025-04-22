import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");
        Socket clientSocket = null;
        ServerSocket serverSocket = null;

        // Uncomment this block to pass the first stage

        try {
            serverSocket = new ServerSocket(4221);

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            clientSocket = serverSocket.accept(); // Wait for connection from client.
            OutputStream outputStream = clientSocket.getOutputStream();
            String httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
            outputStream.write(httpResponse.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                    System.out.println("client socker closed.");
                } catch (IOException e) {
                    System.out.println("IOException: " + e.getMessage());
                }
            }
        }
    }
}
