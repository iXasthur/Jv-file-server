package jv.fileserver;

import jv.http.HTTPContentType;
import jv.http.HTTPRequest;
import jv.http.HTTPResponse;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

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

    private String extractFileExtension(String fileName) {
        String fileExtension = "";
        if (fileName.contains(".")) {
            int index = fileName.lastIndexOf(".");
            if (index < fileName.length() -1) {
                fileExtension = fileName.substring(index + 1);
            }
        }
        return fileExtension;
    }

    synchronized public void parseRequest() {
        String method = request.getRequestMethod();
        String filePathString = serverFilesFolderPath + request.getRelativePath();
        Path filePath = Paths.get(filePathString);

        switch (method) {
            case "HEAD": {
                // curl --head localhost:8080/file

                if (Files.exists(filePath)) {
                    // Send file
                    if (Files.isRegularFile(filePath)) {
                        response = new HTTPResponse(500);

                        try {
                            String fileName = filePath.getFileName().toString();
                            String fileExtension = extractFileExtension(fileName);

                            response = new HTTPResponse(200);
                            response.addHeader("Content-Length", String.valueOf(Files.size(filePath)));
                            response.addHeader("Content-Type", HTTPContentType.getContentTypeFor(fileExtension));
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
                            response.addHeader("Content-Length", String.valueOf(xmlString.getBytes().length));
                            response.addHeader("Content-Type", HTTPContentType.getContentTypeFor("txt"));
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
            case "GET": {
                // curl localhost:8080

                // If folder passed, return its structure
                // If file - return file with download headers
                if (Files.exists(filePath)) {
                    // Send file
                    if (Files.isRegularFile(filePath)) {
                        response = new HTTPResponse(500);

                        try {
                            byte[] bytes = Files.readAllBytes(filePath);
                            String fileName = filePath.getFileName().toString();
                            String fileExtension = extractFileExtension(fileName);

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
                if (request.getRelativePath().equals("/")) {
                    response = new HTTPResponse(403);
                    response.setData("Unable to modify root".getBytes(), "txt");
                    return;
                }

                // X-Copy-From: /path/toanother/file2.txt used to copy from file to file
                // curl -X PUT localhost:8080/newFile.txt --header "X-Copy-From: /path/toanother/file2.txt"

                byte[] dataToWrite = null;

                if (request.getHeaderValue("X-Copy-From") != null) {
                    String pathToCopyFromString = request.getHeaderValue("X-Copy-From").replace("\\", "/");;
                    if (pathToCopyFromString.charAt(0) != '/') {
                        pathToCopyFromString = '/' + pathToCopyFromString;
                    }
                    pathToCopyFromString = serverFilesFolderPath + pathToCopyFromString;

                    Path pathToCopyFrom = Paths.get(pathToCopyFromString);
                    if (Files.exists(pathToCopyFrom)) {
                        try {
                            dataToWrite = Files.readAllBytes(pathToCopyFrom);
                        } catch (IOException e) {
//                            e.printStackTrace();
                        }
                    } else {
                        response = new HTTPResponse(404);
                        response.setData("File to copy from does not exist".getBytes(), "txt");
                    }
                } else {
                    dataToWrite = request.getData();
                }

                if (dataToWrite != null) {
                    try {
                        if (!Files.exists(filePath.getParent())) {
                            Files.createDirectories(filePath.getParent());
                        }
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        }
                        Files.createFile(filePath);
                        try (FileOutputStream stream = new FileOutputStream(filePathString)) {
                            stream.write(dataToWrite);
                        }
                        response = new HTTPResponse(201);
                        response.setData("Created new file".getBytes(), "txt");
                    } catch (IOException e) {
//                    e.printStackTrace();
                    }
                }
                break;
            }
            case "DELETE": {
                // curl -X DELETE localhost:8080/file
                response = new HTTPResponse(500);
                if (request.getRelativePath().equals("/")) {
                    response = new HTTPResponse(403);
                    response.setData("Unable to remove root folder".getBytes(), "txt");
                    return;
                }

                if (Files.exists(filePath)) {
                    try {
                        Files.walk(filePath)
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                        response = new HTTPResponse(200);
                        response.setData("Successfully deleted".getBytes(), "txt");
                    } catch (IOException e) {
//                            e.printStackTrace();
                    }
                } else {
                    response = new HTTPResponse(404);
                }

                break;
            }
            default: {
                break;
            }
        }
        response.addHeader("Connection", "Closed");
        response.addHeader("Server", "Jv-file-server");
    }

    public HTTPResponse getResponse() {
        return response;
    }

}
