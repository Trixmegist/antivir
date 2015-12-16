package org.antivir.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;

public class AntivirServer {

  public static final int DEFAULT_SERVER_PORT = 8181;
  public static final String INFECTION_POSITIVE_RESPONSE = "infected";
  public static final String INFECTION_NEGATIVE_RESPONSE = "safe";
  public static final String INFECTION_MARKER = "virus";
  public static final int BUFFER_SIZE = 3;
  public static final int THREAD_COUNT = 10;

  public static void main(String[] args) throws IOException {
    int port = args.length > 0
        ? parseInt(args[0])
        : DEFAULT_SERVER_PORT;
    start(port);
  }

  private static void start(int port) throws IOException {
    ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);

    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("waiting for connection...");
      while (true) {
        Socket clientSocket = serverSocket.accept();
        scanRequest(clientSocket);
        threadPool.submit(
            () -> scanRequest(clientSocket)
        );
      }
    } finally {
      threadPool.shutdown();
    }
  }

  private static void scanRequest(Socket client) {
    try {
      Reader clientIn = new InputStreamReader(client.getInputStream());
      PrintWriter clientOut = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));

      char[] buf = new char[BUFFER_SIZE];
      int charsRead = 0;
      int matchedCharsCount = 0;

      while ((charsRead = clientIn.read(buf)) != -1) {
        print("chunk read: "+String.valueOf(Arrays.copyOf(buf, charsRead)));
        String chuckStatus = INFECTION_NEGATIVE_RESPONSE;

        for (int i = 0; i < charsRead; i++) {
          if (INFECTION_MARKER.charAt(matchedCharsCount) == buf[i]) {
            matchedCharsCount++;

            if(matchedCharsCount == INFECTION_MARKER.length()) {
              chuckStatus = INFECTION_POSITIVE_RESPONSE;
              break;
            }
          } else {
            i -= matchedCharsCount;
            matchedCharsCount = 0;
          }
        }

        print("sending chunk status to the client...");
        clientOut.println(chuckStatus);
        clientOut.flush();
        print("chuck status: "+chuckStatus);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void print(String string) {
    System.out.println(string);
  }
}
