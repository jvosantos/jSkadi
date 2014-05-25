import java.util.Scanner;
import java.io.IOException;

class CliMatrix {
  public static void main(String args[]) {
    LedMatrix matrixDriver = null;
    Scanner sc = null;

    sc = new Scanner(System.in);
    try {
      matrixDriver = LedMatrix.getInstance();
    } catch (IOException ioe) {
      System.out.println("Error initializing led matrix driver: " + ioe.getMessage());
      return ;
    }

    (new Thread(matrixDriver)).start();

    while(true) {
      matrixDriver.addMessage(parse(sc.nextLine()));
    }
  }

  public static String parse(String raw) {
    raw = raw.replaceAll("<3", Character.toString((char) 128));

    return raw;
  }
}
