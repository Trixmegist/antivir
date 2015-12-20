package org.antivir.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.antivir.server.StreamScanner.StreamScannerBuilder.streamScanner;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StreamScannerTest {
  @Mock
  Runnable onSuccess;
  @Mock
  Runnable onFail;

  @Test
  public void whenStreamContainsSample_callsOnSuccess() throws IOException {
    streamScanner()
        .scan(stringToStream("awd qwvirusqd\nwd qw"))
        .forString("virus")
        .onSuccess(onSuccess)
        .onFail(onFail)
        .build().start();

    verify(onFail, never()).run();
    verify(onSuccess, times(1)).run();
  }

  @Test
  public void whenStreamDoesntContainSample_callsOnFail() throws IOException {
    streamScanner()
        .scan(stringToStream("awd qwvirqd\nwd qw"))
        .forString("virus")
        .onSuccess(onSuccess)
        .onFail(onFail)
        .build().start();

    verify(onFail, times(1)).run();
    verify(onSuccess, never()).run();
  }

  @Test
  public void whenStreamContainsSample_callsOnSuccess_for1CharBufferSize() throws IOException {
    streamScanner()
        .scan(stringToStream("awd qwvirusqd\nwd qw"))
        .forString("virus")
        .withBufferSize(1)
        .onSuccess(onSuccess)
        .onFail(onFail)
        .build().start();

    verify(onFail, never()).run();
    verify(onSuccess, times(1)).run();
  }

  private static Reader stringToStream(String str){
    return new InputStreamReader(new ByteArrayInputStream(str.getBytes()));
  }
}