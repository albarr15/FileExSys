import javax.xml.crypto.Data;
import java.net.*;
import java.sql.Connection;
import java.util.Scanner;
import java.io.*;

public class FileServer {
    private ServerSocket serverSocket;

    public FileServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void runServer() {
        try {
            while (!serverSocket.isClosed()) {
                // listen for incoming client connections
                Socket serverEndpoint = serverSocket.accept();

                System.out.println("Server: New client connected: " + serverEndpoint.getRemoteSocketAddress());
                ClientHandler clientHandler = new ClientHandler(serverEndpoint);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeServer() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        String ServerAddress = args[0];
        int nPort = Integer.parseInt(args[1]);
        System.out.println("Server: Assigned IP Address " + ServerAddress + "...");
        System.out.println("Server: Listening on port " + nPort + "...");

        ServerSocket serverSocket = new ServerSocket(nPort);
        FileServer server = new FileServer(serverSocket);

        server.runServer();
    }
}