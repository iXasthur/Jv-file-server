package jv.fileserver;

import jv.http.HTTPRequest;
import jv.http.HTTPResponse;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
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

    public String getDirectoryStructureXML(String relativePath) throws IOException, ParserConfigurationException, TransformerException {
        String filePathString = serverFilesFolderPath + request.getRelativePath();
        Path filePath = Paths.get(filePathString);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.newDocument();

        Element rootElement = document.createElement("root");
        document.appendChild(rootElement);

        Attr pathAttribute = document.createAttribute("path");
        pathAttribute.setValue(relativePath);
        rootElement.setAttributeNode(pathAttribute);

        Files.list(filePath)
                .forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        Element element = document.createElement("file");
                        element.appendChild(document.createTextNode(String.valueOf(path.getFileName())));
                        rootElement.appendChild(element);
                    } else if (Files.isDirectory(path)) {
                        Element element = document.createElement("dir");
                        element.appendChild(document.createTextNode(String.valueOf(path.getFileName())));
                        rootElement.appendChild(element);
                    }
                });


        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));

        return writer.getBuffer().toString();
    }

    synchronized public void parseRequest() {
        String method = request.getRequestMethod();
        String filePathString = serverFilesFolderPath + request.getRelativePath();
        Path filePath = Paths.get(filePathString);

        switch (method) {
            case "GET": {
                // curl -v localhost:8080

                // If folder passed, return its structure
                // If file - return file with download headers
                if (Files.exists(filePath)) {
                    // Send file
                    if (Files.isRegularFile(filePath)) {
                        response = new HTTPResponse(500);

                        try {
                            byte[] bytes = Files.readAllBytes(filePath);
                            String fileName = filePath.getFileName().toString();
                            String fileExtension = "";
                            if (fileName.contains(".")) {
                                int index = fileName.lastIndexOf(".");
                                if (index < fileName.length() -1) {
                                    fileExtension = fileName.substring(index + 1);
                                }
                            }
                            response = new HTTPResponse(200);
                            response.setData(bytes, fileExtension);
                            response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                        } catch (IOException e) {
//                            e.printStackTrace();
                        }
                    } else if (Files.isDirectory(filePath)) {
                        // Send directory structure
                        response = new HTTPResponse(500);

                        try {
                            String xmlString = getDirectoryStructureXML(request.getRelativePath());
                            response = new HTTPResponse(200);
                            response.setData(xmlString.getBytes(), "txt");
                        } catch (IOException | ParserConfigurationException | TransformerException e) {
//                            e.printStackTrace();
                        }
                    }
                } else {
                    response = new HTTPResponse(404);
                    response.setData("File does not exist".getBytes(), "txt");
                }
                break;
            }
            case "PUT": {
                // curl localhost:8080/uploadFolder/uploaded.txt --upload-file upload.txt

                response = new HTTPResponse(500);
                try {
                    Files.createDirectories(filePath.getParent());
                    Files.createFile(filePath);
                    try (FileOutputStream stream = new FileOutputStream(filePathString)) {
                        stream.write(request.getData());
                    }
                    response = new HTTPResponse(201);
                    response.setData("Created new file".getBytes(), "txt");
                } catch (IOException e) {
//                    e.printStackTrace();
                }

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
        response.addHeader("Connection", "Closed");
    }

    public HTTPResponse getResponse() {
        return response;
    }

}
