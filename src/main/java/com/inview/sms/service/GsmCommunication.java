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
import org.apache.commons.configuration.PropertiesConfiguration;

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
		try {
			portList = CommPortIdentifier.getPortIdentifiers();
			String currentDir = System.getProperty("user.dir");
			PropertiesConfiguration prop = new PropertiesConfiguration(currentDir + "/SmsGsmApplication.ini");
			String portName = prop.getString("PORT_NAME").trim();
			while (portList.hasMoreElements()) {
				portId = (CommPortIdentifier) portList.nextElement();
				if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					if (portId.getName().equals(portName)) { // if (portId.getName().equals("COM7")) {
						System.out.println("Connecting to port :" + portName);

						GsmCommunication reader = new GsmCommunication("Reading app");

					}
				}
			}
		} catch (org.apache.commons.configuration.ConfigurationException e) {
			System.out.println("(ConfigurationException) Properties file loading error.... : " + e.getMessage());
			e.printStackTrace();
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
			final String recievedmessage = "AT +CMGR=" + messagepos;
			GsmCommunication.outputStream.write(recievedmessage.getBytes());
			GsmCommunication.outputStream.write(this.s2.getBytes());
			Thread.sleep(1000L);
			String readingMessage = null;
			final byte[] readBuffering = new byte[200];
			while (GsmCommunication.inputStream.available() > 0) {
				final int numBytes = GsmCommunication.inputStream.read(readBuffering);
				readingMessage = new String(readBuffering);
			}
			System.out.println(readingMessage);
			final Pattern pattern = Pattern.compile("\\d{12}");
			final Matcher matcher = pattern.matcher(readingMessage);
			if (matcher.find()) {
				this.receviedMobileNo = matcher.group(0);
			}
			final String trimingmessage = readingMessage.replace("OK", "");
			final String removingEmptyLinesfromMessage = trimingmessage.replaceAll("(?m)^[ \t]*\r?\n", "");
			final String[] lines = removingEmptyLinesfromMessage.split("\\n");
			final String message = lines[1];
			System.out.println("Incomig Message From Cust:" + message);
			final String[] messageSplit = message.split(" ");
			final int messageLength = messageSplit.length;
			System.out.println(messageLength);
			final InviewAPiService inviewAPiService = new InviewAPiService();
			final GsmCommunication gsmCommunication = new GsmCommunication();
			gsmCommunication.deleteMessage(messagepos);

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
				final String deviceId = messageSplit[0];
				final Long clientId = inviewAPiService.validateHardWare(deviceId);
				final Long orderId = inviewAPiService.getOrderDetails(clientId);
				final String voucherId2 = messageSplit[1].substring(0, 18);
				System.out.println(voucherId2);
				System.out.println(voucherId2);
				this.sendingMessage = inviewAPiService.topup(deviceId, voucherId2, orderId);
			} else if (messageLength == 6) {
				String deviceID = messageSplit[0];
				String fristName = messageSplit[1];
				String lastName = messageSplit[2];

				String mobileNo = messageSplit[3];
				String city = messageSplit[4];
				String state = messageSplit[5];

				sendingMessage = inviewAPiService.ActivationBox(deviceID, fristName, lastName, mobileNo, city, state);
			} else if (messageLength == 1) {
				final String deviceID = messageSplit[0];
				this.sendingMessage = inviewAPiService.ActivationBoxwithBoxID(deviceID);
			} else if (message.toUpperCase().startsWith("DTV")) {
				System.out.println("DTV pass ");
				final String deviceID = messageSplit[1];
				final Long clientId = inviewAPiService.validateHardWare(deviceID);
				final Long orderId = inviewAPiService.getOrderDetails(clientId);
				final String voucherId2 = messageSplit[2].substring(0, 18);
				System.out.println(voucherId2);
				this.sendingMessage = inviewAPiService.topup(deviceID, voucherId2, orderId);
			} else {
				System.out.println("invalid pattern");
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
