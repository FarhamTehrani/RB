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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Worker extends Thread {

  private final Socket socket;
  private final Server server;
  private final String CRLF = "\r\n";
  private BufferedReader inFromClient;
  private DataOutputStream outToClient;


  public Worker(Socket socket, Server server) {
    this.server = server;
    this.socket = socket;
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
          validateLine(line);

          if (line.toLowerCase().contains("get")) {
            filePath = getFilePath(line);
          }
        }
        System.err.println(filePath);
        if (filePath.equals("time")) {
          DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss");
          writeStatusCode("200 OK");
          writeContentType(".txt");
          withPayload(LocalTime.now().format(format));
        } else if (filePath.equals("date")) {
          DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");
          writeStatusCode("200 OK");
          writeContentType(".txt");
          withPayload(LocalDate.now().format(format));
        } else {
          if (filePath.isEmpty()) {
            filePath = "/index.html";
          }
          Path currentWorkingDir = Paths.get("").toAbsolutePath();

          writeStatusCode("200 OK");
          writeContentType(filePath);
          withFilePayload(
                  currentWorkingDir + "\\p3-webserver\\src\\main\\resources\\assets\\" + filePath);
        }
        stopConnection();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  private String getFilePath(String request) {
    String path = request.split(" ")[1];
    try {
      if (path.contains("exit")) {
        stopConnection();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return path.substring(1);
  }

  private boolean validateLine(String line) {
    if (!line.toLowerCase().contains("user-agent") || line.toLowerCase().contains("firefox")) {
      return true;
    }
    writeStatusCode("406");
    writeContentType(".txt");
    withConnection("keep-alive");
    withPayload("406 Not Acceptable, only Firefox is allowed");
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
      e.printStackTrace();
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
      case ".html" -> "text/html";
      default -> "text/plain";
    };
    write("Content-Type: " + contentType);
  }

  private void withContentLength(long contentLength) {
    write("Content-Length: " + contentLength);
  }

  private void withConnection(String connectionType) {
    write("Connection: " + connectionType);
  }

  private void withPayload(String payload) {
    long contentLength = payload.getBytes().length;
    withContentLength(contentLength);
    writeCRLF();
    write(payload);
    writeCRLF();
  }

  private void withFilePayload(String path) {
    File file = new File(path);

    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
      outToClient.flush();

      withContentLength(Files.size(Path.of(path)));
      writeCRLF();

      byte[] dataBuffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
        outToClient.write(dataBuffer, 0, bytesRead);
      }
      inFromClient.close();
      outToClient.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void writeCRLF() {
    write("");
  }

  private void stopConnection() {
    try {
      socket.close();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}