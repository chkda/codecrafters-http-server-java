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

            OutputStream outputStream = clientSocket.getOutputStream();

            if (requestLine == null || requestLine.isEmpty()) {
                notFoundResponseHandler(outputStream);
            } else {
                responseHandler(requestLine, outputStream);
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

    static void successResponseHandler(OutputStream writer) throws IOException {
        String httpSuccessResponse = "HTTP/1.1 200 OK\r\n\r\n";
        writer.write(httpSuccessResponse.getBytes(StandardCharsets.UTF_8));
        writer.flush();
    }

    static void notFoundResponseHandler(OutputStream writer) throws IOException {
        String httpNotFoundResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        writer.write(httpNotFoundResponse.getBytes(StandardCharsets.UTF_8));
        writer.flush();
    }

    static void successResponseHandler(String body, OutputStream writer) throws IOException {
        String httpSuccessResponseWithBody = "HTTP/1.1 200 OK\\r\\nContent-Type: text/plain\\r\\nContent-Length: 3\r\n\r\n" + body;
        writer.write(httpSuccessResponseWithBody.getBytes());
        writer.flush();
    }

    static void responseHandler(String requestLine, OutputStream writer) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length >= 2) {
            String method = parts[0];
            String requestTarget = parts[1];
            if (requestTarget == null) {
                notFoundResponseHandler(writer);
            } else if (requestTarget.equals("/")) {
                successResponseHandler(writer);
            } else if (requestTarget.startsWith("/echo/")) {
                String body = requestTarget.replace("/echo/", "");
                successResponseHandler(body, writer);
            }
        }
    }
}
