import java.util.Queue;
import java.util.LinkedList;

import java.io.IOException;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;

import com.pi4j.wiringpi.Spi;
import com.pi4j.wiringpi.Gpio;

class LedMatrix implements Runnable {
  Queue<String> messages;
  Stopwatch stopwatch;

  final int CHANNEL = 0;
  final int SPI_RATE = 10000000;
  final GpioPinDigitalOutput OE;

  final int SHIFTTIME_MS = 60;

  byte data[];


  LedMatrix() throws IOException {
    System.out.print("Initializing Queue..");
    System.out.flush();
    // Initializing message queue.
    messages = new LinkedList<String>();
    System.out.println("Done");

    System.out.print("Initializing Output Enable pin..");
    System.out.flush();
    // Initializing OE pin.
    final GpioController gpio = GpioFactory.getInstance();
    OE = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "OE", PinState.HIGH);
    System.out.println("Done");

    // Initializing SPI.
    System.out.print("Initializing SPI..");
    System.out.flush();
    com.pi4j.wiringpi.Gpio.wiringPiSetup();
    if(Spi.wiringPiSPISetup(CHANNEL, SPI_RATE) <= -1) {
      throw new IOException();
    }

    byte reset[] = {(byte) 0x00, (byte) 0x00};

    Spi.wiringPiSPIDataRW(CHANNEL, reset, reset.length);
    OE.low();
    OE.high();
    System.out.println("DONE");

    // Initializing stopwatch
    stopwatch = new Stopwatch();

    data = new byte[2];
  }

  public void run() {
    while(true) {
      OE.high();

      while(messages.size() == 0) {
        hold(5);
      }

      // Remove message from queue and add a whitespace to it
      String message = messages.remove();
      System.out.println("Printing message :\"" + message + "\"");
      // For every character in the message
      for(int charIdx = 0; charIdx < message.length()-1; charIdx++) {
        char current = message.charAt(charIdx);
        char next = message.charAt(charIdx+1);
//        System.out.println("Current char: '" + current + "'");
//        System.out.println("next char: '" + next + "'");
        // Display the character 8 times, each time shifting it
        for(int shiftAmnt = 0; shiftAmnt < 8; shiftAmnt++) {
//          System.out.println();
          // Reset StopWatch
          stopwatch.reset();
          // Until time to shift has passed, refresh the character
//          boolean printed = false;
          while(stopwatch.stop() < SHIFTTIME_MS) {
            for(int lineIdx = 0; lineIdx < 8; lineIdx++) {
              OE.high();
//              System.out.format("Font.getByte(%d, %d){0x%02X} << %d = 0x%02X\n", lineIdx, (int) current, Font.getByte(lineIdx, current), shiftAmnt, (Font.getByte(lineIdx, current) << shiftAmnt));
//              System.out.format("Font.getByte(%d, %d){0x%02X} >> %d = 0x%02X\n", lineIdx, (int) next, Font.getByte(lineIdx, current), 8 - shiftAmnt, (Font.getByte(lineIdx, next) >> (8 - shiftAmnt)));
              byte tmpLeft = (byte) (((Font.getByte(lineIdx, current) & 0xFF) << shiftAmnt) & 0xFF);
              byte tmpRight = (byte) (((Font.getByte(lineIdx, next) & 0xFF) >> (8 - shiftAmnt)) & 0xFF);
              data[0] = (byte) (0x80 >> lineIdx);
              data[1] = (byte) (tmpLeft | tmpRight);
//              if(!printed) {
//                System.out.format("\t0x%02X: ", data[0]);
//                System.out.println(String.format("%8s", Integer.toBinaryString(data[1] & 0xFF)).replace(" ", "0"));
//              }
              Spi.wiringPiSPIDataRW(CHANNEL, data, data.length);
              OE.low();
            } // line cycle
//            printed = true;
          } // until time to shift cycle
        } // shift cycle
      } // for every character in message
    } // forever cycle
  } // run

  void addMessage(String message) {
    // Add the new message to the queue with a whitespace at the end
    // so that the last character fades.
    messages.add(" " + message + " ");
  }

  private void hold(long milliseconds) {
    try{
      Thread.sleep(milliseconds);
    } catch (InterruptedException ie) {
      System.out.println("InterruptedException caught and ignored..");
    }
  }

  private class Stopwatch {
    private long start;

    public Stopwatch() {
      start = 0;
    }

    public void reset() {
      start = System.currentTimeMillis();
    }

    public long stop() {
      return System.currentTimeMillis() - start;
    }
  }
}
