package org.example;

import org.junit.Test;

import java.net.URI;
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
    public void twimlRouteDeclaresXmlContentType() throws Exception {
        String source = new String(
                Files.readAllBytes(Paths.get("src/main/java/org/example/Main.java")),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("response.type(\"application/xml\");"));
    }

    @Test
    public void liveSendFlagRequiresExplicitTrue() {
        assertFalse(Main.shouldSendLive(null));
        assertFalse(Main.shouldSendLive(""));
        assertFalse(Main.shouldSendLive("false"));
        assertTrue(Main.shouldSendLive(" TRUE "));
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
    public void defaultsLog4jToInfoInsteadOfDebug() throws Exception {
        String properties = new String(
                Files.readAllBytes(Paths.get("src/main/resources/log4j.properties")),
                StandardCharsets.UTF_8
        );

        assertTrue(properties.contains("log4j.rootLogger=info, stdout"));
        assertFalse(properties.contains("log4j.rootLogger=debug, stdout"));
    }
}
