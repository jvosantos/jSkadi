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

  Timer timer;
  final int SHIFTTIME_MS = 250;
  
  LedMatrix() {
	timer = new Timer(); 
  
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
	String message;
	while(1)
	{
		OE.high();
		
		while(messages.size() == 0) {
			sleep(5);
		}
	
		s = new String( message.top() + ' ' ); 	// added whitespace at end of message to have something to show when shifting to last character.
		for(int i=0; i<s.length()-1; i++)		// s.length()-1 because we added a whitespace
		{
			timer.tic();
			for(int shift = 0; shift<8; shift++)
			{
				while(timer.toc_ms() < SHIFTTIME_MS)
				{
					for(int j=0; j<8; j++)
					{
							OE.high();
							Spi.wiringPiSPIDataRW(CHANNEL, shift2C( font[s.charAt(i)] , font[s.charAt(i+1)] , shift)[j] , 2);
							OE.low();
					}
				}
			}
		}
	}
  }
  
	byte[] shift2C(byte[] char1, byte[] char2, shiftAmount)
	{  
		byte[] char1ShiftedLeft = new byte[8];
		byte[] char2ShiftedRight = new byte[8];
		byte[] result = new byte[8];
		
		// shift char1 lines 1 column left
		for(int i=0; i<8; i++)
		{
			if((byte)(char1[i]<<1) >= -128)
				char1ShiftedLeft[i] = (byte)(char1[i]<<1);
			else
				char1ShiftedLeft[i] = (byte)0;
				
			if( (byte)(char2[i]>>(8-i)) >=0 )
				char2ShiftedRight[i] = (byte)(char2[i]>>(8-i))
			else
				char2ShiftedRight[i] = (byte)0;
				
			result[i] = char1ShiftedLeft[i] | char2ShiftedRight[i];
		}
		
		return result;
	}
	
	void addNewMessage(String message)
	{
		messages.add(message);
	}
	
}

public class Timer
{
	private long time_ms;

	void tic()
	{
		this.time_ms = System.currentTimeMillis();
	}
  
	int toc_ms()
	{
		return (int)( this.time_ms - System.currrentTimeMillis() );
	}
}