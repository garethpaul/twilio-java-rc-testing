package org.example;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.CallCreator;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Play;
import com.twilio.twiml.voice.Say;
import com.twilio.type.PhoneNumber;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import spark.Spark;

import static spark.Spark.*;


public class Main {
    static String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
    static String authToken = System.getenv("TWILIO_AUTH_TOKEN");
    static String twilioNumber = System.getenv("TWILIO_PHONE_NUMBER");
    static String NGROK_BASE_URL = System.getenv("NGROK_URL");
    private static final Pattern E164_PHONE_NUMBER = Pattern.compile("^\\+[1-9]\\d{1,14}$");


    static private String renderContent(String htmlFile) {
        try {
            return new String(Files.readAllBytes(Paths.get(Main.class.getResource(htmlFile).toURI())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
    }

    static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && E164_PHONE_NUMBER.matcher(phoneNumber.trim()).matches();
    }

    static String callConfigurationError(String accountSid, String authToken, String twilioNumber, String ngrokBaseUrl) {
        List<String> missing = new ArrayList<>();
        if (isBlank(accountSid)) {
            missing.add("TWILIO_ACCOUNT_SID");
        }
        if (isBlank(authToken)) {
            missing.add("TWILIO_AUTH_TOKEN");
        }
        if (isBlank(twilioNumber)) {
            missing.add("TWILIO_PHONE_NUMBER");
        }
        if (isBlank(ngrokBaseUrl)) {
            missing.add("NGROK_URL");
        }
        if (!missing.isEmpty()) {
            return "Missing required configuration: " + String.join(", ", missing);
        }
        if (!isValidPhoneNumber(twilioNumber)) {
            return "TWILIO_PHONE_NUMBER must be a valid E.164 phone number";
        }
        if (!ngrokBaseUrl.trim().startsWith("https://")) {
            return "NGROK_URL must start with https://";
        }
        return null;
    }

    static URI twimlUri(String ngrokBaseUrl) {
        String trimmed = ngrokBaseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return URI.create(trimmed + "/twiml");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static void main(String[] args) {

        port(getHerokuAssignedPort());

        Spark.staticFiles.location("/public");

        // twiml endpoint
        post("/twiml", (request, response) -> {
            // generate the TwiML response to tell Twilio what to do
            Say sayHello = new Say.Builder("JAVA RC, JAVA RC, YES IT'S THE JAVA-RC, HURRAY FOR THE JAVA RC, LET'S GO JAVA RC").build();
            Play playSong = new Play.Builder("https://api.twilio.com/cowbell.mp3").build();
            VoiceResponse voiceResponse = new VoiceResponse.Builder().say(sayHello).play(playSong).build();
            return voiceResponse.toXml();
        });

        // this endpoint handles dialing outbound phone calls with the TwilioRestClient object
        get("/dial-phone", (request, response) -> {
            String phoneNumber = request.queryParams("number");//request.params(":number");
            if (!isValidPhoneNumber(phoneNumber)) {
                response.status(400);
                return "Hey, you need to enter a valid E.164 phone number in the URL!";
            }

            String configurationError = callConfigurationError(accountSid, authToken, twilioNumber, NGROK_BASE_URL);
            if (configurationError != null) {
                response.status(503);
                return configurationError;
            }

            PhoneNumber to = new PhoneNumber(phoneNumber.trim());
            PhoneNumber from = new PhoneNumber(twilioNumber.trim());
            URI uri = twimlUri(NGROK_BASE_URL);
            TwilioRestClient client = new TwilioRestClient.Builder(accountSid, authToken).build();

            // Make the call using the TwilioRestClient we instantiated
            new CallCreator(to, from, uri).create(client);
            return "Dialing " + phoneNumber.trim() + " from your Twilio phone number...";
        });
    }
}
