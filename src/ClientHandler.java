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
            if (clientEndpoint != null) clientEndpoint.close();
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

    @Override
    public void run() {
        String command;
        try {

        while (true) {
            command = disReader.readUTF();

                switch (command) {
                    case "/leave":
                        dosWriter.writeUTF("Connection closed. Thank you!");
                        dosWriter.flush();
                        closeAll();
                        return;
                    case "/?":
                        printCommands();
                        break;

                    default:
                        dosWriter.writeUTF("Error: Command not found.");
                        break;
                }
                dosWriter.flush();
        }} catch (IOException e) {
            System.out.println("Client disconnected: " + clientEndpoint.getRemoteSocketAddress());
            throw new RuntimeException(e);
        } finally {
            closeAll();
        }
    }
}
