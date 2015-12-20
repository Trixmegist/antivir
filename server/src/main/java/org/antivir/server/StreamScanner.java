package org.antivir.server;

import java.io.IOException;
import java.io.Reader;

import static java.util.Objects.requireNonNull;
import static org.antivir.common.ConsoleUtils.print;

class StreamScanner {
  public static final int DEFAULT_BUFFER_SIZE = 3;

  private final Reader stream;
  private final String sample;
  private final Runnable onSuccess;
  private final Runnable onFail;
  private final int bufferSize;

  public StreamScanner(Reader stream, String sample, Runnable onSuccess, Runnable onFail) {
    this.stream = requireNonNull(stream);
    this.sample = requireNonNull(sample);
    this.onSuccess = requireNonNull(onSuccess);
    this.onFail = requireNonNull(onFail);
    this.bufferSize = DEFAULT_BUFFER_SIZE;
  }

  public StreamScanner(Reader stream, String sample, Runnable onSuccess, Runnable onFail, int bufferSize) {
    this.stream = requireNonNull(stream);
    this.sample = requireNonNull(sample);
    this.onSuccess = requireNonNull(onSuccess);
    this.onFail = requireNonNull(onFail);
    this.bufferSize = bufferSize;
  }

  public void start() throws IOException {
    print("Scanning for sample: " + sample);
    char[] chunk = new char[bufferSize];
    int charsRead = 0;
    int matchedCharsCount = 0;

    while ((charsRead = stream.read(chunk)) != -1) {
      print("chunk read: ", chunk, charsRead);
      for (int i = 0; i < charsRead; i++) {
        if (sample.charAt(matchedCharsCount) == chunk[i]) {
          matchedCharsCount++;

          if (matchedCharsCount == sample.length()) {
            print("sample found");
            onSuccess.run();
            return;
          }
        } else {
          i = i >= matchedCharsCount
              ? i - matchedCharsCount
              : 0;
          matchedCharsCount = 0;
        }
      }
    }
    print("sample not found");
    onFail.run();
  }

  public static class StreamScannerBuilder {
    private Reader stream;
    private String sample;
    private Runnable onSuccess;
    private Runnable onFail;
    private int bufferSize;

    public static StreamScannerBuilder streamScanner() {
      return new StreamScannerBuilder();
    }

    public StreamScannerBuilder withBufferSize(int chars) {
      this.bufferSize = chars;
      return this;
    }

    public StreamScannerBuilder scan(Reader stream) {
      this.stream = stream;
      return this;
    }

    public StreamScannerBuilder forString(String sample) {
      this.sample = sample;
      return this;
    }

    public StreamScannerBuilder onSuccess(Runnable onSuccess) {
      this.onSuccess = onSuccess;
      return this;
    }

    public StreamScannerBuilder onFail(Runnable onFail) {
      this.onFail = onFail;
      return this;
    }

    public StreamScanner build() {
      return bufferSize > 0
          ? new StreamScanner(stream, sample, onSuccess, onFail, bufferSize)
          : new StreamScanner(stream, sample, onSuccess, onFail);
    }
  }
}
