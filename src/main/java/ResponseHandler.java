import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ResponseHandler implements Runnable {

    BufferedReader reader;
    OutputStream writer;

    public ResponseHandler(BufferedReader reader, OutputStream writer) {
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public void run() {
        this.responseHandler();
    }

    void successResponseHandler() throws IOException {
        String httpSuccessResponse = "HTTP/1.1 200 OK\r\n\r\n";
        this.writer.write(httpSuccessResponse.getBytes(StandardCharsets.UTF_8));
        this.writer.flush();
    }

    void notFoundResponseHandler() throws IOException {
        String httpNotFoundResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        this.writer.write(httpNotFoundResponse.getBytes(StandardCharsets.UTF_8));
        this.writer.flush();
    }

    void successResponseHandler(String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String httpSuccessResponseWithBody = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n\r\n";
        this.writer.write(httpSuccessResponseWithBody.getBytes(StandardCharsets.UTF_8));
        this.writer.write(bodyBytes);
        this.writer.flush();
    }

    HashMap<String, String> extractHeaders() throws IOException{
        String headerline = this.reader.readLine();
        HashMap<String, String> hashMap = new HashMap<>();
        while (headerline != null && !headerline.isEmpty()) {
            int ind = headerline.indexOf(":");
            if (ind > 0) {
                String headerKey = headerline.substring(0, ind).toLowerCase();
                String headerValue = headerline.substring(ind+1).trim();
                hashMap.put(headerKey, headerValue);
            }
            headerline = this.reader.readLine();
        }
        return hashMap;
    }

    void responseHandler()  {
        try {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                this.notFoundResponseHandler();
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                this.notFoundResponseHandler();
                return;
            }

            String method = parts[0];
            String requestTarget = parts[1];
            HashMap<String, String> headers = extractHeaders();
            if (requestTarget == null) {
                this.notFoundResponseHandler();
            } else if (requestTarget.equals("/")) {
                this.successResponseHandler();
            } else if (requestTarget.startsWith("/echo/")) {
                String endpoint = requestTarget.replace("/echo/", "");
                this.successResponseHandler(endpoint);
            } else if (requestTarget.equals("/user-agent") && headers.containsKey("user-agent")) {
                String header = headers.get("user-agent");
                this.successResponseHandler(header);
            } else {
                this.notFoundResponseHandler();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
