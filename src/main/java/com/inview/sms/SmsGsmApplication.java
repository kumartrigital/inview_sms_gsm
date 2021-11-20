package com.inview.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.inview.sms.service.GsmCommunication;

@SpringBootApplication
public class SmsGsmApplication {

	static GsmCommunication gsm = new GsmCommunication();

	public static void main(String[] args) {
		SpringApplication.run(SmsGsmApplication.class, args);

		SpringApplication application = new SpringApplication(SmsGsmApplication.class);

		gsm.portStart(application);
	}

}
