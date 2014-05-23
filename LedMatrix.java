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

  LedMatrix() {
    // Initializing message queue.
    messages = new LinkedList<String>();

    // Initializing OE pin.
    final GpioController gpio = GpioFactory.getInstance();
    OE = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "OE", PinState.HIGH);

    // Initializing SPI.
    com.pi4j.wiringpi.Gpio.wiringPiSetup();
    if(Spi.wiringPiSPISetup(CHANNEL, SPI_RATE) <= -1) {
      throw IOException;
    }

    byte reset[] = {(byte) 0x00, (byte) 0x00};

    Spi.wiringPiSPIDataRW(CHANNEL, reset, reset.length);
    OE.low();
    OE.high();
  }

  void run() {
    OE.high();
    while(message.size() == 0) {
      sleep(5);
    }



    OE.low();
  }

  void addNewMessage(String message) {
    messages.add(message);
  }
}
