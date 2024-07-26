import java.io.*;
import java.net.Socket;

public class FileClient {

    private Socket clientEndpoint;
    private DataInputStream disReader;
    private DataOutputStream dosWriter;
    private BufferedReader stdIn;
    private String clientHandle;

    public FileClient(String serverAddress, int port) {
        try {
            this.clientEndpoint = new Socket(serverAddress, port);
            this.disReader = new DataInputStream(this.clientEndpoint.getInputStream());
            this.dosWriter = new DataOutputStream(this.clientEndpoint.getOutputStream());
            this.stdIn = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException e) {
            closeAll();
            throw new RuntimeException("Failed to initialize FileClient", e);
        }
    }

    public void setClientHandle(String clientHandle) {
        this.clientHandle = clientHandle;
    }

    public void sendCommand() {
        while (clientEndpoint.isConnected()) {
            try {
                String command = stdIn.readLine(); // Read user input
                if (command != null && !command.trim().isEmpty()) {
                    dosWriter.writeUTF(command);
                    dosWriter.flush();

                    // Process server response
                    String response = disReader.readUTF();
                    System.out.println("Server response: \n" + response);
                }

            } catch (IOException e) {
                System.out.println("Error in communication: " + e.getMessage());
            }
        }
        closeAll();
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

    private void closeAll() {
        try {
            if (stdIn != null) stdIn.close();
            if (disReader != null) disReader.close();
            if (dosWriter != null) dosWriter.close();
            if (clientEndpoint != null) clientEndpoint.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java FileClient <server-address> <port>");
            return;
        }

        String serverAddress = args[0];
        int port = Integer.parseInt(args[1]);

        FileClient client = new FileClient(serverAddress, port);
        client.sendCommand();
    }
}
