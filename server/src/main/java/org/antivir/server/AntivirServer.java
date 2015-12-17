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
      System.out.println("waiting for connection...");
      while (true) {
        Socket clientSocket = serverSocket.accept();
        threadPool.submit(() -> scanRequest(clientSocket));
      }
    } finally {
      threadPool.shutdown();
    }
  }

  private void scanRequest(Socket client) {
    try {
      Reader clientIn = new InputStreamReader(client.getInputStream());
      PrintWriter clientOut = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));

      char[] buf = new char[BUFFER_SIZE];
      int charsRead = 0;
      int matchedCharsCount = 0;

      while ((charsRead = clientIn.read(buf)) != -1) {
        print("chunk read: " + String.valueOf(Arrays.copyOf(buf, charsRead)));
        String chunkStatus = INFECTION_NEGATIVE_RESPONSE;

        for (int i = 0; i < charsRead; i++) {
          if (INFECTION_MARKER.charAt(matchedCharsCount) == buf[i]) {
            matchedCharsCount++;

            if (matchedCharsCount == INFECTION_MARKER.length()) {
              chunkStatus = INFECTION_POSITIVE_RESPONSE;
              break;
            }
          } else {
            i -= matchedCharsCount;
            matchedCharsCount = 0;
          }
        }

        print("sending chunk status to the client...");
        clientOut.println(chunkStatus);
        clientOut.flush();
        print("chuck status: " + chunkStatus);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void scanRequest2(Socket client) {
    try (Reader clientIn = new InputStreamReader(client.getInputStream());
         PrintWriter clientOut = new PrintWriter(new OutputStreamWriter(client.getOutputStream()))) {

      clientOut.println(findInStream(clientIn, INFECTION_MARKER)
          ? INFECTION_POSITIVE_RESPONSE
          : INFECTION_NEGATIVE_RESPONSE);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static boolean findInStream(Reader stream, String sample) throws IOException {
    char[] buf = new char[BUFFER_SIZE];
    int charsRead = 0;
    int matchedCharsCount = 0;

    boolean result = false;
    while ((charsRead = stream.read(buf)) != -1) {
      print("chunk read: " + String.valueOf(Arrays.copyOf(buf, charsRead)));
      if (findInChunk(buf, charsRead, sample, matchedCharsCount)) {
        result = true;
        break;
      }
    }
    return result;
  }

  private static boolean findInChunk(char[] chunk, int length, String sample, int matchedCharsCount) {
    for (int i = 0; i < length; i++) {
      if (sample.charAt(matchedCharsCount) == chunk[i]) {
        matchedCharsCount++;

        if (matchedCharsCount == sample.length()) {
          return true;
        }
      } else {
        i -= matchedCharsCount;
        matchedCharsCount = 0;
      }
    }
    return false;
  }


  private static class StreamScanner {
    private Reader stream;
    private String needle;
    private Runnable onSuccess;
    private Runnable onFail;

    private StreamScanner(Reader stream) {
      this.stream = stream;
    }

    public static StreamScanner scan(Reader stream) {
      return new StreamScanner(stream);
    }

    public StreamScanner forString(String needle) {
      this.needle = needle;
      return this;
    }

    public StreamScanner onSuccess(Runnable cb) {
      this.onSuccess = cb;
      return this;
    }

    public StreamScanner onFail(Runnable cb) {
      this.onFail = cb;
      return this;
    }

    public void start() throws IOException {
      char[] buf = new char[BUFFER_SIZE];
      int charsRead = 0;
      int matchedCharsCount = 0;

      Runnable resultCallback = onFail;
      while ((charsRead = stream.read(buf)) != -1) {
        print("chunk read: " + String.valueOf(Arrays.copyOf(buf, charsRead)));
        if (findInChunk(buf, charsRead, needle, matchedCharsCount)) {
          resultCallback = onSuccess;
          break;
        }
      }
      resultCallback.run();
    }

    private boolean findInChunk(char[] chunk, int length, String needle, int matchedCharsCount) {
      for (int i = 0; i < length; i++) {
        if (needle.charAt(matchedCharsCount) == chunk[i]) {
          matchedCharsCount++;

          if (matchedCharsCount == needle.length()) {
            return true;
          }
        } else {
          i -= matchedCharsCount;
          matchedCharsCount = 0;
        }
      }
      return false;
    }
  }

  private static void print(String string) {
    System.out.println(string);
  }
}
