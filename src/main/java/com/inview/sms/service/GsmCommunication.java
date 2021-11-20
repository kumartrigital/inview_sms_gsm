package com.inview.sms.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

@Component
public class GsmCommunication implements Runnable, SerialPortEventListener {

	static CommPortIdentifier portId;
	static Enumeration portList;
	static InputStream inputStream;
	SerialPort serialPort;
	Thread readThread;
	static OutputStream outputStream;
	String s1 = "at";
	String s2 = "\r\n";
	String receviedMobileNo = null;
	String sendingMessage = null;

	public void portStart(SpringApplication application) {
		portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().equals("COM7")) {
					GsmCommunication reader = new GsmCommunication("Reading app");

				}
			}
		}
	}

	public GsmCommunication() {

	}

	public GsmCommunication(String name) {
		try {
			serialPort = (SerialPort) portId.open(name, 2000);
		} catch (PortInUseException e) {
			System.out.println(e);
		}
		try {
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();
			System.out.println("Streaming");
		} catch (IOException e) {
			System.out.println(e);
		}
		try {
			System.out.println("config Listener");
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			System.out.println(e);
		}
		serialPort.notifyOnDataAvailable(true);
		try {
			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException e) {
			System.out.println(e);
		}
		readThread = new Thread(this);
		readThread.start();
	}

	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.out.println(e);
		}
	}

	public void serialEvent(SerialPortEvent event) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			byte[] readBuffer = new byte[20];

			try {

				while (inputStream.available() > 0) {
					int numBytes = inputStream.read(readBuffer);
				}
				System.out.println(new String(readBuffer));
				String readBufferNotification = new String(readBuffer);

				if (readBufferNotification.contains("SM")) {
					String messagepos = readBufferNotification.substring(14, 16);

					GsmCommunication gsmCommunication = new GsmCommunication();
					Thread consumer = new Thread(new Runnable() {
						@Override
						public void run() {
							gsmCommunication.modeSetup();
							sendingMessage = readMessage(messagepos);
							gsmCommunication.deleteMessage(messagepos);

						}
					});
					Thread producer = new Thread(new Runnable() {
						@Override
						public void run() {
							if (!(sendingMessage == null)) {
								gsmCommunication.sendMessage(receviedMobileNo, sendingMessage);
							} else {
								System.out.println("SMS did'nt send to cust");
							}

						}
					});

					consumer.start();
					consumer.join();
					producer.start();

				}
			} catch (Exception e) {
				System.out.println(e);
			}
			break;
		}

	}

	public void modeSetup() {
		try {
			outputStream.write(s1.getBytes());
			outputStream.write(s2.getBytes());
			Thread.sleep(1000);
			outputStream.write("AT +CMGF=1".getBytes());
			outputStream.write(s2.getBytes());
			byte[] readBuffering = new byte[20];

			String result = null;
			while (inputStream.available() > 0) {
				int numbyte = inputStream.read(readBuffering);
				result = new String(readBuffering);
			}

			System.out.println("mode : Setting to TEXT");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String readMessage(String messagepos) {

		try {
			String recievedmessage = "AT +CMGR=" + messagepos;

			outputStream.write(recievedmessage.getBytes());
			outputStream.write(s2.getBytes());
			Thread.sleep(1000);

			String readingMessage = null;
			byte[] readBuffering = new byte[200];

			while (inputStream.available() > 0) {
				int numBytes = inputStream.read(readBuffering);

				readingMessage = new String(readBuffering);

				System.out.println(numBytes);
			}

			System.out.println(readingMessage);
			Pattern pattern = Pattern.compile("\\d{12}");
			Matcher matcher = pattern.matcher(readingMessage);

			if (matcher.find()) {
				receviedMobileNo = matcher.group(0);
				System.out.println(receviedMobileNo);
			}

			String[] result = readingMessage.split("\\R");
			String message = result[2];
			System.out.println("Incomig Message From Cust:" + message);
			String[] messageSplit = message.split(" ");
			int messageLength = messageSplit.length;
			System.out.println(messageLength);

			InviewAPiService inviewAPiService = new InviewAPiService();

			if (message.startsWith("RF")) {
				String deviceId = messageSplit[1];
				Long clientID = inviewAPiService.validateHardWare(deviceId);
				sendingMessage = inviewAPiService.entitlementRefersh(clientID);

			} else if (message.startsWith("PVOD")) {
				String deviceID = messageSplit[1];
				String itemID = messageSplit[2];
				String voucherId = messageSplit[3];
				sendingMessage = inviewAPiService.moviepurchase(deviceID, itemID, voucherId);

			} else if (messageLength == 2) {
				String deviceId = messageSplit[0];
				Long clientId = inviewAPiService.validateHardWare(deviceId);
				Long orderId = inviewAPiService.getOrderDetails(clientId);
				String voucherId = messageSplit[1];
				sendingMessage = inviewAPiService.topup(deviceId, voucherId, orderId);

			} else if (messageLength == 6) {
				String deviceID = messageSplit[0];
				String fristName = messageSplit[1];
				String lastName = messageSplit[2];

				String mobileNo = messageSplit[3];
				String city = messageSplit[4];
				String state = messageSplit[5];

				sendingMessage = inviewAPiService.ActivationBox(deviceID, fristName, lastName, mobileNo, city, state);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sendingMessage;
	}

	public void deleteMessage(String messagepos) {
		try {
			String delteMessage = "AT +CMGD=" + messagepos;
			outputStream.write(delteMessage.getBytes());
			outputStream.write(s2.getBytes());
			Thread.sleep(1000);
			byte[] deletingBuffering = new byte[20];

			while (inputStream.available() > 0) {
				int numBytes = inputStream.read(deletingBuffering);
				System.out.println(numBytes);
			}

			System.out.println("Deleted: " + new String(deletingBuffering));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(String mobileNo, String message) {
		try {
			outputStream.write(s1.getBytes());
			outputStream.write(s2.getBytes());
			Thread.sleep(1000);
			outputStream.write("AT +CMGF=1".getBytes());
			outputStream.write(s2.getBytes());
			Thread.sleep(1000);
			String SendMessage = "AT +CMGS=" + mobileNo;
			Thread.sleep(1000);

			outputStream.write(SendMessage.getBytes());
			outputStream.write(s2.getBytes());
			Thread.sleep(1000);
			outputStream.write(message.getBytes());
			outputStream.write(s2.getBytes());
			Thread.sleep(1000);

			byte[] readBuffering = new byte[20];

			String result = new String(readBuffering);
			System.out.println("sending mesage to " + mobileNo + " " + result);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
