import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.Socket;
import java.net.ServerSocket;

class ServerMatrix {
  public static void main(String args[]) {
    int portnumber = 3410;
    boolean emote = false;
    ServerSocket server = null;
    boolean verbose = false;
    boolean clientVerbose = false;
    int clientCounter = 0;
    LedMatrix matrixDriver = null;

    for(String arg : args) {
      if(arg.equals("--port")) {
        try {
          portnumber = Integer.parseInt(arg.substring(6));
        } catch (NumberFormatException nfe) {
          System.out.println("Error: port number specified is not a number.");
          System.out.println("More info with \"--help\"");
          return ;
        }
      } else if(arg.equals("--emote")) {
        emote = true;
      } else if(arg.equals("--client-verbose")) {
        clientVerbose = true;
      } else if(arg.equals("--verbose")) {
        verbose = true;
      } else if(arg.equals("--help") || arg.equals("-h")) {
        usage();
        return ;
      } else {
        System.out.println("Unknown option argument: " + arg);
        System.out.println("More info with \"--help\"");
      }
    }

    try {
      matrixDriver = LedMatrix.getInstance();
    } catch(IOException ioe) {
      System.out.println("Error initializing led matrix driver: " + ioe.getMessage());
      return ;
    }

    (new Thread(matrixDriver)).start();

    try {
      server = new ServerSocket(portnumber);
    } catch (Exception e) {
      System.out.println("Error creating server: " + e.getMessage());
      return ;
    }

    while(true) {
      try {
        if(verbose) System.out.println("Waiting for clients to connect...");
        (new Thread(new ClientHandler(server.accept(), clientVerbose))).start();
        clientCounter++;
        if(verbose) System.out.println("Client #" + clientCounter + " Connected!");
      } catch (IOException ioe) {
        System.out.println("Error launching new client: " + ioe.getMessage());
      }
    }
  }

  private static void usage() {
    System.out.println("ServerMatrix - Server to interact with the 8x8 led matrix");
    System.out.println("\t-h, --help");
    System.out.println("\t\tPrints this manual.");
    System.out.println("\t--port=PORTNUMBER");
    System.out.println("\t\tSpecifies the port number to be used by the server. Default port number is 3410.");
    System.out.println("\t--emote");
    System.out.println("\t\tReplaces some emotes with equivalent characters.");
  }

  private static class ClientHandler implements Runnable {
    private Socket socket = null;
    private LedMatrix matrixDriver = null;
    private BufferedReader in = null;
    private boolean verbose = false;

    public ClientHandler(Socket socket) throws IOException {
      this.socket = socket;
      matrixDriver = LedMatrix.getInstance();
    }

    public ClientHandler(Socket socket, boolean verbose) throws IOException {
      this.socket = socket;
      this.verbose = verbose;
      matrixDriver = LedMatrix.getInstance();
    }

    public void run() {
      try{
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String message;

        while((message = in.readLine()) != null) {
          if(message.equals("exit")) {
            if(verbose) System.out.println("Closing client");
            return ;
          }
          if(verbose) System.out.println("Adding new message: \"" + parse(message) + "\"");
          matrixDriver.addMessage(parse(message));
        }
      } catch(IOException ioe) {
        System.out.println("Error: " + ioe.getMessage());
      } finally {
        try {
          in.close();
          socket.close();
        } catch (Exception e) {
          System.out.println("Error: Couldn't close I/O streams.");
        }
      } // finaly
    } // run

    public static String parse(String raw) {
      raw = raw.replaceAll("<3", Character.toString((char) 128));

      return raw;
    }
  }
}
