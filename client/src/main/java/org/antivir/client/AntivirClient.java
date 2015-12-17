package org.antivir.client;

import java.io.*;
import java.net.Socket;

import static java.lang.Integer.parseInt;

public class AntivirClient {

  public static final int DEFAULT_SERVER_PORT = 8181;
  public static final String DEFAULT_SERVER_HOST = "localhost";
  public static final int BUFFER_SIZE = 3;
  public static final String INFECTION_POSITIVE_RESPONSE = "infected";
  public static final String INFECTION_NEGATIVE_RESPONSE = "safe";

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Please specify a file to scan as a first argument");
      return;
    }

    String filepathToScan = args[0];
    String serverHost = args.length > 1
        ? args[1]
        : DEFAULT_SERVER_HOST;
    int serverPort = args.length > 2
        ? parseInt(args[2])
        : DEFAULT_SERVER_PORT;

    if (filepathToScan == null) {
      System.out.println("Please specify a file to scan as a first argument");
      return;
    }

    File fileToScan = new File(filepathToScan);
    String scanResult = null;
    try {
      scanResult = scan(fileToScan, serverHost, serverPort);
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println(scanResult);
  }

  private static String scan(File file, String antivirHost, int antivirPort) throws IOException {
    InputStream fileIn = new FileInputStream(file);

    Socket server = new Socket(antivirHost, antivirPort);
    OutputStream serverOut = server.getOutputStream();
    BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));

    byte[] buf = new byte[BUFFER_SIZE];
    int bytesRead = 0;

    while ((bytesRead = fileIn.read(buf)) != -1) {
      try {
        serverOut.write(buf, 0, bytesRead);
        serverOut.flush();
      } catch (IOException e) {

      }
    }

    print("reading chunk status");
    String chunkStatus = serverIn.readLine();
    print("chunk status: " + chunkStatus);
    if (INFECTION_POSITIVE_RESPONSE.equals(chunkStatus)) {
      serverIn.close();
      serverOut.close();
      return INFECTION_POSITIVE_RESPONSE;
    }
    server.shutdownOutput();
    return INFECTION_NEGATIVE_RESPONSE;
  }

  private static void print(String string) {
    System.out.println(string);
  }
}
