package jv.fileserver;

import jv.http.HTTPRequest;
import jv.http.HTTPResponse;

import java.io.*;
import java.net.Socket;

public class FileServerThread extends Thread {

    private final Socket socket;
    private final String serverFilesFolderPath;

    public FileServerThread(Socket socket, String severFilesFolderPath) {
        this.socket = socket;
        this.serverFilesFolderPath = severFilesFolderPath;
    }

    @Override
    public void run() {
        super.run();

        try (DataInputStream inputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())){

            HTTPRequest request = new HTTPRequest(inputStream);
            HTTPResponse response = new APIHandler(request, serverFilesFolderPath).getResponse();
            response.send(outputStream);

        } catch (IOException e) {
            // Everything will be closed by try-with-resources
            // e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}