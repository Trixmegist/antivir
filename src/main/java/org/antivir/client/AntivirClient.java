package org.antivir.client;

import java.io.*;
import java.net.Socket;

import static java.lang.Integer.parseInt;
import static org.antivir.common.Constants.DEFAULT_SERVER_HOST;
import static org.antivir.common.Constants.DEFAULT_SERVER_PORT;

public class AntivirClient {

  public static void main(String[] args) {
    String filepathToScan = args[0];
    String serverHost = args[1] != null
        ? args[1]
        : DEFAULT_SERVER_HOST;
    int serverPort = args[2] != null
        ? parseInt(args[2])
        : DEFAULT_SERVER_PORT;

    if(filepathToScan == null) {
      System.out.println("Please specify a file to scan as a first argument");
      return;
    }

    File fileToScan = new File(filepathToScan);
    String scanResult = null;
    try {
      scanResult = requestScan(fileToScan, serverHost, serverPort);
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println(scanResult);
  }

  private static String requestScan(File file, String antivirHost, int antivirPort) throws IOException {
    Socket server = new Socket(antivirHost, antivirPort);
    sendFile(server, file);
    BufferedReader response = new BufferedReader(new InputStreamReader(server.getInputStream()));
    return response.readLine();
  }

  private static void sendFile(Socket socket, File file) throws IOException {
    BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file));
    BufferedOutputStream socketOut = new BufferedOutputStream(socket.getOutputStream());

    byte[] buf = new byte[1024];
    while (fileIn.read(buf) > 0){
      socketOut.write(buf);
    }
  }
}
