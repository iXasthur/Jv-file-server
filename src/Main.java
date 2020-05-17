import jv.fileserver.FileServerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) {

        // curl localhost:8080 --output jvOut.txt

        // Set 8080 as default value in IDE

        if (args.length != 1) {
            System.out.println("Invalid args. Args: <port>");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid args. Args: <port>");
            return;
        }

        // The Java runtime automatically closes the input and output streams, the client socket,
        // and the server socket because they have been created in the try-with-resources statement.
        // + JVM/OS will close everything on exit
        try (ServerSocket server = new ServerSocket(port)) {

            System.out.println("Created file server on localhost:" + port);

            while (true) {
                Socket socket = server.accept();

                FileServerThread thread = new FileServerThread(socket);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
