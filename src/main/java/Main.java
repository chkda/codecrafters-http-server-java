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

            while (true){
                clientSocket = serverSocket.accept(); // Wait for connection from client.
                InputStream inputStream = clientSocket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                OutputStream outputStream = clientSocket.getOutputStream();

                ResponseHandler responseHandler = new ResponseHandler(bufferedReader, outputStream);

                Thread handlerThread = new Thread(responseHandler);
                handlerThread.start();
                System.out.println("accepted new connection");
            }


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
