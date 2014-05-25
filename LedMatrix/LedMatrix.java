import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
  private static LedMatrix instance = null;
  Queue<String> messages;
  Stopwatch stopwatch;

  final int CHANNEL = 0;
  final int SPI_RATE = 10000000;
  final GpioPinDigitalOutput OE;

  final int SHIFTTIME_MS = 60;

  byte data[];

  public static LedMatrix getInstance() throws IOException {
    if(instance == null) {
      instance = new LedMatrix();
    }

    return instance;
  }

  private LedMatrix() throws IOException {
    System.out.print("Initializing Queue..");
    System.out.flush();
    // Initializing message queue.
    messages = new ConcurrentLinkedQueue<String>();
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
        if((int) current > 255 || (int) current < 0) current = (char) 0;
        if((int) next > 255 || (int) next < 0) next = (char) 0;
        // Display the character 8 times, each time shifting it
        for(int shiftAmnt = 0; shiftAmnt < 8; shiftAmnt++) {
          // Reset StopWatch
          stopwatch.reset();
          // Until time to shift has passed, refresh the character
          while(stopwatch.stop() < SHIFTTIME_MS) {
            for(int lineIdx = 0; lineIdx < 8; lineIdx++) {
              OE.high();
              byte tmpLeft = (byte) (((Font.getByte(lineIdx, current) & 0xFF) << shiftAmnt) & 0xFF);
              byte tmpRight = (byte) (((Font.getByte(lineIdx, next) & 0xFF) >> (8 - shiftAmnt)) & 0xFF);
              data[0] = (byte) (0x80 >> lineIdx);
              data[1] = (byte) (tmpLeft | tmpRight);
              Spi.wiringPiSPIDataRW(CHANNEL, data, data.length);
              OE.low();
            } // line cycle
//            printed = true;
          } // until time to shift cycle
        } // shift cycle
      } // for every character in message
    } // forever cycle
  } // run

  public synchronized void addMessage(String message) {
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
