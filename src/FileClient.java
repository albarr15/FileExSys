import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileClient {

    private Socket clientEndpoint;
    private DataInputStream disReader;
    private DataOutputStream dosWriter;
    private BufferedReader stdIn;
    private String clientHandle;

    private boolean isRegistered = false;

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
                System.out.println("\nEnter command: ");
                String command = stdIn.readLine(); // Read user input
                if (command != null && !command.trim().isEmpty()) {
                    String[] commandParts = command.split(" ");
                    String mainCommand = commandParts[0];

                    if (mainCommand.equals("/?")) {
                        String commands =
                        "+-------------------------- LIST OF COMMANDS ---------------------------------+\n" +
                        "| Input Syntax                    |   Description                             |\n" +
                        "+---------------------------------+-------------------------------------------+\n" +
                        "| /join <server_ip_add> <port>    | Connect to the server application         |\n" +
                        "| /leave                          | Disconnect to the server application      |\n" +
                        "| /register <handle>              | Register a unique handle or alias         |\n" +
                        "| /store <filename>               | Send file to server                       |\n" +
                        "| /dir                            | Request directory file list from a server |\n" +
                        "| /?                              | Request command help to output all Input  |\n" +
                        "|                                 | Syntax commands for references            |\n" +
                        "+-----------------------------------------------------------------------------+";

                        System.out.println(commands);
                    }
                    
                    else if (mainCommand.equals("/join")) {
                        handleJoinCommand(commandParts);
                    }
                    
                    else if (mainCommand.equals("/store") && commandParts.length > 1) {
                        if(isRegistered) {
                            String fileName = commandParts[1];
                            handleStoreCommand(fileName);
                        }
                        else {
                            System.out.println("Error: You need to register first with /register <handle>");
                        }
                    }
                    
                    else if (mainCommand.equals("/dir")) {
                        handleDirCommand();
                    }

                    else if (mainCommand.equals("/register") && commandParts.length > 1) {
                        String handle = commandParts[1];
                        handleRegisterCommand(handle);
                    }

                    else if (clientEndpoint != null && clientEndpoint.isConnected()) {
                        handleServerCommand(command);
                    }

                    else {
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

    private void handleStoreCommand(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("Error: File not found.");
                return;
            }
    
            // Send the command and filename to the server
            dosWriter.writeUTF("/store " + fileName);
            dosWriter.flush();
    
            // Send the file to the server
            try (FileInputStream fileInStream = new FileInputStream(file)) {
                long fileSize = file.length();
                dosWriter.writeLong(fileSize);
                dosWriter.flush();
    
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInStream.read(buffer)) != -1) {
                    dosWriter.write(buffer, 0, bytesRead);
                }
    
                dosWriter.flush();

                 // Get the current timestamp
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String timestamp = now.format(formatter);

                System.out.println("User" + "<" + timestamp + ">: Uploaded " + fileName);
            }
        } catch (IOException e) {
            System.out.println("Error during file sending: " + e.getMessage());
        }
    }
    
    private void handleDirCommand() {
        try {
            // Send the /dir command to the server
            dosWriter.writeUTF("/dir");
            dosWriter.flush();
    
            // Read the server's response (list of files)
            String response = disReader.readUTF();
            System.out.println("Server response: \n" + response);
        } catch (IOException e) {
            System.out.println("Error during directory listing: " + e.getMessage());
        }
    }

    private void handleRegisterCommand(String handle) {
        try {
            dosWriter.writeUTF("/register " + handle);
            dosWriter.flush();
    
            String response = disReader.readUTF();
    
            if (response.contains("Registration successful.")) {
                isRegistered = true; // Set isRegistered to true if registration is successful
                System.out.println("Registration successful.");
            } else {
                System.out.println("Registration failed: " + response);
            }
        } catch (IOException e) {
            System.out.println("Error during registration: " + e.getMessage());
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

        
        String commands =
        "+-------------------------- LIST OF COMMANDS ---------------------------------+\n" +
        "| Input Syntax                    |   Description                             |\n" +
        "+---------------------------------+-------------------------------------------+\n" +
        "| /join <server_ip_add> <port>    | Connect to the server application         |\n" +
        "| /leave                          | Disconnect to the server application      |\n" +
        "| /register <handle>              | Register a unique handle or alias         |\n" +
        "| /store <filename>               | Send file to server                       |\n" +
        "| /dir                            | Request directory file list from a server |\n" +
        "| /get <filename>                 | Request a file from a server              |\n" +
        "| /?                              | Request command help to output all Input  |\n" +
        "|                                 | Syntax commands for references            |\n" +
        "+-----------------------------------------------------------------------------+";

        System.out.println(commands);
        

        client.sendCommand();
    }
}
