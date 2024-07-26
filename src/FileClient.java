import java.io.*;
import java.net.Socket;

public class FileClient {

    private Socket clientEndpoint;
    private DataInputStream disReader;
    private DataOutputStream dosWriter;
    private BufferedReader stdIn;
    private String clientHandle;

    public FileClient() {
        this.stdIn = new BufferedReader(new InputStreamReader(System.in));
    }

    public void setClientHandle(String clientHandle) {
        this.clientHandle = clientHandle;
    }

    public boolean connectToServer(String serverAddress, Integer port) throws IOException {
        try {
            this.clientEndpoint = new Socket(serverAddress, port);
            this.disReader = new DataInputStream(clientEndpoint.getInputStream());
            this.dosWriter = new DataOutputStream(clientEndpoint.getOutputStream());
            return true;
        }
        catch (IOException e) {
            // e.printStackTrace();
            return false;
        }
    }

    public void sendCommand() {
        while (true) {
            try {
                String command = stdIn.readLine(); // Read user input
                if (command != null && !command.trim().isEmpty()) {
                    String[] commandParts = command.split(" ");
                    String mainCommand = commandParts[0];

                    if (mainCommand.equals("/join")) {
                        handleJoinCommand(commandParts);
                    } else if (clientEndpoint != null && clientEndpoint.isConnected()) {
                        handleServerCommand(command);
                    } else {
                        System.out.println("Not connected to any server. Use /join to connect.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Error in reading command: " + e.getMessage());
                if (clientEndpoint != null && clientEndpoint.isConnected()) {
                    disconnect();
                    System.out.println("Client disconnected.");
                }
            }
        }
    }

    private void handleJoinCommand(String[] commandParts) throws IOException {
        if (commandParts.length >= 3) {
            String serverAddress = commandParts[1];
            int port = Integer.parseInt(commandParts[2]);
            if (!connectToServer(serverAddress, port)) {
                System.out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
            } else {
                System.out.println("Client connected successfully.");
            }
        } else {
            System.out.println("Usage: /join <server-address> <port>");
        }
    }

    private void handleServerCommand(String command) {
        if (command.equals("/leave")) {
            try {
                // Notify the server that the client wants to leave
                dosWriter.writeUTF(command);
                dosWriter.flush();
                // Perform client-side cleanup
                String response = disReader.readUTF();
                System.out.println("Server response: \n" + response);
                disconnect();
            } catch (IOException e) {
                System.out.println("Error during leaving: " + e.getMessage());
                disconnect();
            }
        } else {
            try {
                // Handle other commands
                dosWriter.writeUTF(command);
                dosWriter.flush();

                // Process server response
                String response = disReader.readUTF();
                System.out.println("Server response: \n" + response);
            } catch (IOException e) {
                System.out.println("Error during communication: " + e.getMessage());
                disconnect();
            }
        }
    }



    private void downloadFile(String fileName) {
        try {
            long fileSize = disReader.readLong();

            FileOutputStream fileOutStream = new FileOutputStream(fileName);
            byte[] buffer = new byte[4096];
            int readBytes;
            long total = 0;

            while ((total < fileSize) &&
                    (readBytes = disReader.read(buffer, 0, (int)Math.min(buffer.length, fileSize - total))) != -1) {
                fileOutStream.write(buffer, 0, readBytes);
                total += readBytes;
            }
            fileOutStream.close();

            if (total == fileSize) {
                System.out.println("Client: Downloaded file \"" + fileName + "\"");
            } else {
                System.out.println("Client: File download incomplete.");
            }
        } catch (IOException e) {
            System.out.println("Error downloading file: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            if (disReader != null) disReader.close();
            if (dosWriter != null) dosWriter.close();
            if (clientEndpoint != null) clientEndpoint.close();

            System.out.println("Client disconnected.");
        } catch (IOException e) {
            System.out.println("Error during disconnection: " + e.getMessage());
        }
        clientEndpoint = null;
    }

    public static void main(String[] args) {
        System.out.println("Client instantiated successfully.");

        FileClient client = new FileClient();
        client.sendCommand();
    }
}
