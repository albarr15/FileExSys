import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlerList = new ArrayList<>();
    private Socket clientEndpoint;
    private DataInputStream disReader;
    private DataOutputStream dosWriter;
    private String clientHandle;

    public ClientHandler(Socket clientEndpoint) {
        this.clientEndpoint = clientEndpoint;
        try {
            this.disReader = new DataInputStream(this.clientEndpoint.getInputStream());
            this.dosWriter = new DataOutputStream(this.clientEndpoint.getOutputStream());
            clientHandlerList.add(this);
        } catch (IOException e) {
            closeAll();
        }
    }

    public void setClientHandle(String clientHandle) {
        this.clientHandle = clientHandle;
    }

    public void removeClientHandler() {
        clientHandlerList.remove(this);
    }

    public void closeAll() {
        removeClientHandler();

        try {
            if (disReader != null) disReader.close();
            if (dosWriter != null) dosWriter.close();
            if (clientEndpoint != null) {
                System.out.println("Client " + clientEndpoint.getRemoteSocketAddress() + " has disconnected.");
                clientEndpoint.close();
            }
        } catch (IOException e) {
            System.out.println("Error during disconnection: " + e.getMessage());
        } finally {
        disReader = null;
        dosWriter = null;
        clientEndpoint = null;
    }
    }

    private void printCommands() throws IOException {
        dosWriter.writeUTF(
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
                        "+-----------------------------------------------------------------------------+"
        );
        dosWriter.flush();
    }

    private void sendFile(String fileName) {
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

    private void receiveFile(String filename) {
        File serverDir = new File("serverDir");
        if (!serverDir.exists()) {
            serverDir.mkdir();
        }
    
        File file = new File(serverDir, filename);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            // Using the existing input stream from the client
            long fileSize = disReader.readLong(); // Read file size from existing disReader
            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalRead = 0;
    
            while (totalRead < fileSize && (bytesRead = disReader.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            bos.flush();
            System.out.println("Server: Uploaded " + filename);
        } catch (EOFException eof) {
            System.out.println("Client disconnected prematurely during file reception.");
            // Handle incomplete file reception if needed
        } catch (IOException e) {
            System.out.println("Error during file reception: " + e.getMessage());
        }
    }
    
    private void sendDirectoryList() {
        File serverDir = new File("serverDir");
        if (!serverDir.exists()) {
            serverDir.mkdir();
        }
    
        File[] files = serverDir.listFiles();
        if (files != null && files.length > 0) {
            StringBuilder fileList = new StringBuilder();
            for (File file : files) {
                fileList.append(file.getName()).append("\n");
            }
            try {
                dosWriter.writeUTF("Directory listing:\n" + fileList.toString());
                dosWriter.flush();

                System.out.println("Server: Sent directory listing to client.");
            } catch (IOException e) {
                System.out.println("Error sending directory list: " + e.getMessage());
            }
        } else {
            try {
                dosWriter.writeUTF("No files found in the directory.");
                dosWriter.flush();
            } catch (IOException e) {
                System.out.println("Error sending empty directory list: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void run() {
        String command;
        try {
            while (true) {
                command = disReader.readUTF(); // Read the command
                
                if (command == null || command.trim().isEmpty()) {
                    continue; // Skip if command is null or empty
                }

                String[] commandParts = command.split(" ");
                String mainCommand = commandParts[0];

                switch (mainCommand) {
                    case "/leave":
                        dosWriter.writeUTF("Connection closed. Thank you!");
                        dosWriter.flush();
                        closeAll();
                        return;
                        
                    case "/?":
                        printCommands();
                        break;
                        
                    case "/store":
                        if (commandParts.length > 1) {
                            String fileName = commandParts[1];
                            receiveFile(fileName);
                        } else {
                            dosWriter.writeUTF("Error: Missing filename for /store command.");
                            dosWriter.flush();
                        }
                        break;
                    
                    case "/dir":
                        sendDirectoryList();
                        break;

                    default:
                        dosWriter.writeUTF("Error: Command not found.");
                        dosWriter.flush();
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + clientEndpoint.getRemoteSocketAddress());
            // Handle exception or log it
        } finally {
            closeAll();
        }
    }

}
