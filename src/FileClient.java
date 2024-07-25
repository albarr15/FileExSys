import java.net.*;
import java.io.*;
import java.util.Scanner;

public class FileClient {
    private static void downloadFile(String fileName, DataInputStream disReader) {
        try {
            long fileSize = disReader.readLong();

            FileOutputStream fileOutStream = new FileOutputStream(fileName);
            byte[] buffer = new byte[4096];
            int readBytes;
            long total = 0;

            while ((total < fileSize) &&
                    (readBytes = disReader.read(buffer, 0, (int)Math.min(buffer.length, fileSize-total))) != -1 ) {
                fileOutStream.write(buffer, 0, readBytes);
                total += readBytes;
            }
            fileOutStream.close();

            if (total == fileSize) {
                System.out.println("Client: Downloaded file \"" + fileName + "\"");
            } else {
                System.out.println("Client: File download incomplete.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // server address and port is predefined when running for now
        String sServerAddress = args[0];
        int nPort = Integer.parseInt(args[1]);

        try {
            Socket clientEndpoint = new Socket(sServerAddress, nPort);

            System.out.println("Client: Connected to server at " + clientEndpoint.getRemoteSocketAddress());

            DataOutputStream dosWriter = new DataOutputStream(clientEndpoint.getOutputStream());
            DataInputStream disReader = new DataInputStream(clientEndpoint.getInputStream());
            Scanner scanner = new Scanner(System.in);

            boolean isExit = false;
            while (!isExit) {
                String command = scanner.nextLine();

                dosWriter.writeUTF(command);

                String response = disReader.readUTF();

                switch (response) {
                    case "/leave":
                        isExit = true;
                        break;
                    default:
                        System.out.println("Server response: \n" + response);
                        break;
                }
            }
            scanner.close();
            disReader.close();
            dosWriter.close();
            clientEndpoint.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Connection closed. Thank you!");
            System.out.println("Client: Connection is terminated.");
        }
    }
}