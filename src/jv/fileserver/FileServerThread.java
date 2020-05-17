package jv.fileserver;

import jv.http.HTTPRequest;
import jv.http.HTTPResponse;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class FileServerThread extends Thread {

    private final Socket socket;

    public FileServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        super.run();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader((socket.getInputStream())));
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())){

            HTTPRequest request = new HTTPRequest(reader);
            HTTPResponse response = new APIHandler(request).getResponse();
            response.send(outputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}