package org.example;

import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MainTest {
    @Test
    public void validatesE164PhoneNumbers() {
        assertTrue(Main.isValidPhoneNumber("+123456"));
        assertFalse(Main.isValidPhoneNumber(null));
        assertFalse(Main.isValidPhoneNumber(""));
        assertFalse(Main.isValidPhoneNumber("   "));
        assertFalse(Main.isValidPhoneNumber("123456"));
        assertFalse(Main.isValidPhoneNumber("+12abc"));
    }

    @Test
    public void defaultsInvalidAssignedPorts() {
        Map<String, String> env = new HashMap<>();
        assertEquals(4567, Main.portFromEnv(env));

        env.put("PORT", " 5000 ");
        assertEquals(5000, Main.portFromEnv(env));

        env.put("PORT", "not-a-port");
        assertEquals(4567, Main.portFromEnv(env));

        env.put("PORT", "0");
        assertEquals(4567, Main.portFromEnv(env));

        env.put("PORT", "-1");
        assertEquals(4567, Main.portFromEnv(env));

        env.put("PORT", "65536");
        assertEquals(4567, Main.portFromEnv(env));
    }

    @Test
    public void reportsMissingCallConfiguration() {
        assertEquals(
                "Missing required configuration: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER, NGROK_URL",
                Main.callConfigurationError(null, "", "   ", null)
        );
    }

    @Test
    public void dryRunConfigurationDoesNotRequireTwilioCredentials() {
        assertNull(Main.callConfigurationError(null, "", "+123456", "https://example.ngrok.io", false));
    }

    @Test
    public void liveConfigurationRequiresTwilioCredentials() {
        assertEquals(
                "Missing required configuration: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN",
                Main.callConfigurationError(null, "", "+123456", "https://example.ngrok.io", true)
        );
    }

    @Test
    public void rejectsInvalidConfiguredTwilioNumber() {
        assertEquals(
                "TWILIO_PHONE_NUMBER must be a valid E.164 phone number",
                Main.callConfigurationError("sid", "token", "not-a-number", "https://example.ngrok.io")
        );
    }

    @Test
    public void rejectsInsecureCallbackBaseUrl() {
        assertEquals(
                "NGROK_URL must be a valid https origin URL",
                Main.callConfigurationError("sid", "token", "+123456", "http://example.ngrok.io")
        );
    }

    @Test
    public void rejectsMalformedCallbackBaseUrl() {
        assertEquals(
                "NGROK_URL must be a valid https origin URL",
                Main.callConfigurationError("sid", "token", "+123456", "https://")
        );
        assertEquals(
                "NGROK_URL must be a valid https origin URL",
                Main.callConfigurationError("sid", "token", "+123456", "not a url")
        );
    }

    @Test
    public void rejectsCallbackBaseUrlsWithPathQueryFragmentOrUserInfo() {
        assertFalse(Main.isValidCallbackBaseUrl("https://example.ngrok.io/path"));
        assertFalse(Main.isValidCallbackBaseUrl("https://example.ngrok.io?token=secret"));
        assertFalse(Main.isValidCallbackBaseUrl("https://example.ngrok.io#fragment"));
        assertFalse(Main.isValidCallbackBaseUrl("https://user@example.ngrok.io"));
    }

    @Test
    public void acceptsCompleteCallConfiguration() {
        assertNull(Main.callConfigurationError("sid", "token", "+123456", "https://example.ngrok.io"));
    }

    @Test
    public void validatesCallbackBaseUrls() {
        assertTrue(Main.isValidCallbackBaseUrl(" https://example.ngrok.io "));
        assertTrue(Main.isValidCallbackBaseUrl("https://example.ngrok.io/"));
        assertFalse(Main.isValidCallbackBaseUrl(null));
        assertFalse(Main.isValidCallbackBaseUrl(""));
        assertFalse(Main.isValidCallbackBaseUrl("https://"));
        assertFalse(Main.isValidCallbackBaseUrl("http://example.ngrok.io"));
        assertFalse(Main.isValidCallbackBaseUrl("not a url"));
    }

    @Test
    public void buildsTwimlUriWithSingleSlash() {
        URI uri = Main.twimlUri("https://example.ngrok.io/");
        assertEquals("https://example.ngrok.io/twiml", uri.toString());
    }

    @Test
    public void buildsTwimlXmlResponse() {
        String xml = Main.twimlResponseXml();

        assertTrue(xml.contains("<Response>"));
        assertTrue(xml.contains("<Say>JAVA RC"));
        assertTrue(xml.contains("https://api.twilio.com/cowbell.mp3"));
    }

    @Test
    public void twimlRouteRequiresPostAndReturnsXml() throws Exception {
        HttpServer server = Main.startServer(0);
        try {
            int port = server.getAddress().getPort();
            HttpResponse getResponse = request(port, "GET", "/twiml", null);
            HttpResponse postResponse = request(port, "POST", "/twiml", "");

            assertEquals(405, getResponse.status);
            assertEquals("POST", getResponse.allow);
            assertEquals(200, postResponse.status);
            assertTrue(postResponse.contentType.startsWith("application/xml"));
            assertTrue(postResponse.body.contains("<Response>"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void dialPhoneRouteRequiresFormPostAndKeepsDryRunDefault() throws Exception {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        Main.twilioNumber = "+15551234567";
        Main.NGROK_BASE_URL = "https://example.ngrok.io";
        Main.TWILIO_SEND_LIVE = "false";
        HttpServer server = Main.startServer(0);
        try {
            int port = server.getAddress().getPort();
            HttpResponse getResponse = request(port, "GET", "/dial-phone", null);
            HttpResponse invalidResponse = request(port, "POST", "/dial-phone", "number=invalid");
            HttpResponse dryRunResponse = request(port, "POST", "/dial-phone", "number=%2B15551231234");

            assertEquals(405, getResponse.status);
            assertEquals("POST", getResponse.allow);
            assertEquals(400, invalidResponse.status);
            assertEquals(200, dryRunResponse.status);
            assertEquals(
                    "Dry run: would dial ********1234 from your Twilio phone number...",
                    dryRunResponse.body
            );
        } finally {
            server.stop(0);
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
        }
    }

    @Test
    public void rootRouteServesTheUpdatedForm() throws Exception {
        HttpServer server = Main.startServer(0);
        try {
            HttpResponse response = request(server.getAddress().getPort(), "GET", "/", null);

            assertEquals(200, response.status);
            assertTrue(response.contentType.startsWith("text/html"));
            assertTrue(response.body.contains("Twilio Java voice testing"));
            assertTrue(response.body.contains("method=\"post\" action=\"/dial-phone\""));
            assertEquals("no-store", response.cacheControl);
            assertEquals("DENY", response.frameOptions);
            assertEquals("no-referrer", response.referrerPolicy);
            assertEquals("camera=(), geolocation=(), microphone=()", response.permissionsPolicy);
            assertTrue(response.contentSecurityPolicy.contains("default-src 'none'"));
            assertTrue(response.contentSecurityPolicy.contains("form-action 'self'"));
            assertTrue(response.contentSecurityPolicy.contains("frame-ancestors 'none'"));
            assertTrue(response.contentSecurityPolicy.contains("https://cdn.jsdelivr.net"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void dialPhoneFormSubmitsWithPost() throws Exception {
        String html = new String(
                Files.readAllBytes(Paths.get("src/main/resources/public/index.html")),
                StandardCharsets.UTF_8
        );

        assertTrue(html.contains("<form method=\"post\" action=\"/dial-phone\">"));
        assertFalse(html.contains("<form method=\"get\" action=\"/dial-phone\">"));
    }

    @Test
    public void dialPhoneFormRequiresPhoneNumber() throws Exception {
        String html = new String(
                Files.readAllBytes(Paths.get("src/main/resources/public/index.html")),
                StandardCharsets.UTF_8
        );

        assertTrue(html.contains(
                "id=\"number\" name=\"number\" placeholder=\"+ country code and number\" "
                        + "pattern=\"^\\+[1-9][0-9]{1,14}$\" required"
        ));
        assertTrue(html.contains(
                "id=\"dialToken\" name=\"dialToken\" placeholder=\"Live dial token\""
        ));
    }

    @Test
    public void liveSendFlagRequiresExplicitTrue() {
        assertFalse(Main.shouldSendLive(null));
        assertFalse(Main.shouldSendLive(""));
        assertFalse(Main.shouldSendLive("false"));
        assertTrue(Main.shouldSendLive(" TRUE "));
    }

    @Test
    public void liveDialAuthorizationRequiresAnExactConfiguredToken() {
        assertFalse(Main.authorizedDialToken(null, "secret"));
        assertFalse(Main.authorizedDialToken("", "secret"));
        assertFalse(Main.authorizedDialToken("secret", null));
        assertFalse(Main.authorizedDialToken("secret", "wrong"));
        assertTrue(Main.authorizedDialToken("secret", "secret"));
    }

    @Test
    public void liveDialRejectsMissingOrIncorrectAuthorizationBeforeCallingTwilio() {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        try {
            Main.twilioNumber = "+15551234567";
            Main.NGROK_BASE_URL = "https://example.ngrok.io";
            Main.TWILIO_SEND_LIVE = "true";
            Main.accountSid = "sid";
            Main.authToken = "token";

            Main.TWILIO_DIAL_TOKEN = null;
            assertEquals(503, Main.dialPhone("+15557654321", null).status);

            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            assertEquals(403, Main.dialPhone("+15557654321", "wrong").status);
        } finally {
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
            Main.TWILIO_DIAL_TOKEN = originalDialToken;
            Main.accountSid = originalAccountSid;
            Main.authToken = originalAuthToken;
        }
    }

    @Test
    public void liveDialHidesTwilioProviderFailureDetails() {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        try {
            Main.twilioNumber = "+15551234567";
            Main.NGROK_BASE_URL = "https://example.ngrok.io";
            Main.TWILIO_SEND_LIVE = "true";
            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            Main.accountSid = "sid";
            Main.authToken = "token";
            Main.HttpResult result = Main.dialPhone(
                    "+15557654321",
                    "dial-secret",
                    (to, from, callbackUri) -> {
                        throw new RuntimeException("provider response included auth-token-secret");
                    }
            );

            assertEquals(502, result.status);
            assertEquals("Twilio call request failed.", result.body);
            assertFalse(result.body.contains("auth-token-secret"));
        } finally {
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
            Main.TWILIO_DIAL_TOKEN = originalDialToken;
            Main.accountSid = originalAccountSid;
            Main.authToken = originalAuthToken;
        }
    }

    @Test
    public void describesDryRunAndLiveDialMessages() {
        assertEquals(
                "Dry run: would dial ***3456 from your Twilio phone number...",
                Main.dialMessage("+123456", true)
        );
        assertEquals(
                "Dialing ***3456 from your Twilio phone number...",
                Main.dialMessage("+123456", false)
        );
    }

    @Test
    public void redactsDialTargetsInDryRunMessages() {
        String message = Main.dialMessage("+1234567890", true);

        assertFalse(message.contains("+1234567890"));
        assertTrue(message.contains("******7890"));
    }

    @Test
    public void redactsShortDialTargets() {
        assertEquals("****", Main.redactPhoneNumber("+123"));
    }

    @Test
    public void invalidDialTargetMessageMatchesPostFormSubmission() throws Exception {
        assertEquals(
                "Hey, you need to enter a valid E.164 phone number.",
                Main.invalidDialTargetMessage()
        );

        String source = new String(
                Files.readAllBytes(Paths.get("src/main/java/org/example/Main.java")),
                StandardCharsets.UTF_8
        );
        assertFalse(source.contains("in the URL"));
    }

    @Test
    public void defaultsLog4jToInfoInsteadOfDebug() throws Exception {
        String properties = new String(
                Files.readAllBytes(Paths.get("src/main/resources/log4j.properties")),
                StandardCharsets.UTF_8
        );

        assertTrue(properties.contains("log4j.rootLogger=info, stdout"));
        assertFalse(properties.contains("log4j.rootLogger=debug, stdout"));
    }

    @Test
    public void pomDoesNotIncludeUnusedLegacyDependencies() throws Exception {
        String pom = new String(
                Files.readAllBytes(Paths.get("pom.xml")),
                StandardCharsets.UTF_8
        );

        assertFalse(pom.contains("<artifactId>spark-streaming_2.10</artifactId>"));
        assertFalse(pom.contains("<artifactId>velocity</artifactId>"));
        assertFalse(pom.contains("webjars-"));
    }

    private static HttpResponse request(int port, String method, String path, String body)
            throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "http://127.0.0.1:" + port + path
        ).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        if (body != null) {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=utf-8"
            );
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bodyBytes);
            }
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        String responseBody = stream == null ? "" : readAll(stream);
        HttpResponse response = new HttpResponse(
                status,
                connection.getHeaderField("Content-Type"),
                connection.getHeaderField("Allow"),
                connection.getHeaderField("Cache-Control"),
                connection.getHeaderField("Content-Security-Policy"),
                connection.getHeaderField("Permissions-Policy"),
                connection.getHeaderField("Referrer-Policy"),
                connection.getHeaderField("X-Frame-Options"),
                responseBody
        );
        connection.disconnect();
        return response;
    }

    private static String readAll(InputStream input) throws Exception {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static final class HttpResponse {
        final int status;
        final String contentType;
        final String allow;
        final String cacheControl;
        final String contentSecurityPolicy;
        final String permissionsPolicy;
        final String referrerPolicy;
        final String frameOptions;
        final String body;

        HttpResponse(
                int status,
                String contentType,
                String allow,
                String cacheControl,
                String contentSecurityPolicy,
                String permissionsPolicy,
                String referrerPolicy,
                String frameOptions,
                String body
        ) {
            this.status = status;
            this.contentType = contentType;
            this.allow = allow;
            this.cacheControl = cacheControl;
            this.contentSecurityPolicy = contentSecurityPolicy;
            this.permissionsPolicy = permissionsPolicy;
            this.referrerPolicy = referrerPolicy;
            this.frameOptions = frameOptions;
            this.body = body;
        }
    }
}
