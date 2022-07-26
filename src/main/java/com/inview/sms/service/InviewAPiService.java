package com.inview.sms.service;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public class InviewAPiService {

	public HttpComponentsClientHttpRequestFactory getfactory()
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
				.loadTrustMaterial((X509Certificate[] chain, String authType) -> true).build();

		SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, (s, sslSession) -> true);
		CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		return requestFactory;
	}

	public RestTemplate getTemplate() {
		RestTemplate restTemplate = null;
		try {
			restTemplate = new RestTemplate(getfactory());
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			e.printStackTrace();
		}
		return restTemplate;

	}

	final RestTemplate restTemplate = getTemplate();

	String baseURl = "https://45.63.98.216:8877/ngbplatform/api/v1/";
	String boxActivation = "activationprocess/simpleactivation";
	String topup = "orders/topUp/";
	String pvod = "orders/TOVDtopUp";
	String entitleMentEndPoint = "orders/retrackOsdmessage";
	String validateHardware = "clients/search/serial_no?columnValue=";
	String getorderDetails = "orders/getRenewalOrderByClient?";
	String authStr = "integration:integration@2020";
	String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());

	
	  public String ActivationBoxwithBoxID(final String deviceID) {
	        final String activationUrl = String.valueOf(this.baseURl) + this.boxActivation;
	        final JSONObject clientData = new JSONObject();
	        clientData.put("boxId", (Object)deviceID);
	        final HttpHeaders headers = new HttpHeaders();
	        headers.add("X-Obs-Platform-TenantId", "default");
	        headers.add("Authorization", "Basic " + this.base64Creds);
	        headers.add("Content-Type", "application/json");
	        
	        
			HttpEntity<String> request = new HttpEntity<>(clientData.toString(), headers);
	        ResponseEntity<String> result = null;
	        try {
	            result = (ResponseEntity<String>)this.restTemplate.exchange(activationUrl, HttpMethod.POST, (HttpEntity)request, (Class)String.class, new Object[0]);
	        }
	        catch (RestClientResponseException e) {
				result = ResponseEntity.status(e.getRawStatusCode()).body(e.getResponseBodyAsString());
	        }
	        System.out.println(result);
	        final int status = result.getStatusCodeValue();
	        if (status == 200) {
	            return "The Unique Box ID 301234567890 has been successfully registered and activated";
	        }
	        return "Activation Failed";
	    }
	
	
	
	public String ActivationBox(String deviceID, String fristName, String lastName, String mobileNo, String city,
			String state) {

		JSONArray address = new JSONArray();

		JSONObject addressprimary = new JSONObject();
		addressprimary.put("addressNo", "");
		addressprimary.put("state", state);
		addressprimary.put("country", "Nigeria");
		addressprimary.put("city", city);
		addressprimary.put("postCode", "");
		addressprimary.put("countryCode", "NGA");
		addressprimary.put("addressType", "PRIMARY");

		JSONObject addressblilling = new JSONObject();
		addressblilling.put("addressNo", "");
		addressblilling.put("state", state);
		addressblilling.put("country", "Nigeria");
		addressblilling.put("city", city);
		addressblilling.put("postCode", "");
		addressblilling.put("countryCode", "NGA");
		addressblilling.put("addressType", "BILLING");

		address.add(addressprimary);
		address.add(addressblilling);

		JSONArray devices = new JSONArray();
		JSONObject device = new JSONObject();
		device.put("deviceId", deviceID);
		devices.add(device);

		JSONArray plans = new JSONArray();
		JSONObject plan = new JSONObject();
		plan.put("planCode", "BASE");
		plans.add(plan);

		JSONArray credentails = new JSONArray();
		JSONObject credentail = new JSONObject();
		credentail.put("username", fristName);
		credentail.put("password", "password123");
		credentails.add(credentail);

		JSONObject paymentInfo = new JSONObject();
		paymentInfo.put("paymentMethodID", "VOUCHER");
		JSONObject voucherCodes = new JSONObject();
		voucherCodes.put("voucherCode", deviceID);

		paymentInfo.put("voucherCodes", voucherCodes);

		JSONObject clientData = new JSONObject();
		clientData.put("forename", fristName);
		clientData.put("surname", lastName);
		clientData.put("gender", "male");
		clientData.put("email", fristName + "@gmail.com");
		clientData.put("mobile", mobileNo);
		clientData.put("dob", "");
		clientData.put("officeId", 3);
		clientData.put("address", address);
		clientData.put("devices", devices);
		clientData.put("plans", plans);
		clientData.put("credentails", credentails);
		clientData.put("refId", "12345");
		clientData.put("IsVerified", "NO");
		clientData.put("paymentInfo", paymentInfo);
		clientData.put("valid", true);
		clientData.put("salutation", "MR");

		address.clear();
		devices.clear();
		plans.clear();
		credentails.clear();

		String activationUrl = baseURl + boxActivation;

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Obs-Platform-TenantId", "default");
		headers.add("Authorization", "Basic " + base64Creds);
		headers.add("Content-Type", "application/json");

		HttpEntity<String> request = new HttpEntity<>(clientData.toString(), headers);
		ResponseEntity<String> result = null;
		try {
			result = restTemplate.exchange(activationUrl, HttpMethod.POST, request, String.class);
		} catch (RestClientResponseException e) {

			result = ResponseEntity.status(e.getRawStatusCode()).body(e.getResponseBodyAsString());
		}
		System.out.println(result);
		int status = result.getStatusCodeValue();

		if (status == 200) {
			return "The Unique Box ID 301234567890 has been successfully registered and activated";
		} else {
			String message = null;
			String devloperMessage = null;
			JSONObject exceptionJson = new JSONObject(result.getBody());
			org.json.JSONArray errorJson = exceptionJson.getJSONArray("errors");
			JSONObject error = errorJson.getJSONObject(0);
			devloperMessage = error.getString("developerMessage");

			if (devloperMessage.contains("PinNumber")) {
				message = "Inactive Voucher. The provider voucher" + deviceID + "is not active. Please try again";
			}
			if (devloperMessage.contains("SerialNumber")) {
				message = "You have sent an invalid Unique box ID number" + deviceID
						+ ". Please check your BOX ID number and try again";
			} else {
				message = "An error occurred. Kindly contact  07003887277 for additional help";
			}
			System.out.println(message);
			return message;

		}

	}

	// Completed
	public Long validateHardWare(String deviceId) {
		String validateHardWareURl = baseURl + validateHardware + deviceId;

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Obs-Platform-TenantId", "default");
		headers.add("Authorization", "Basic " + base64Creds);
		headers.add("Content-Type", "application/json");

		HttpEntity entity = new HttpEntity(headers);
		ResponseEntity<String> response = restTemplate.exchange(validateHardWareURl, HttpMethod.GET, entity,
				String.class);
		JSONObject responseBody = new JSONObject(response.getBody());
		Long clientID = responseBody.getLong("id");
		System.out.println(clientID);
		return clientID;
	}

	public String entitlementRefersh(Long clientID) {
		String refreshEntitlementUrl = baseURl + entitleMentEndPoint;

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Obs-Platform-TenantId", "default");
		headers.add("Authorization", "Basic " + base64Creds);
		headers.add("Content-Type", "application/json");

		JSONArray jsonArray = new JSONArray();
		JSONObject requestMessage = new JSONObject();
		requestMessage.put("Activation", "true");
		jsonArray.add(requestMessage);

		JSONObject refreshPayload = new JSONObject();
		refreshPayload.put("requestMessage", jsonArray);
		refreshPayload.put("requestType", "RETRACK");
		refreshPayload.put("clientServiceId", clientID);
		refreshPayload.put("type", "single");
		refreshPayload.put("clientId", clientID);
		jsonArray.clear();

		HttpEntity<String> request = new HttpEntity<>(refreshPayload.toString(), headers);
		ResponseEntity<String> result = null;
		try {
			result = restTemplate.exchange(refreshEntitlementUrl, HttpMethod.POST, request, String.class);
		} catch (RestClientResponseException e) {
			result = ResponseEntity.status(e.getRawStatusCode()).body(e.getResponseBodyAsString());
		}
		if (result.getStatusCode() == HttpStatus.OK) {
			System.out.println("successful operation");
			return "Request for entitlement refresh was successful";
		} else {
			System.out.println("Unsuccessful operation");
			return "The operation was unsuccessful. Please try again later";
		}
	}

	public Long getOrderDetails(Long clientId) {
		String orderURL = baseURl + getorderDetails + "clientId=" + clientId + "&planType=210";
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Obs-Platform-TenantId", "default");
		headers.add("Authorization", "Basic " + base64Creds);
		headers.add("Content-Type", "application/json");
		HttpEntity<?> request = new HttpEntity(headers);
		ResponseEntity<String> result = restTemplate.exchange(orderURL, HttpMethod.GET, request, String.class);
		JSONObject json = new JSONObject(result.getBody());
		Long orderid = json.getLong("id");
		System.out.println(orderid);
		return orderid;

	}

	public String topup(String deviceID, String voucherId, Long orderID) {

		String topupUrl = baseURl + topup + orderID;

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Obs-Platform-TenantId", "default");
		headers.add("Authorization", "Basic " + base64Creds);
		headers.add("Content-Type", "application/json");

		JSONObject paymentDetails = new JSONObject();
		paymentDetails.put("paymentType", "voucherPayment");
		paymentDetails.put("voucherId", voucherId);

		JSONObject topupPayload = new JSONObject();
		topupPayload.put("stbNo", deviceID);
		topupPayload.put("paymentDetails", paymentDetails);

		HttpEntity<String> request = new HttpEntity<>(topupPayload.toString(), headers);

		ResponseEntity<String> result = null;
		try {
			result = restTemplate.exchange(topupUrl, HttpMethod.POST, request, String.class);
		} catch (RestClientResponseException e) {
			result = ResponseEntity.status(e.getRawStatusCode()).body(e.getResponseBodyAsString());
		}
		if (result.getStatusCode() == HttpStatus.OK) {
			return "Digital TV Pass for your box " + deviceID + "was successful ";
		} else {
			String message = null;
			String devloperMessage = null;
			JSONObject exceptionJson = new JSONObject(result.getBody());
			org.json.JSONArray errorJson = exceptionJson.getJSONArray("errors");
			JSONObject error = errorJson.getJSONObject(0);
			devloperMessage = error.getString("developerMessage");

			if (devloperMessage.contains("PinNumber")) {
				message = "Invalid voucher. Please try again with a valid voucher";
			}
			if (devloperMessage.contains("SerialNumber")) {
				message = "You have sent an invalid Unique box ID number" + deviceID
						+ ". Please check your BOX ID number and try again";
			} else {
				message = "An error occurred. Please contact 07003887277 for additional help.";
			}
			System.out.println(message);
			return message;

		}

	}

	public String moviepurchase(String deviceID, String itemID, String voucherId) {
		String pvodURL = baseURl + pvod;

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Obs-Platform-TenantId", "default");
		headers.add("Authorization", "Basic " + base64Creds);
		headers.add("Content-Type", "application/json");

		JSONObject paymentDetails = new JSONObject();
		paymentDetails.put("paymentType", "voucherPayment");
		paymentDetails.put("voucherId", voucherId);

		JSONObject pvodPayload = new JSONObject();
		pvodPayload.put("stbNo", deviceID);
		pvodPayload.put("itemName", itemID);
		pvodPayload.put("paymentDetails", paymentDetails);

		HttpEntity<String> request = new HttpEntity<>(pvodPayload.toString(), headers);
		ResponseEntity<String> result = null;
		try {
			result = restTemplate.exchange(pvodURL, HttpMethod.POST, request, String.class);
		} catch (RestClientResponseException e) {
			result = ResponseEntity.status(e.getRawStatusCode()).body(e.getResponseBodyAsString());
		}
		System.out.println(result);
		if (result.getStatusCode() == HttpStatus.OK) {
			return "You have successfully purchased a new movie. Please leave your FreeTV STB switched on. You should be able to play your movie in a few minutes";
		} else {
			String message = null;
			String devloperMessage = null;
			JSONObject exceptionJson = new JSONObject(result.getBody());
			org.json.JSONArray errorJson = exceptionJson.getJSONArray("errors");
			JSONObject error = errorJson.getJSONObject(0);
			devloperMessage = error.getString("developerMessage");

			if (devloperMessage.contains("PinNumber")) {
				message = "The voucher number " + voucherId
						+ " you have sent is inactive. Please try again with a valid voucher nunber. Or call 07003887277 for help ";
			}
			if (devloperMessage.contains("SerialNumber")) {
				message = "The Box ID number" + deviceID
						+ "you have sent is invalid. Please check your Box ID number on your screen and try again";
			}
			if (devloperMessage.contains("movie")) {
				message = "The Movie ID number" + itemID
						+ " you have sent is incorrect.Please check your Movie ID number on your screen and try again";
			} else {
				message = "An error occurred. Please contact 07003887277 for additional help.";
			}
			System.out.println(message);
			return message;

		}

	}
	
	

	public static void main(String args[]) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		InviewAPiService service = new InviewAPiService();
		// service.ActivationBox("121212121212", "venkat", "akula", "9030989090",
		// "Gardesncity", "Lagos");

		service.topup("9100202020222", "SM467109136", 10l);
	}

}