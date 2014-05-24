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

  final int CHANNEL = 0;
  final int SPI_RATE = 10000000;
  final GpioPinDigitalOutput OE;

  LedMatrix() throws IOException {
    // Initializing message queue.
    messages = new LinkedList<String>();

    // Initializing OE pin.
    final GpioController gpio = GpioFactory.getInstance();
    OE = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "OE", PinState.HIGH);

    // Initializing SPI.
    com.pi4j.wiringpi.Gpio.wiringPiSetup();
    if(Spi.wiringPiSPISetup(CHANNEL, SPI_RATE) <= -1) {
      throw new IOException();
    }

    byte reset[] = {(byte) 0x00, (byte) 0x00};

    Spi.wiringPiSPIDataRW(CHANNEL, reset, reset.length);
    OE.low();
    OE.high();
  }

  public void run() {
    OE.high();
    while(messages.size() == 0) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException ie) {
        System.out.println("InterruptedException caught...");
      }
    }

    for(String message : messages) {
      System.out.println(message);
    }

    OE.low();
  }

  void addMessage(String message) {
    messages.add(message);
  }
}
