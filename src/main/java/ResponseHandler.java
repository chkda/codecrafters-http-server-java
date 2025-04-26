import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class ResponseHandler implements Runnable {

    BufferedReader reader;
    OutputStream writer;
    String directoryPath;

    public ResponseHandler(BufferedReader reader, OutputStream writer, String directoryPath) {
        this.reader = reader;
        this.writer = writer;
        this.directoryPath = directoryPath;
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

    void successResponseHandler(String body, String contentType, String encoding) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String httpSuccessResponseWithBody = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n";
        if (encoding != null && encoding.equals("gzip")) {
            httpSuccessResponseWithBody += "Content-Encoding: gzip" + "\r\n";
        }
        httpSuccessResponseWithBody += "Content-Length: " + bodyBytes.length + "\r\n\r\n";
        this.writer.write(httpSuccessResponseWithBody.getBytes(StandardCharsets.UTF_8));
        this.writer.write(bodyBytes);
        this.writer.flush();
    }

    void createdResponseHandler() throws IOException {
        String httpCreatedResponse = "HTTP/1.1 201 Created\r\n\r\n";
        this.writer.write(httpCreatedResponse.getBytes(StandardCharsets.UTF_8));
        this.writer.flush();
    }

    HashMap<String, String> extractHeaders() throws IOException {
        String headerline = this.reader.readLine();
        HashMap<String, String> hashMap = new HashMap<>();
        while (headerline != null && !headerline.isEmpty()) {
            int ind = headerline.indexOf(":");
            if (ind > 0) {
                String headerKey = headerline.substring(0, ind).toLowerCase();
                String headerValue = headerline.substring(ind + 1).trim();
                hashMap.put(headerKey, headerValue);
            }
            headerline = this.reader.readLine();
        }
        return hashMap;
    }

    void responseHandler() {
        try {
            String requestLine = this.reader.readLine();
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
            HashMap<String, String> headers = this.extractHeaders();
            String encodingHeader = headers.get("accept-encoding");
            String encoding = null;

            if (encodingHeader != null) {
                String[] encodings = encodingHeader.replace(" ", "").split(",");
                for (String enc : encodings) {
                    if (enc.equals("gzip")) {
                        encoding = enc;
                        break;
                    }
                }
            }
            String requestBody = null;
            String contentLengthHeader = headers.get("content-length");
            int contentLength = 0;

            if (contentLengthHeader != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthHeader);
                } catch (NumberFormatException e) {
                    this.notFoundResponseHandler();
                    return;
                }
            }

            if (method.equals("POST") && contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int charsRead = this.reader.read(bodyChars, 0, contentLength);
                if (charsRead == contentLength) {
                    requestBody = new String(bodyChars);
                } else {
                    this.notFoundResponseHandler();
                    return;
                }
            }

            if (requestTarget == null) {
                this.notFoundResponseHandler();
            } else if (requestTarget.equals("/")) {
                this.successResponseHandler();
            } else if (requestTarget.startsWith("/echo/")) {
                String endpoint = requestTarget.replace("/echo/", "");
                this.successResponseHandler(endpoint, "text/plain", encoding);
            } else if (requestTarget.equals("/user-agent") && headers.containsKey("user-agent")) {
                String header = headers.get("user-agent");
                this.successResponseHandler(header, "text/plain", encoding);
            } else if (requestTarget.startsWith("/files/") && !requestTarget.replace("/files/", "").isEmpty()) {
                String fileName = requestTarget.replace("/files/", "");
                if (method.equals("GET")) {
                    String fileContents = this.getFileContents(fileName);
                    if (fileContents == null) {
                        this.notFoundResponseHandler();
                        return;
                    }
                    this.successResponseHandler(fileContents, "application/octet-stream", encoding);
                } else if (method.equals("POST") && requestBody != null) {
                    this.createFile(fileName, requestBody);
                    this.createdResponseHandler();
                }
            } else {
                this.notFoundResponseHandler();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    String getFileContents(String fileName) throws IOException {
        Path filePath = Paths.get(this.directoryPath + fileName);
        if (Files.notExists(filePath)) {
            return null;
        }
        return Files.readString(filePath);
    }

    void createFile(String filename, String fileContents) throws IOException {
        Path filePath = Paths.get(this.directoryPath + filename);
        Files.deleteIfExists(filePath);
        Files.createFile(filePath);
        Files.writeString(filePath, fileContents);
    }
}
