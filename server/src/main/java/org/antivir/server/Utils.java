package org.antivir.server;

import static java.util.Arrays.copyOf;

public class Utils {
  public static void print(String string) {
    System.out.println(string);
  }

  public static void print(String string, char[] buf, int length) {
    System.out.println(string + new String(copyOf(buf, length)));
  }
}
