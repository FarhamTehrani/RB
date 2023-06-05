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

  private Socket socket;
  private Server server;

  private BufferedReader input;
  private DataOutputStream output;
  private final String CRLF = "\r\n";

  public Worker(Socket socket, Server server) {
    this.socket = socket;
    this.server = server;
  }

  @Override
  public void run() {
    begin();
  }

  private void begin() {
    try {
      input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      output = new DataOutputStream(socket.getOutputStream());
      String line;
      String filePath = "";
      while (!socket.isClosed()) {
        while ((line = input.readLine()) != null && !line.isEmpty()) {
          System.err.println(line);
          validateLine(line);

          if (line.toLowerCase().contains("get")) {
            filePath = getFilePath(line);
          }
        }
        System.err.println(filePath);
        if (filePath.equals("time")) {
          DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss");
          withStatusCode("200 OK");
          withContentType(".txt");
          withPayload(LocalTime.now().format(format));
        } else if (filePath.equals("date")) {
          DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");
          withStatusCode("200 OK");
          withContentType(".txt");
          withPayload(LocalDate.now().format(format));
        } else {
          if (filePath.isEmpty()) {
            filePath = "/index.html";
          }
          Path currentWorkingDir = Paths.get("").toAbsolutePath();

          withStatusCode("200 OK");
          withContentType(filePath);
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
    withStatusCode("406");
    withContentType(".txt");
    withConnection("keep-alive");
    withPayload("406 Not Acceptable, only Firefox is allowed");
    stopConnection();
    return false;
  }

  private void write(String message) {
    try {
      output.flush();
      if (!socket.isClosed()) {
        output.writeBytes(message + CRLF);
      } else {
        input.close();
        output.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void withStatusCode(String statusCode) {
    write("HTTP/1.0 " + statusCode);
  }

  private void withContentType(String filePath) {
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
    withCRLF();
    write(payload);
    withCRLF();
  }

  private void withFilePayload(String path) {
    File file = new File(path);

    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
      output.flush();

      withContentLength(Files.size(Path.of(path)));
      withCRLF();

      byte[] dataBuffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
        output.write(dataBuffer, 0, bytesRead);
      }
      input.close();
      output.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void withCRLF() {
    write("");
  }

  private void stopConnection() {
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    //interrupt();
  }
}