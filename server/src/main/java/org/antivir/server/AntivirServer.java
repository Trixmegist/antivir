package org.antivir.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.copyOf;
import static java.util.Objects.requireNonNull;

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

      clientOut.println(findInStream(clientIn, INFECTION_MARKER)
          ? INFECTION_POSITIVE_RESPONSE
          : INFECTION_NEGATIVE_RESPONSE);

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

  private static boolean findInStream(Reader stream, String sample) throws IOException {
    char[] chunk = new char[BUFFER_SIZE];
    int charsRead = 0;
    int matchedCharsCount = 0;

    while ((charsRead = stream.read(chunk)) != -1) {
      print("chunk read: ", chunk, charsRead);
      for (int i = 0; i < charsRead; i++) {
        if (sample.charAt(matchedCharsCount) == chunk[i]) {
          matchedCharsCount++;

          if (matchedCharsCount == sample.length()) {
            print("infection found");
            return true;
          }
        } else {
          i -= matchedCharsCount;
          matchedCharsCount = 0;
        }
      }
    }
    print("infection not found");
    return false;
  }

//  private static class StreamScanner {
//    private final Reader stream;
//    private final String sample;
//    private final Runnable onSuccess;
//    private final Runnable onFail;
//
//    public StreamScanner(Reader stream, String sample, Runnable onSuccess, Runnable onFail) {
//      this.stream = requireNonNull(stream);
//      this.sample = requireNonNull(sample);
//      this.onSuccess = requireNonNull(onSuccess);
//      this.onFail = requireNonNull(onFail);
//    }
//
//    public static StreamScanner searchFor(String sample) {
//      return new StreamScanner(stream);
//    }
//
//    public static class StreamScannerBuilder
//
//    public StreamScanner forString(String needle) {
//      this.sample = needle;
//      return this;
//    }
//
//    public StreamScanner onSuccess(Runnable cb) {
//      this.onSuccess = cb;
//      return this;
//    }
//
//    public StreamScanner onFail(Runnable cb) {
//      this.onFail = cb;
//      return this;
//    }
//
//    public void start() throws IOException {
//      char[] buf = new char[BUFFER_SIZE];
//      int charsRead = 0;
//      int matchedCharsCount = 0;
//
//      Runnable resultCallback = onFail;
//      while ((charsRead = stream.read(buf)) != -1) {
//        print("chunk read: " + String.valueOf(Arrays.copyOf(buf, charsRead)));
//        if (findInChunk(buf, charsRead, sample, matchedCharsCount)) {
//          resultCallback = onSuccess;
//          break;
//        }
//      }
//      resultCallback.run();
//    }
//
//    private boolean findInChunk(char[] chunk, int length, String needle, int matchedCharsCount) {
//      for (int i = 0; i < length; i++) {
//        if (needle.charAt(matchedCharsCount) == chunk[i]) {
//          matchedCharsCount++;
//
//          if (matchedCharsCount == needle.length()) {
//            return true;
//          }
//        } else {
//          i -= matchedCharsCount;
//          matchedCharsCount = 0;
//        }
//      }
//      return false;
//    }
//  }

  private static void print(String string) {
    System.out.println(string);
  }

  private static void print(String string, char[] buf, int length) {
    System.out.println(string + new String(copyOf(buf, length)));
  }
}
