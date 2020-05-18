package jv.fileserver;

import jv.http.HTTPRequest;
import jv.http.HTTPResponse;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class APIHandler {

    private final String serverFilesFolderPath;

    private final HTTPRequest request;
    private HTTPResponse response = new HTTPResponse(501);

    public APIHandler(HTTPRequest request, String serverFilesFolderPath) {
        this.request = request;
        this.serverFilesFolderPath = serverFilesFolderPath;

        parseRequest();
    }

    synchronized public void parseRequest() {
        String method = request.getRequestMethod();
        switch (method) {
            case "GET": {
                // If folder passed, return its structure
                // If file - return file with download headers
                String filePathString = serverFilesFolderPath + request.getRelativePath();
                Path filePath = Paths.get(filePathString);
                if (Files.exists(filePath)) {
                    response = new HTTPResponse(200);

                    if (Files.isRegularFile(filePath)) {
                        response.setData("FILE!".getBytes(), "txt");
                    } else if (Files.isDirectory(filePath)) {
                        response.setData("DIRECTORY!".getBytes(), "txt");
                    }
                    //                    response.addHeader("Content-Disposition", "attachment; filename=\"helloWorld.txt\"");
                } else {
                    response = new HTTPResponse(404);
                }
                response.addHeader("Connection", "Closed");
                break;
            }
            case "PUT": {
                break;
            }
            case "DELETE": {
                break;
            }
            case "HEAD": {
                break;
            }
            default: {
                break;
            }
        }
    }

    public HTTPResponse getResponse() {
        return response;
    }

}
