package org.antivir.common;

import static java.util.Arrays.copyOf;

public class ConsoleUtils {
  public static void print(String string) {
    System.out.println(string);
  }

  public static void print(String string, char[] buf, int length) {
    System.out.println(string + new String(copyOf(buf, length)));
  }

  public static void print(String string, byte[] buf, int length) {
    System.out.println(string + new String(copyOf(buf, length)));
  }
}
