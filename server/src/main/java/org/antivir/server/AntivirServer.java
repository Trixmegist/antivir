package org.antivir.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;
import static org.antivir.common.ServerDefaults.*;
import static org.antivir.server.StreamScanner.StreamScannerBuilder.streamScaner;
import static org.antivir.server.Utils.print;

public class AntivirServer {

  public static final String INFECTION_MARKER = "virus";
  public static final int THREAD_COUNT = 10;

  private final int port;

  public AntivirServer(int port) {
    this.port = port;
  }

  public static void main(String[] args) throws IOException {
    int port = args.length > 0
        ? parseInt(args[0])
        : DEFAULT_SERVER_PORT;

    new AntivirServer(port).start();
  }

  private void start() throws IOException {
    ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);

    try (ServerSocket serverSocket = new ServerSocket(port)) {
      print("waiting for connection...");
      while (true) {
        Socket clientSocket = serverSocket.accept();
        threadPool.submit(() -> scanRequest(clientSocket));
      }
    } finally {
      threadPool.shutdown();
    }
  }

  private void scanRequest(Socket client) {
    try (Reader clientIn = new InputStreamReader(client.getInputStream());
         PrintWriter clientOut = new PrintWriter(new OutputStreamWriter(client.getOutputStream()))) {

      streamScaner()
          .scan(clientIn)
          .forString(INFECTION_MARKER)
          .onSuccess(() -> clientOut.println(INFECTION_POSITIVE_RESPONSE))
          .onFail(() -> clientOut.println(INFECTION_NEGATIVE_RESPONSE))
          .build()
          .start();

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        client.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
