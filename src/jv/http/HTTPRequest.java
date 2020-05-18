package jv.http;


import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class HTTPRequest {

    private final String requestLine;
    private final HashMap<String, String> headers = new HashMap<>(0);
    private byte[] data = new byte[]{};

    public HTTPRequest(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Nothing to read from socket");
        }
        requestLine = line;

        // Read headers
        while ((line = reader.readLine()) != null && !line.equals("")) {
            int spaceIndex = line.indexOf(" ");
            headers.put(line.substring(0, spaceIndex-1), line.substring(spaceIndex+1));
        }

        // Read data

    }

    public void outputRequest() {
        System.out.println(requestLine);
        for(HashMap.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            System.out.println(key + ": " + value);
        }
    }

    public String getRequestMethod() {
        if (requestLine != null) {
            String[] split = requestLine.split(" ");
            return split[0];
        }
        return null;
    }

    public String getRelativePath() {
        if (requestLine != null) {
            String[] split = requestLine.split(" ");
            String path = split[1];
            try {
                URL url = new URL(path);
                path = url.getPath();
            } catch (MalformedURLException e) {
                // Do nothing. Path is already relative
            }
            return path;
        }
        return null;
    }

    public byte[] getData() {
        return data;
    }
}