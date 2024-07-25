import javax.xml.crypto.Data;
import java.net.*;
import java.io.*;

public class FileServer {

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

    private static void printCommands(DataOutputStream dosWriter) throws IOException {
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
    }

    public static void main(String[] args) {
        String sServerAddress = args[0];
        int nPort = Integer.parseInt(args[1]);
        System.out.println("Server: Assigned IP Address " + args[0] + "...");
        System.out.println("Server: Listening on port " + args[1] + "...");
        ServerSocket serverSocket;
        Socket serverEndpoint;

        try {
            serverSocket = new ServerSocket(nPort);
            serverEndpoint = serverSocket.accept();

            System.out.println("Server: New client connected: " + serverEndpoint.getRemoteSocketAddress());

            DataInputStream disReader = new DataInputStream(serverEndpoint.getInputStream());
            DataOutputStream dosWriter = new DataOutputStream(serverEndpoint.getOutputStream());

            while (true) {
                String command = disReader.readUTF();
                System.out.println("Client Input: " + command);

                switch (command) {
                    case "/?":
                        printCommands(dosWriter);
                        break;
                    case "/leave":
                        dosWriter.writeUTF("/leave");
                        break;
                    default:
                        dosWriter.writeUTF("Error: Command not found.\n");
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Server: Connection is terminated.");
        }
    }
}