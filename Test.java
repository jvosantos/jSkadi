import java.util.Scanner;
import java.io.IOException;

class Test {
  public static void main(String args[]) throws IOException, InterruptedException {
    LedMatrix matrixDriver = new LedMatrix();

    Scanner sc = new Scanner(System.in);

    (new Thread(matrixDriver)).start();

    String message;
    while(true) {
      matrixDriver.addMessage(parse(sc.nextLine()));
    }
  }

  public static String parse(String raw) {
    String processed = new String();

    for(int idx = 0; idx < raw.length(); idx++) {
      if(raw.charAt(idx) == '(' && idx+1 < raw.length()) {
        if(raw.substring(idx+1).startsWith("heart)")) {
          processed += Character.toString((char) 128);
          idx+=6;
        }
      } else {
          processed += Character.toString(raw.charAt(idx));
      }
    }
    return processed;
  }

}
