import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

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

            OutputStream outputStream = clientSocket.getOutputStream();

            responseHandler(bufferedReader, outputStream);

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
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String httpSuccessResponseWithBody = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n\r\n";
        writer.write(httpSuccessResponseWithBody.getBytes(StandardCharsets.UTF_8));
        writer.write(bodyBytes);
        writer.flush();
    }

    static HashMap<String, String> extractHeaders(BufferedReader reader) throws IOException{
        String headerline = reader.readLine();
        HashMap<String, String> hashMap = new HashMap<>();
        while (headerline != null && !headerline.isEmpty()) {
            int ind = headerline.indexOf(":");
            if (ind > 0) {
                String headerKey = headerline.substring(0, ind).toLowerCase();
                String headerValue = headerline.substring(ind+1).trim();
                hashMap.put(headerKey, headerValue);
            }
            headerline = reader.readLine();
        }
        return hashMap;
    }

    static void responseHandler(BufferedReader reader, OutputStream writer) throws IOException {
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            notFoundResponseHandler(writer);
            return;
        }

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            notFoundResponseHandler(writer);
            return;
        }

        String method = parts[0];
        String requestTarget = parts[1];
        HashMap<String, String> headers = extractHeaders(reader);
        if (requestTarget == null) {
            notFoundResponseHandler(writer);
        } else if (requestTarget.equals("/")) {
            successResponseHandler(writer);
        } else if (requestTarget.startsWith("/echo/")) {
            String endpoint = requestTarget.replace("/echo/", "");
            successResponseHandler(endpoint, writer);
        } else if (requestTarget.equals("/user-agent") && headers.containsKey("user-agent")) {
            String header = headers.get("user-agent");
            successResponseHandler(header, writer);
        } else {
            notFoundResponseHandler(writer);
        }

    }
}
