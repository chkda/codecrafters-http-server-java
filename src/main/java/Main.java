import java.io.*;
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
            InputStream inputStream = clientSocket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String requestLine = bufferedReader.readLine();

            String httpSuccessResponse = "HTTP/1.1 200 OK\r\n\r\n";
            String httpFailureResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            OutputStream outputStream = clientSocket.getOutputStream();

            if (requestLine != null && !requestLine.isEmpty()) {
                System.out.println("Request line recieved: " + requestLine);
                String[] parts = requestLine.split(" ");
                if (parts.length >= 2) {
                    String method = parts[0];
                    String requestTarget = parts[1];
                    if (requestTarget != null && requestTarget.equals("/")) {
                        outputStream.write(httpSuccessResponse.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    } else {
                        outputStream.write(httpFailureResponse.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                }
            } else {
                outputStream.write(httpFailureResponse.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
            
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
