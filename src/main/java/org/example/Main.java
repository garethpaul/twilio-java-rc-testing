package org.example;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.CallCreator;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Play;
import com.twilio.twiml.voice.Say;
import com.twilio.type.PhoneNumber;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Main {
    static String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
    static String authToken = System.getenv("TWILIO_AUTH_TOKEN");
    static String twilioNumber = System.getenv("TWILIO_PHONE_NUMBER");
    static String NGROK_BASE_URL = System.getenv("NGROK_URL");
    static String TWILIO_SEND_LIVE = System.getenv("TWILIO_SEND_LIVE");
    static String TWILIO_DIAL_TOKEN = System.getenv("TWILIO_DIAL_TOKEN");
    private static final Pattern E164_PHONE_NUMBER = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final int MAX_FORM_BYTES = 8 * 1024;
    interface CallSender {
        void create(PhoneNumber to, PhoneNumber from, URI callbackUri);
    }


    static private String renderContent(String htmlFile) {
        try (InputStream input = Main.class.getResourceAsStream(htmlFile)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing classpath resource: " + htmlFile);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
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

    static boolean authorizedDialToken(String configuredToken, String providedToken) {
        if (isBlank(configuredToken) || providedToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    static String dialMessage(String phoneNumber, boolean dryRun) {
        String redactedPhoneNumber = redactPhoneNumber(phoneNumber);
        if (dryRun) {
            return "Dry run: would dial " + redactedPhoneNumber + " from your Twilio phone number...";
        }
        return "Dialing " + redactedPhoneNumber + " from your Twilio phone number...";
    }

    static String invalidDialTargetMessage() {
        return "Hey, you need to enter a valid E.164 phone number.";
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

    static HttpServer startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/twiml", Main::handleTwiml);
        server.createContext("/dial-phone", Main::handleDialPhone);
        server.createContext("/", Main::handleStaticContent);
        server.start();
        return server;
    }

    private static void handleTwiml(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) {
            return;
        }
        sendResponse(exchange, 200, "application/xml", twimlResponseXml());
    }

    private static void handleDialPhone(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) {
            return;
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/x-www-form-urlencoded")) {
            sendResponse(exchange, 415, "text/plain; charset=utf-8", "Expected form data.");
            return;
        }
        String phoneNumber;
        String dialToken;
        try {
            byte[] requestBody = readRequestBody(exchange);
            phoneNumber = formValue(requestBody, "number");
            dialToken = formValue(requestBody, "dialToken");
        } catch (RequestTooLargeException requestTooLargeException) {
            sendResponse(exchange, 413, "text/plain; charset=utf-8", "Form submission is too large.");
            return;
        }
        HttpResult result = dialPhone(phoneNumber, dialToken);
        sendResponse(exchange, result.status, "text/plain; charset=utf-8", result.body);
    }

    private static void handleStaticContent(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) {
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path)) {
            sendResponse(exchange, 200, "text/html; charset=utf-8", renderContent("/public/index.html"));
        } else if ("/test.html".equals(path)) {
            sendResponse(exchange, 200, "text/html; charset=utf-8", renderContent("/public/test.html"));
        } else {
            sendResponse(exchange, 404, "text/plain; charset=utf-8", "Not found.");
        }
    }

    static HttpResult dialPhone(String phoneNumber) {
        return dialPhone(phoneNumber, null);
    }

    static HttpResult dialPhone(String phoneNumber, String dialToken) {
        return dialPhone(phoneNumber, dialToken, Main::createTwilioCall);
    }

    static HttpResult dialPhone(String phoneNumber, String dialToken, CallSender callSender) {
        if (!isValidPhoneNumber(phoneNumber)) {
            return new HttpResult(400, invalidDialTargetMessage());
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
            return new HttpResult(503, configurationError);
        }
        if (!sendLive) {
            return new HttpResult(200, dialMessage(phoneNumber, true));
        }
        if (isBlank(TWILIO_DIAL_TOKEN)) {
            return new HttpResult(503, "Missing required configuration: TWILIO_DIAL_TOKEN");
        }
        if (!authorizedDialToken(TWILIO_DIAL_TOKEN, dialToken)) {
            return new HttpResult(403, "Invalid dial authorization token.");
        }

        PhoneNumber to = new PhoneNumber(phoneNumber.trim());
        PhoneNumber from = new PhoneNumber(twilioNumber.trim());
        URI uri = twimlUri(NGROK_BASE_URL);
        try {
            callSender.create(to, from, uri);
        } catch (RuntimeException providerError) {
            return new HttpResult(502, "Twilio call request failed.");
        }
        return new HttpResult(200, dialMessage(phoneNumber, false));
    }

    private static void createTwilioCall(PhoneNumber to, PhoneNumber from, URI uri) {
        TwilioRestClient client = new TwilioRestClient.Builder(accountSid, authToken).build();
        new CallCreator(to, from, uri).create(client);
    }

    private static boolean requireMethod(HttpExchange exchange, String method) throws IOException {
        if (method.equals(exchange.getRequestMethod())) {
            return true;
        }
        exchange.getResponseHeaders().set("Allow", method);
        sendResponse(exchange, 405, "text/plain; charset=utf-8", "Method not allowed.");
        return false;
    }

    private static byte[] readRequestBody(HttpExchange exchange)
            throws IOException, RequestTooLargeException {
        try (InputStream input = exchange.getRequestBody();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_FORM_BYTES) {
                    throw new RequestTooLargeException();
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static String formValue(byte[] body, String expectedName) {
        String form = new String(body, StandardCharsets.UTF_8);
        for (String pair : form.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && expectedName.equals(decodeFormComponent(parts[0]))) {
                return decodeFormComponent(parts[1]);
            }
        }
        return null;
    }

    private static String decodeFormComponent(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException | IOException invalidEncoding) {
            return null;
        }
    }

    private static void sendResponse(
            HttpExchange exchange,
            int status,
            String contentType,
            String body
    ) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set(
                "Content-Security-Policy",
                "default-src 'none'; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                        + "form-action 'self'; frame-ancestors 'none'; base-uri 'none'"
        );
        exchange.getResponseHeaders().set("Permissions-Policy", "camera=(), geolocation=(), microphone=()");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.sendResponseHeaders(status, responseBytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(responseBytes);
        }
    }

    static final class HttpResult {
        final int status;
        final String body;

        HttpResult(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private static final class RequestTooLargeException extends Exception {
    }

    public static void main(String[] args) throws IOException {
        startServer(getHerokuAssignedPort());
    }
}
