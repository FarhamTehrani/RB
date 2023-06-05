import java.net.ServerSocket;
import java.net.Socket;

public class Server {
  private boolean serviceRequested;
  public final int serverPort;
  public Server(int serverPort) {
        serviceRequested = true;
        this.serverPort = serverPort;
  }
  public static void main(String[] args) {
      Server server = new Server(6001);
      server.startServer();
  }
  public void startServer() {
      ServerSocket welcomeSocket; // TCP-Server-Socketklasse
      Socket connectionSocket; // TCP-Standard-Socketklasse

      int nextThreadNumber = 0;
      try{
          /* Server-Socket erzeugen */
          System.err.println("Creating new TCP Server Socket Port " + serverPort);
          welcomeSocket = new ServerSocket(serverPort);

          while (serviceRequested) {
              System.err.println("TCP Server is waiting for connection - listening TCP port " + serverPort);
              /*
               * Blockiert auf Verbindungsanfrage warten --> nach Verbindungsaufbau
               * Standard-Socket erzeugen und an connectionSocket zuweisen
               */
              connectionSocket = welcomeSocket.accept();

              /* Neuen Arbeits-Thread erzeugen und die Nummer, den Socket sowie das Serverobjekt uebergeben */
              (new Worker(connectionSocket, this)).start();
          }} catch (Exception e) {
          System.err.println(e.getMessage());
      }
  }
}