package org.antivir.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;
import static org.antivir.common.Constants.*;

public class AntivirServer {

  public static final String INFECTION_MARKER = "virus";

  public static void main(String[] args) throws IOException {
    int port = args[0] != null
        ? parseInt(args[0])
        : DEFAULT_SERVER_PORT;
    start(port);
  }

  private static void start(int port) throws IOException {
    ExecutorService threadPool = Executors.newFixedThreadPool(10);

    try (ServerSocket serverSocket = new ServerSocket(port)) {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        threadPool.submit(() -> processRequest(clientSocket));
      }
    } finally {
      threadPool.shutdown();
    }
  }

  private static void processRequest(Socket client) {
    try {
      BufferedReader request = new BufferedReader(new InputStreamReader(client.getInputStream()));
      PrintWriter response = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
      response.println(isInfected(request)
          ? INFECTION_POSITIVE_RESPONSE
          : INFECTION_NEGATIVE_RESPONSE);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static boolean isInfected(BufferedReader clientStream) throws IOException {
    return scan(clientStream, INFECTION_MARKER);
  }

  private static boolean scan(BufferedReader stream, String string) throws IOException {
    char[] buf = new char[1024];
    int matchedCharsCount = 0;

    while (stream.read(buf) > 0) {
      for (char charFromStream : buf) {
        if (string.length() >= matchedCharsCount) {
          return true;
        }

        if (string.charAt(matchedCharsCount) == charFromStream) {
          matchedCharsCount++;
        } else {
          matchedCharsCount = 0;
        }
      }
    }
    return false;
  }
}
