import javax.xml.crypto.Data;
import java.net.*;
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

    private static void sendFile(String fileName, DataOutputStream dosWriter) {
        try {
            File file = new File(fileName);
            FileInputStream fileInStream = new FileInputStream(file);

            long numBytes = file.length();
            dosWriter.writeLong(numBytes);

            byte[] buffer = new byte[4096];
            int readBytes;

            System.out.println("Server: Sending file \"" + file.getName() + "\" (" + file.length() + " bytes)");

            while ((readBytes = fileInStream.read(buffer)) != -1) {
                dosWriter.write(buffer, 0, readBytes);
            }

            fileInStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String sServerAddress = args[0];
        int nPort = Integer.parseInt(args[1]);
        System.out.println("Server: Assigned IP Address " + args[0] + "...");
        System.out.println("Server: Listening on port " + args[1] + "...");

        ServerSocket serverSocket = new ServerSocket(nPort);
        FileServer server = new FileServer(serverSocket);

        server.runServer();
    }
}