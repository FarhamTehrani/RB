import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Worker extends Thread {

  private final Socket socket;
  private final Server server;
  private final String CRLF = "\r\n";
  private BufferedReader inFromClient;
  private DataOutputStream outToClient;
  RestController restController;


  public Worker(Socket socket, Server server) {
    this.server = server;
    this.socket = socket;
    restController = new RestController();
  }

  @Override
  public void run() {
    try {
      inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      outToClient = new DataOutputStream(socket.getOutputStream());
      String line;
      String filePath = "";
      while (!socket.isClosed()) {
        while ((line = inFromClient.readLine()) != null && !line.isEmpty()) {
          System.err.println(line);
          isFirefox(line);

          if (line.toLowerCase().contains("get")) {
            filePath = getFilePath(line);
          }
        }
        System.err.println(filePath);
        if (filePath.equals("time")) {
          writeStatusCode("200 OK");
          writeContentType(".txt");
          writeToClient(restController.returnTime());
        } else if (filePath.equals("date")) {
          writeStatusCode("200 OK");
          writeContentType(".txt");
          writeToClient(restController.returnDate());
        } else {
          if (filePath.isEmpty()) {
            filePath = "/index.html";
          }
          Path currentWorkingDir = Paths.get("").toAbsolutePath();

          writeStatusCode("200 OK");
          writeContentType(filePath);
          writeFileContent(
                  currentWorkingDir + "\\p3-webserver\\src\\main\\resources\\assets\\" + filePath);
        }
        stopConnection();
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
  private String getFilePath(String request) {
    String path = request.split(" ")[1];
    try {
      if (path.contains("exit")) {
        stopConnection();
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
    return path.substring(1);
  }

  private boolean isFirefox(String line) throws IOException {
    String filePath = getFilePath(line);
    if(filePath.equals("date") || filePath.equals("time")){
      return true;
    }
    if (!line.toLowerCase().contains("user-agent") || line.toLowerCase().contains("firefox")) {
      return true;
    }
    writeStatusCode("406");
    writeContentType(".txt");
    writeConnectionType("keep-alive");
    writeToClient("HTTP/1.0 406 Not Acceptable");
    stopConnection();
    return false;
  }

  private void write(String message) {
    try {
      outToClient.flush();
      if (!socket.isClosed()) {
        outToClient.writeBytes(message + CRLF);
      } else {
        inFromClient.close();
        outToClient.close();
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  private void writeStatusCode(String statusCode) {
    write("HTTP/1.0 " + statusCode);
  }

  private void writeContentType(String filePath) {
    String fileType = filePath.substring(filePath.indexOf("."));
    String contentType = switch (fileType) {
      case ".gif" -> "image/gif";
      case ".jpg" -> "image/jpeg";
      case ".ico" -> "image/x-icon";
      case ".pdf" -> "application/pdf";
      default -> "text/html";
    };
    write("Content-Type: " + contentType);
  }

  private void writeContentLength(long contentLength) {
    write("Content-Length: " + contentLength);
  }

  private void writeConnectionType(String connectionType) {
    write("Connection: " + connectionType);
  }

  private void writeToClient(String line) throws IOException {
    outToClient.write((line + CRLF).getBytes());
    long contentLength = line.getBytes().length;
    writeContentLength(contentLength);
    write("");
    write(line);
    write("");
  }

  private void writeFileContent(String path) {
    File file = new File(path);

    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
      outToClient.flush();

      writeContentLength(Files.size(Path.of(path)));
      write("");

      byte[] dataBuffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
        outToClient.write(dataBuffer, 0, bytesRead);
      }
      inFromClient.close();
      outToClient.close();

    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
  private void stopConnection() {
    try {
      socket.close();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}