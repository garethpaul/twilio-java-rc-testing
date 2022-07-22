package org.example;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Call;
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

import spark.Spark;

import static spark.Spark.*;


public class Main {
    static String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
    static String authToken = System.getenv("TWILIO_AUTH_TOKEN");
    static String twilioNumber = System.getenv("TWILIO_PHONE_NUMBER");
    static String NGROK_BASE_URL = System.getenv("NGROK_URL");


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


    public static void main(String[] args) {

        port(getHerokuAssignedPort());


        Helper helper = new Helper();

        // instantiate the TwilioRestClient helper library with our Twilio credentials set as constants
        TwilioRestClient client = new TwilioRestClient.Builder(accountSid, authToken).build();

        Spark.staticFiles.location("/public");

        // twiml endpoint
        post("/twiml", (request, response) -> {
            // generate the TwiML response to tell Twilio what to do
            Say sayHello = new Say.Builder("Hello from Twilio, Java 8 and Spark!").build();
            Play playSong = new Play.Builder("https://api.twilio.com/cowbell.mp3").build();
            VoiceResponse voiceResponse = new VoiceResponse.Builder().say(sayHello).play(playSong).build();
            return voiceResponse.toXml();
        });

        // this endpoint handles dialing outbound phone calls with the TwilioRestClient object
        get("/dial-phone/:number", (request, response) -> {
            String phoneNumber = request.params(":number");
            /* as long as the phone number is not blank or null, we'll attempt to dial it, but
               you can add more exception handling here */
            if (!phoneNumber.isEmpty()) {
                PhoneNumber to = new PhoneNumber(phoneNumber);
                PhoneNumber from = new PhoneNumber(twilioNumber);
                URI uri = URI.create(NGROK_BASE_URL + "/twiml");

                // Make the call using the TwilioRestClient we instantiated
                Call call = new CallCreator(to, from, uri).create(client);
                return "Dialing " + phoneNumber + " from your Twilio phone number...";
            } else {
                return "Hey, you need to enter a valid phone number in the URL!";
            }
        });
    }
}