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
import java.util.Map;
import java.util.regex.Pattern;

import spark.Spark;

import static spark.Spark.*;


public class Main {
    static String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
    static String authToken = System.getenv("TWILIO_AUTH_TOKEN");
    static String twilioNumber = System.getenv("TWILIO_PHONE_NUMBER");
    static String NGROK_BASE_URL = System.getenv("NGROK_URL");
    static String TWILIO_SEND_LIVE = System.getenv("TWILIO_SEND_LIVE");
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
        return portFromEnv(new ProcessBuilder().environment());
    }

    static int portFromEnv(Map<String, String> env) {
        String value = env.get("PORT");
        if (isBlank(value)) {
            return 4567;
        }
        try {
            int port = Integer.parseInt(value.trim());
            if (port > 0 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException numberFormatException) {
            return 4567;
        }
        return 4567;
    }

    static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && E164_PHONE_NUMBER.matcher(phoneNumber.trim()).matches();
    }

    static String callConfigurationError(String accountSid, String authToken, String twilioNumber, String ngrokBaseUrl) {
        return callConfigurationError(accountSid, authToken, twilioNumber, ngrokBaseUrl, true);
    }

    static String callConfigurationError(
            String accountSid,
            String authToken,
            String twilioNumber,
            String ngrokBaseUrl,
            boolean sendLive
    ) {
        List<String> missing = new ArrayList<>();
        if (sendLive && isBlank(accountSid)) {
            missing.add("TWILIO_ACCOUNT_SID");
        }
        if (sendLive && isBlank(authToken)) {
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
        if (!isValidCallbackBaseUrl(ngrokBaseUrl)) {
            return "NGROK_URL must be a valid https origin URL";
        }
        return null;
    }

    static boolean shouldSendLive(String value) {
        return value != null && value.trim().equalsIgnoreCase("true");
    }

    static String dialMessage(String phoneNumber, boolean dryRun) {
        String redactedPhoneNumber = redactPhoneNumber(phoneNumber);
        if (dryRun) {
            return "Dry run: would dial " + redactedPhoneNumber + " from your Twilio phone number...";
        }
        return "Dialing " + redactedPhoneNumber + " from your Twilio phone number...";
    }

    static String redactPhoneNumber(String phoneNumber) {
        String value = phoneNumber == null ? "" : phoneNumber.trim();
        if (value.length() <= 4) {
            return "****";
        }
        return repeat("*", value.length() - 4) + value.substring(value.length() - 4);
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    static URI twimlUri(String ngrokBaseUrl) {
        String trimmed = ngrokBaseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return URI.create(trimmed + "/twiml");
    }

    static boolean isValidCallbackBaseUrl(String ngrokBaseUrl) {
        if (isBlank(ngrokBaseUrl)) {
            return false;
        }
        try {
            URI uri = new URI(ngrokBaseUrl.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && !isBlank(uri.getHost())
                    && isBlank(uri.getRawUserInfo())
                    && isRootPath(uri)
                    && isBlank(uri.getRawQuery())
                    && isBlank(uri.getRawFragment());
        } catch (URISyntaxException uriSyntaxException) {
            return false;
        }
    }

    private static boolean isRootPath(URI uri) {
        String path = uri.getRawPath();
        return isBlank(path) || "/".equals(path);
    }

    static String twimlResponseXml() {
        Say sayHello = new Say.Builder(
                "JAVA RC, JAVA RC, YES IT'S THE JAVA-RC, HURRAY FOR THE JAVA RC, LET'S GO JAVA RC"
        ).build();
        Play playSong = new Play.Builder(
                "https://api.twilio.com/cowbell.mp3"
        ).build();
        VoiceResponse voiceResponse = new VoiceResponse.Builder()
                .say(sayHello)
                .play(playSong)
                .build();
        return voiceResponse.toXml();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static void main(String[] args) {

        port(getHerokuAssignedPort());

        Spark.staticFiles.location("/public");

        // twiml endpoint
        post("/twiml", (request, response) -> {
            response.type("application/xml");
            return twimlResponseXml();
        });

        // this endpoint handles dialing outbound phone calls with the TwilioRestClient object
        get("/dial-phone", (request, response) -> {
            String phoneNumber = request.queryParams("number");//request.params(":number");
            if (!isValidPhoneNumber(phoneNumber)) {
                response.status(400);
                return "Hey, you need to enter a valid E.164 phone number in the URL!";
            }

            boolean sendLive = shouldSendLive(TWILIO_SEND_LIVE);
            String configurationError = callConfigurationError(
                    accountSid,
                    authToken,
                    twilioNumber,
                    NGROK_BASE_URL,
                    sendLive
            );
            if (configurationError != null) {
                response.status(503);
                return configurationError;
            }

            if (!sendLive) {
                return dialMessage(phoneNumber, true);
            }

            PhoneNumber to = new PhoneNumber(phoneNumber.trim());
            PhoneNumber from = new PhoneNumber(twilioNumber.trim());
            URI uri = twimlUri(NGROK_BASE_URL);
            TwilioRestClient client = new TwilioRestClient.Builder(accountSid, authToken).build();

            // Make the call using the TwilioRestClient we instantiated
            new CallCreator(to, from, uri).create(client);
            return dialMessage(phoneNumber, false);
        });
    }
}
