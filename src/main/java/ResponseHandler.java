import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

public class ResponseHandler implements Runnable {

    BufferedReader reader;
    OutputStream writer;
    String directoryPath;
    Socket clientSocket;

    public ResponseHandler(Socket clientSocket, String directoryPath) {
        this.clientSocket = clientSocket;
        this.directoryPath = directoryPath;
    }

    @Override
    public void run() {
        try {
            boolean keepAlive = true;
            while (keepAlive) {
                InputStream inputStream = this.clientSocket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                OutputStream outputStream = this.clientSocket.getOutputStream();
                this.reader = bufferedReader;
                this.writer = outputStream;
                keepAlive = this.responseHandler();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            if (this.clientSocket != null) {
                try {
                    this.clientSocket.close();
                    System.out.println("client socker closed.");
                } catch (IOException e) {
                    System.out.println("IOException: " + e.getMessage());
                }
            }
        }
//        this.responseHandler();
    }

    void successResponseHandler(boolean connAlive) throws IOException {
        String httpSuccessResponse = "HTTP/1.1 200 OK\r\n";
        if (connAlive) {
            httpSuccessResponse += "Connection: close\r\n\r\n";
        } else {
            httpSuccessResponse += "\r\n";
        }
        this.writer.write(httpSuccessResponse.getBytes(StandardCharsets.UTF_8));
        this.writer.flush();
    }

    void notFoundResponseHandler(boolean connAlive) throws IOException {
        String httpNotFoundResponse = "HTTP/1.1 404 Not Found\r\n";
        if (connAlive) {
            httpNotFoundResponse += "Connection: close\r\n\r\n";
        } else {
            httpNotFoundResponse += "\r\n";
        }
        this.writer.write(httpNotFoundResponse.getBytes(StandardCharsets.UTF_8));
        this.writer.flush();
    }

    byte[] gzipEncode(String content) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        gzipOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    void successResponseHandler(String body, String contentType, String encoding, boolean connAlive) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String httpSuccessResponseWithBody = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n";
        if (connAlive) {
            httpSuccessResponseWithBody += "Connection: close\r\n";
        }
        if (encoding != null && encoding.equals("gzip")) {
            httpSuccessResponseWithBody += "Content-Encoding: gzip" + "\r\n";
            bodyBytes = this.gzipEncode(body);
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

    boolean responseHandler() throws IOException {

        String requestLine = this.reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            this.notFoundResponseHandler(false);
            return false;
        }

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            this.notFoundResponseHandler(false);
            return false;
        }

        String method = parts[0];
        String requestTarget = parts[1];
        HashMap<String, String> headers = this.extractHeaders();
        String connectionHeader = headers.get("connection");
        boolean keepAlive = !(connectionHeader != null && connectionHeader.equals("close")) ;

        String encodingHeader = headers.get("accept-encoding");
        String encoding = null;

        String requestBody = null;
        String contentLengthHeader = headers.get("content-length");
        int contentLength = 0;

        if (encodingHeader != null) {
            String[] encodings = encodingHeader.replace(" ", "").split(",");
            for (String enc : encodings) {
                if (enc.equals("gzip")) {
                    encoding = enc;
                    break;
                }
            }
        }


        if (contentLengthHeader != null) {
            try {
                contentLength = Integer.parseInt(contentLengthHeader);
            } catch (NumberFormatException e) {
                this.notFoundResponseHandler(false);
                return keepAlive;
            }
        }

        if (method.equals("POST") && contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int charsRead = this.reader.read(bodyChars, 0, contentLength);
            if (charsRead == contentLength) {
                requestBody = new String(bodyChars);
            } else {
                this.notFoundResponseHandler(keepAlive);
                return keepAlive;
            }
        }

        if (requestTarget == null) {
            this.notFoundResponseHandler(keepAlive);
        } else if (requestTarget.equals("/")) {
            this.successResponseHandler(keepAlive);
        } else if (requestTarget.startsWith("/echo/")) {
            String endpoint = requestTarget.replace("/echo/", "");
            this.successResponseHandler(endpoint, "text/plain", encoding, keepAlive);
        } else if (requestTarget.equals("/user-agent") && headers.containsKey("user-agent")) {
            String header = headers.get("user-agent");
            this.successResponseHandler(header, "text/plain", encoding, keepAlive);
        } else if (requestTarget.startsWith("/files/") && !requestTarget.replace("/files/", "").isEmpty()) {
            String fileName = requestTarget.replace("/files/", "");
            if (method.equals("GET")) {
                String fileContents = this.getFileContents(fileName);
                if (fileContents == null) {
                    this.notFoundResponseHandler(keepAlive);
                    return keepAlive;
                }
                this.successResponseHandler(fileContents, "application/octet-stream", encoding, keepAlive);
            } else if (method.equals("POST") && requestBody != null) {
                this.createFile(fileName, requestBody);
                this.createdResponseHandler();
            }
        } else {
            this.notFoundResponseHandler(keepAlive);
        }


        return keepAlive;

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
