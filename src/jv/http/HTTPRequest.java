package jv.http;


import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

public class HTTPRequest {

    private final String requestLine;
    private final HashMap<String, String> headers = new HashMap<>(0);
    private byte[] data = new byte[]{};

    public HTTPRequest(DataInputStream inputStream) throws IOException {
        Scanner scanner = new Scanner(inputStream);
        String line;
        if (scanner.hasNextLine()) {
            line = scanner.nextLine();
            requestLine = line;
        } else {
            throw new IOException("Nothing to read from socket");
        }

        // Read headers
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (!line.equals("")) {
                int spaceIndex = line.indexOf(" ");
                headers.put(line.substring(0, spaceIndex - 1), line.substring(spaceIndex + 1));
            } else {
                break;
            }
        }

        if (headers.get("Content-Length") != null) {
            int count = Integer.parseInt(headers.get("Content-Length"));
            data = inputStream.readNBytes(count);
        } else if (headers.get("Transfer-Encoding") != null && headers.get("Transfer-Encoding").equals("chunked")) {
            // Read chunked data
            int count = readChunkSize(inputStream);
            while (count > 0) {
                byte[] buffer = inputStream.readNBytes(count);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(data);
                outputStream.write(buffer);
                data = outputStream.toByteArray();

                inputStream.readNBytes(2); // skip \r\n
                count = readChunkSize(inputStream);
            }
            inputStream.readNBytes(2);
        }
    }

    public String getHeaderValue(String key) {
        return headers.get(key);
    }

    private int readChunkSize(DataInputStream inputStream) throws IOException {
        Vector<Byte> bytes = new Vector<>(0);
        while (true) {
            byte b = inputStream.readByte();
            if (bytes.size() > 0) {
                if (bytes.lastElement() == 13 && b == 10) {
                    bytes.remove(bytes.lastElement());
                    break;
                }
            }
            bytes.add(b);
        }
        byte[] buffer = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            buffer[i] = bytes.elementAt(i);
        }
        return Integer.parseInt(new String(buffer),16);
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