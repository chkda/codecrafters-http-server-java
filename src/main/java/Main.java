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
            String directory = null;
            for (int i=0 ; i< args.length; i++) {
                if (args[i].equals("--directory")) {
                    directory = args[i+1];
                }
            }

            serverSocket = new ServerSocket(4221);

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            while (true){
                clientSocket = serverSocket.accept(); // Wait for connection from client.
                ResponseHandler responseHandler = new ResponseHandler(clientSocket, directory);

                Thread handlerThread = new Thread(responseHandler);
                handlerThread.start();
                System.out.println("accepted new connection");
            }


        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }


}
