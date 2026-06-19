package org.example;

import com.sun.net.httpserver.HttpServer;
import com.twilio.http.HttpClient;
import com.twilio.http.HttpMethod;
import com.twilio.http.Request;
import com.twilio.http.Response;
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
    private static final int LOOPBACK_TIMEOUT_MILLIS = 10_000;

    @Test
    public void validatesE164PhoneNumbers() {
        assertTrue(Main.isValidPhoneNumber("+123456"));
        assertFalse(Main.isValidPhoneNumber(null));
        assertFalse(Main.isValidPhoneNumber(""));
        assertFalse(Main.isValidPhoneNumber("   "));
        assertFalse(Main.isValidPhoneNumber("123456"));
        assertFalse(Main.isValidPhoneNumber("+12abc"));
        assertFalse(Main.isValidPhoneNumber(" +123456"));
        assertFalse(Main.isValidPhoneNumber("+123456 "));
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
    public void liveConfigurationRejectsMalformedTwilioAccountSids() {
        assertEquals(
                "TWILIO_ACCOUNT_SID must be a valid Twilio Account SID",
                Main.callConfigurationError(
                        "not-an-account-sid",
                        "token",
                        "+123456",
                        "https://example.ngrok.io",
                        true
                )
        );
    }

    @Test
    public void rejectsInvalidConfiguredTwilioNumber() {
        assertEquals(
                "TWILIO_PHONE_NUMBER must be a valid E.164 phone number",
                Main.callConfigurationError("AC00000000000000000000000000000000", "token", "not-a-number", "https://example.ngrok.io")
        );
    }

    @Test
    public void rejectsInsecureCallbackBaseUrl() {
        assertEquals(
                "NGROK_URL must be a valid https origin URL",
                Main.callConfigurationError("AC00000000000000000000000000000000", "token", "+123456", "http://example.ngrok.io")
        );
    }

    @Test
    public void rejectsMalformedCallbackBaseUrl() {
        assertEquals(
                "NGROK_URL must be a valid https origin URL",
                Main.callConfigurationError("AC00000000000000000000000000000000", "token", "+123456", "https://")
        );
        assertEquals(
                "NGROK_URL must be a valid https origin URL",
                Main.callConfigurationError("AC00000000000000000000000000000000", "token", "+123456", "not a url")
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
        assertNull(Main.callConfigurationError("AC00000000000000000000000000000000", "token", "+123456", "https://example.ngrok.io"));
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
    public void dialPhoneRouteRejectsOversizedFormBodies() throws Exception {
        StringBuilder body = new StringBuilder("number=");
        while (body.length() <= 9 * 1024) {
            body.append('1');
        }

        HttpServer server = Main.startServer(0);
        try {
            HttpResponse response = request(
                    server.getAddress().getPort(),
                    "POST",
                    "/dial-phone",
                    body.toString()
            );

            assertEquals(413, response.status);
            assertEquals("Form submission is too large.", response.body);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void dialPhoneRouteRequiresTheExactFormMediaType() throws Exception {
        HttpServer server = Main.startServer(0);
        try {
            int port = server.getAddress().getPort();
            HttpResponse missing = request(port, "POST", "/dial-phone", null);
            HttpResponse unrelated = request(
                    port,
                    "POST",
                    "/dial-phone",
                    "number=invalid",
                    "text/plain"
            );
            HttpResponse spoofed = request(
                    port,
                    "POST",
                    "/dial-phone",
                    "number=invalid",
                    "application/x-www-form-urlencoded-evil"
            );
            HttpResponse parameterized = request(
                    port,
                    "POST",
                    "/dial-phone",
                    "number=invalid",
                    "Application/X-WWW-Form-Urlencoded; charset=UTF-8"
            );

            assertEquals(415, missing.status);
            assertEquals(415, unrelated.status);
            assertEquals(415, spoofed.status);
            assertEquals(400, parameterized.status);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void dialPhoneRouteRejectsAmbiguousOrMalformedRelevantFormFields() throws Exception {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        Main.resetLiveDialRateLimit();
        HttpServer server = Main.startServer(0);
        try {
            Main.twilioNumber = "+15551234567";
            Main.NGROK_BASE_URL = "https://example.ngrok.io";
            Main.TWILIO_SEND_LIVE = "true";
            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            Main.accountSid = null;
            Main.authToken = null;
            int port = server.getAddress().getPort();

            String[] invalidBodies = {
                    "number=%2B15557654321&number=%2B15550000000&dialToken=dial-secret",
                    "number=%2B15557654321&dialToken=dial-secret&dialToken=wrong",
                    "number=%2B15557654321&dialToken=dial-secret"
                            + "&requestId=11111111-1111-4111-8111-111111111111"
                            + "&requestId=22222222-2222-4222-8222-222222222222",
                    "num%ZZber=%2B15557654321&dialToken=dial-secret",
                    "number=%ZZ&dialToken=dial-secret",
                    "number=%2B15557654321&submit&dialToken=dial-secret"
            };
            for (String body : invalidBodies) {
                HttpResponse response = request(port, "POST", "/dial-phone", body);
                assertEquals(400, response.status);
                assertEquals("Invalid form submission.", response.body);
                assertFalse(response.body.contains("TWILIO_"));
            }
        } finally {
            server.stop(0);
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
            Main.TWILIO_DIAL_TOKEN = originalDialToken;
            Main.accountSid = originalAccountSid;
            Main.authToken = originalAuthToken;
            Main.resetLiveDialRateLimit();
        }
    }

    @Test
    public void dialPhoneRouteIgnoresUnknownFormFields() throws Exception {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        HttpServer server = Main.startServer(0);
        try {
            Main.twilioNumber = "+15551234567";
            Main.NGROK_BASE_URL = "https://example.ngrok.io";
            Main.TWILIO_SEND_LIVE = "false";

            HttpResponse response = request(
                    server.getAddress().getPort(),
                    "POST",
                    "/dial-phone",
                    "number=%2B15557654321&submit=Dial"
            );

            assertEquals(200, response.status);
            assertTrue(response.body.startsWith("Dry run: would dial "));
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
            assertFalse(response.body.contains("__LIVE_DIAL_REQUEST_ID__"));
            assertTrue(response.body.matches(
                    "(?s).*name=\"requestId\" value=\"[0-9a-f-]{36}\".*"
            ));
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
        assertTrue(html.contains(
                "name=\"requestId\" value=\"__LIVE_DIAL_REQUEST_ID__\""
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
    public void liveDialAuthorizationPrecedesProviderConfigurationValidation() {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        int[] senderCalls = {0};
        try {
            Main.twilioNumber = null;
            Main.NGROK_BASE_URL = null;
            Main.TWILIO_SEND_LIVE = "true";
            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            Main.accountSid = null;
            Main.authToken = null;

            Main.HttpResult unauthorized = Main.dialPhone(
                    "+15557654321",
                    "wrong",
                    (to, from, callbackUri) -> senderCalls[0]++
            );
            Main.HttpResult authorized = Main.dialPhone(
                    "+15557654321",
                    "dial-secret",
                    (to, from, callbackUri) -> senderCalls[0]++
            );

            assertEquals(403, unauthorized.status);
            assertEquals("Invalid dial authorization token.", unauthorized.body);
            assertFalse(unauthorized.body.contains("TWILIO_"));
            assertEquals(503, authorized.status);
            assertTrue(authorized.body.contains("TWILIO_ACCOUNT_SID"));
            assertEquals(0, senderCalls[0]);
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
    public void liveDialRouteDoesNotDiscloseProviderConfigurationBeforeAuthorization() throws Exception {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        HttpServer server = Main.startServer(0);
        try {
            Main.twilioNumber = null;
            Main.NGROK_BASE_URL = null;
            Main.TWILIO_SEND_LIVE = "true";
            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            Main.accountSid = null;
            Main.authToken = null;

            HttpResponse response = request(
                    server.getAddress().getPort(),
                    "POST",
                    "/dial-phone",
                    "number=%2B15557654321&dialToken=wrong"
            );

            assertEquals(403, response.status);
            assertEquals("Invalid dial authorization token.", response.body);
            assertFalse(response.body.contains("TWILIO_"));
        } finally {
            server.stop(0);
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
            Main.TWILIO_DIAL_TOKEN = originalDialToken;
            Main.accountSid = originalAccountSid;
            Main.authToken = originalAuthToken;
        }
    }

    @Test
    public void liveDialRateLimiterAllowsFiveAttemptsAndResetsAfterOneMinute() {
        Main.LiveDialRateLimiter limiter = new Main.LiveDialRateLimiter(5, 60_000L);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertTrue(limiter.tryAcquire(1_000L));
        }
        assertFalse(limiter.tryAcquire(60_999L));
        assertTrue(limiter.tryAcquire(61_000L));
    }

    @Test
    public void liveDialRouteRateLimitsOnlyAfterParsingAndAuthorization() throws Exception {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        Main.resetLiveDialRateLimit();
        Main.currentTimeMillis = () -> 1_000L;
        HttpServer server = Main.startServer(0);
        try {
            Main.twilioNumber = "+15551234567";
            Main.NGROK_BASE_URL = "https://example.ngrok.io";
            Main.TWILIO_SEND_LIVE = "true";
            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            Main.accountSid = "AC00000000000000000000000000000000";
            Main.authToken = "00000000000000000000000000000000";
            int port = server.getAddress().getPort();
            for (int attempt = 0; attempt < 5; attempt++) {
                Main.HttpResult response = Main.dialPhone(
                        "+15557654321",
                        "dial-secret",
                        String.format("00000000-0000-0000-0000-%012d", attempt),
                        (to, from, callbackUri) -> { }
                );
                assertEquals(200, response.status);
            }

            HttpResponse limited = request(
                    port,
                    "POST",
                    "/dial-phone",
                    "number=%2B15557654321&dialToken=dial-secret"
                            + "&requestId=00000000-0000-0000-0000-000000000005"
            );

            assertEquals(429, limited.status);
            assertEquals("60", limited.retryAfter);
            assertEquals("Too many live dial attempts.", limited.body);
        } finally {
            server.stop(0);
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
            Main.TWILIO_DIAL_TOKEN = originalDialToken;
            Main.accountSid = originalAccountSid;
            Main.authToken = originalAuthToken;
            Main.resetLiveDialRateLimit();
        }
    }

    @Test
    public void unauthorizedLiveDialRequestsDoNotConsumeTheAuthorizedRateLimit() throws Exception {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        Main.resetLiveDialRateLimit();
        HttpServer server = Main.startServer(0);
        try {
            Main.twilioNumber = "+15551234567";
            Main.NGROK_BASE_URL = "https://example.ngrok.io";
            Main.TWILIO_SEND_LIVE = "true";
            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            Main.accountSid = null;
            Main.authToken = null;
            int port = server.getAddress().getPort();

            for (int attempt = 0; attempt < 6; attempt++) {
                HttpResponse unauthorized = request(
                        port,
                        "POST",
                        "/dial-phone",
                        "number=%2B15557654321&dialToken=wrong"
                );
                assertEquals(403, unauthorized.status);
            }

            HttpResponse authorized = request(
                    port,
                    "POST",
                    "/dial-phone",
                    "number=%2B15557654321&dialToken=dial-secret"
            );

            assertEquals(503, authorized.status);
            assertTrue(authorized.body.contains("TWILIO_ACCOUNT_SID"));
        } finally {
            server.stop(0);
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
            Main.TWILIO_DIAL_TOKEN = originalDialToken;
            Main.accountSid = originalAccountSid;
            Main.authToken = originalAuthToken;
            Main.resetLiveDialRateLimit();
        }
    }

    @Test
    public void dryRunRouteIgnoresExhaustedLiveDialRateLimit() throws Exception {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        Main.resetLiveDialRateLimit();
        Main.currentTimeMillis = () -> 1_000L;
        HttpServer server = Main.startServer(0);
        try {
            Main.twilioNumber = "+15551234567";
            Main.NGROK_BASE_URL = "https://example.ngrok.io";
            Main.TWILIO_SEND_LIVE = "true";
            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            Main.accountSid = "AC00000000000000000000000000000000";
            Main.authToken = "00000000000000000000000000000000";
            for (int attempt = 0; attempt < 5; attempt++) {
                assertEquals(
                        200,
                        Main.dialPhone(
                                "+15557654321",
                                "dial-secret",
                                String.format("10000000-0000-0000-0000-%012d", attempt),
                                (to, from, callbackUri) -> { }
                        ).status
                );
            }

            Main.TWILIO_SEND_LIVE = "false";
            HttpResponse dryRun = request(
                    server.getAddress().getPort(),
                    "POST",
                    "/dial-phone",
                    "number=%2B15557654321"
            );

            assertEquals(200, dryRun.status);
            assertTrue(dryRun.body.startsWith("Dry run:"));
        } finally {
            server.stop(0);
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
            Main.TWILIO_DIAL_TOKEN = originalDialToken;
            Main.accountSid = originalAccountSid;
            Main.authToken = originalAuthToken;
            Main.resetLiveDialRateLimit();
        }
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
            Main.accountSid = "AC00000000000000000000000000000000";
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
            Main.accountSid = "AC00000000000000000000000000000000";
            Main.authToken = "token";
            Main.HttpResult result = Main.dialPhone(
                    "+15557654321",
                    "dial-secret",
                    "22222222-2222-4222-8222-222222222222",
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
    public void liveDialRequestIdPreventsASecondProviderAttemptAfterAnUnknownOutcome() {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        int[] senderCalls = {0};
        Main.resetLiveDialRateLimit();
        try {
            Main.twilioNumber = "+15551234567";
            Main.NGROK_BASE_URL = "https://example.ngrok.io";
            Main.TWILIO_SEND_LIVE = "true";
            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            Main.accountSid = "AC00000000000000000000000000000000";
            Main.authToken = "00000000000000000000000000000000";

            Main.HttpResult first = Main.dialPhone(
                    "+15557654321",
                    "dial-secret",
                    "11111111-1111-4111-8111-111111111111",
                    (to, from, callbackUri) -> {
                        senderCalls[0]++;
                        throw new RuntimeException("provider response was lost");
                    }
            );
            Main.HttpResult duplicate = Main.dialPhone(
                    "+15557654321",
                    "dial-secret",
                    "11111111-1111-4111-8111-111111111111",
                    (to, from, callbackUri) -> senderCalls[0]++
            );

            assertEquals(502, first.status);
            assertEquals(409, duplicate.status);
            assertEquals("Duplicate live dial request.", duplicate.body);
            assertEquals(1, senderCalls[0]);
        } finally {
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
            Main.TWILIO_DIAL_TOKEN = originalDialToken;
            Main.accountSid = originalAccountSid;
            Main.authToken = originalAuthToken;
            Main.resetLiveDialRateLimit();
        }
    }

    @Test
    public void liveDialRequiresAnExplicitRequestIdBeforeCallingTheProvider() {
        String originalNumber = Main.twilioNumber;
        String originalBaseUrl = Main.NGROK_BASE_URL;
        String originalSendLive = Main.TWILIO_SEND_LIVE;
        String originalDialToken = Main.TWILIO_DIAL_TOKEN;
        String originalAccountSid = Main.accountSid;
        String originalAuthToken = Main.authToken;
        int[] senderCalls = {0};
        Main.resetLiveDialRateLimit();
        try {
            Main.twilioNumber = "+15551234567";
            Main.NGROK_BASE_URL = "https://example.ngrok.io";
            Main.TWILIO_SEND_LIVE = "true";
            Main.TWILIO_DIAL_TOKEN = "dial-secret";
            Main.accountSid = "AC00000000000000000000000000000000";
            Main.authToken = "00000000000000000000000000000000";

            Main.HttpResult result = Main.dialPhone(
                    "+15557654321",
                    "dial-secret",
                    (to, from, callbackUri) -> senderCalls[0]++
            );

            assertEquals(400, result.status);
            assertEquals("Missing or invalid live dial request ID.", result.body);
            assertEquals(0, senderCalls[0]);
        } finally {
            Main.twilioNumber = originalNumber;
            Main.NGROK_BASE_URL = originalBaseUrl;
            Main.TWILIO_SEND_LIVE = originalSendLive;
            Main.TWILIO_DIAL_TOKEN = originalDialToken;
            Main.accountSid = originalAccountSid;
            Main.authToken = originalAuthToken;
            Main.resetLiveDialRateLimit();
        }
    }

    @Test
    public void twilioTransportMakesOnlyOneAttemptForAProviderFailure() {
        int[] networkAttempts = {0};
        HttpClient delegate = new HttpClient() {
            @Override
            public Response makeRequest(Request request) {
                networkAttempts[0]++;
                return new Response("{}", 503);
            }
        };
        Main.SingleAttemptHttpClient client = new Main.SingleAttemptHttpClient(delegate);

        Response response = client.reliableRequest(
                new Request(HttpMethod.POST, "https://api.twilio.com/test")
        );

        assertEquals(503, response.getStatusCode());
        assertEquals(1, networkAttempts[0]);
    }

    @Test
    public void twilioClientUsesTheSingleAttemptTransport() {
        assertTrue(
                Main.buildTwilioRestClient(
                        "AC00000000000000000000000000000000",
                        "00000000000000000000000000000000"
                ).getHttpClient() instanceof Main.SingleAttemptHttpClient
        );
    }

    @Test
    public void twilioClientDisablesApacheAutomaticRetries() throws Exception {
        String source = new String(
                Files.readAllBytes(Paths.get("src/main/java/org/example/Main.java")),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains(".disableAutomaticRetries()"));
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
        assertTrue(pom.contains("<artifactId>slf4j-nop</artifactId>"));
    }

    private static HttpResponse request(int port, String method, String path, String body)
            throws Exception {
        return request(
                port,
                method,
                path,
                body,
                "application/x-www-form-urlencoded; charset=utf-8"
        );
    }

    private static HttpResponse request(
            int port,
            String method,
            String path,
            String body,
            String contentType
    ) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "http://127.0.0.1:" + port + path
        ).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(LOOPBACK_TIMEOUT_MILLIS);
        connection.setReadTimeout(LOOPBACK_TIMEOUT_MILLIS);
        if (body != null) {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            if (contentType != null) {
                connection.setRequestProperty("Content-Type", contentType);
            }
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
                connection.getHeaderField("Retry-After"),
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
        final String retryAfter;
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
                String retryAfter,
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
            this.retryAfter = retryAfter;
            this.body = body;
        }
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
