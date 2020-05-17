package jv.fileserver;

import jv.http.HTTPRequest;
import jv.http.HTTPResponse;

public class APIHandler {

    private final HTTPRequest request;
    private HTTPResponse response = new HTTPResponse(501);

    public APIHandler(HTTPRequest request) {
        this.request = request;

        parseRequest();
    }

    public void parseRequest() {
        switch (request.getRequestMethod()) {
            case "GET": {
                response = new HTTPResponse(200);
                response.setData("Hello world!".getBytes());
                // https://www.tutorialspoint.com/http/http_responses.htm
                // https://stackoverflow.com/questions/20508788/do-i-need-content-type-application-octet-stream-for-file-download
            }
            case "PUT": {

            }
            case "DELETE": {

            }
            case "HEAD": {

            }
            default: {

            }
        }
    }

    public HTTPResponse getResponse() {
        return response;
    }

}
