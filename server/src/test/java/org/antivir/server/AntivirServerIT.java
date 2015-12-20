package org.antivir.server;

import org.antivir.client.AntivirClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;
import static org.antivir.common.ConsoleUtils.print;
import static org.antivir.common.ServerDefaults.INFECTION_NEGATIVE_RESPONSE;
import static org.antivir.common.ServerDefaults.INFECTION_POSITIVE_RESPONSE;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class AntivirServerIT {
  public static final int TEST_SERVER_PORT = 8182;
  private static AntivirServer theServer;
  private static ExecutorService serverThread;

  @BeforeClass
  public static void startAntivirServer() throws Exception {
    try {
      serverThread = newSingleThreadExecutor();
      theServer = new AntivirServer(TEST_SERVER_PORT);
      serverThread.submit(() -> {
        try {
          print("Starting the server...");
          theServer.start();
        } catch (IOException e) {
          new RuntimeException("Can't start the server", e);
        }
      });
    } catch (RuntimeException e) {
      e.printStackTrace();
      serverThread.shutdownNow();
    }
  }

  @AfterClass
  public static void stopAntivirServer() throws Exception {
    print("Shutting down the server...");
    if (theServer != null) {
      //TODO stop the server
    }
    if (serverThread != null) {
      shutdownThreadPool(serverThread);
    }
  }

  @Test
  public void whenSingleClientSendsInfectedFile_itReceivesPositiveResponse() throws Exception {
    AntivirClient client = new AntivirClient(TEST_SERVER_PORT, "localhost");
    File file = new File(resource("infectedFile.txt"));

    String result = client.scan(file);
    assertThat(result, equalTo(INFECTION_POSITIVE_RESPONSE));
  }

  @Test
  public void whenSingleClientSendsNotInfecteSafeFile_itReceivesNegativeResponse() throws Exception {
    AntivirClient client = new AntivirClient(TEST_SERVER_PORT, "localhost");
    File file = new File(resource("safeFile.txt"));

    String result = client.scan(file);
    assertThat(result, equalTo(INFECTION_NEGATIVE_RESPONSE));
  }

  @Test
  public void whenMultipleClientSendFilesConcurrently_serverProcessesTheirRequestsCorrectly() throws Exception {
    multitask(10, () -> {
      try {
        return new AntivirClient(TEST_SERVER_PORT, "localhost").scan(new File(resource("infectedFile.txt")));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, (result, ex) -> {
      assertThat(ex, is(nullValue()));
      assertThat(result, equalTo(INFECTION_POSITIVE_RESPONSE));
    }).join();
  }

  private String resource(String name) {
    URL url = this.getClass().getClassLoader().getResource(name);
    if (url == null) {
      throw new NullPointerException("Resource not found: "+name);
    }
    return url.getFile();
  }

  @SuppressWarnings("unchecked")
  private static <T> CompletableFuture<T> multitask(int threads,
                                             Supplier<T> resultSupplier,
                                             BiConsumer<T, Throwable> afterEach) {
    ExecutorService threadPool = newFixedThreadPool(threads);
    return (CompletableFuture<T>) allOf(generate(() ->
        supplyAsync(resultSupplier, threadPool).handle((result, ex) -> {
          afterEach.accept(result, ex);
          return null;
        }))
        .limit(threads)
        .collect(toList())
        .toArray(new CompletableFuture[threads]))
        .thenRun(() -> shutdownThreadPool(threadPool));
  }

  private static void shutdownThreadPool(ExecutorService threadPool) {
    try {
      threadPool.awaitTermination(1L, SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      threadPool.shutdownNow();
    }
  }
}