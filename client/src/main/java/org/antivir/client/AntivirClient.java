package org.antivir.client;

import java.io.*;
import java.net.Socket;

import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;
import static org.antivir.common.ConsoleUtils.print;
import static org.antivir.common.ServerDefaults.DEFAULT_SERVER_HOST;
import static org.antivir.common.ServerDefaults.DEFAULT_SERVER_PORT;

public class AntivirClient {

  public static final int BUFFER_SIZE = 3;

  private final int serverPort;
  private final String serverHost;

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Please specify a file to scan as a first argument");
      return;
    }

    String fileToScan = args[0];
    String serverHost = args.length > 1
        ? args[1]
        : DEFAULT_SERVER_HOST;
    int serverPort = args.length > 2
        ? parseInt(args[2])
        : DEFAULT_SERVER_PORT;

    if (fileToScan == null) {
      System.out.println("Please specify a file to scan as a first argument");
      return;
    }

    try {
      File file = new File(fileToScan);
      AntivirClient client = new AntivirClient(serverPort, serverHost);
      print("scanning: " + file.getAbsolutePath());
      print("result: " + client.scan(new File(fileToScan)));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public AntivirClient(int serverPort, String serverHost) {
    this.serverPort = serverPort;
    this.serverHost = requireNonNull(serverHost);
  }

  public String scan(File file) throws IOException {
    try (InputStream fileIn = new FileInputStream(file);
         Socket server = new Socket(serverHost, serverPort);
         OutputStream serverOut = server.getOutputStream();
         BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()))) {

      byte[] buf = new byte[BUFFER_SIZE];
      int bytesRead = 0;

      while ((bytesRead = fileIn.read(buf)) != -1) {
        try {
          serverOut.write(buf, 0, bytesRead);
          serverOut.flush();
          print("chunk sent: ", buf, bytesRead);
        } catch (IOException e) {
          if (!serverIn.ready()) {
            throw e;
          }
        }

        if (serverIn.ready()) {
          break;
        }
      }

      server.shutdownOutput();
      return serverIn.readLine();
    }
  }
}
