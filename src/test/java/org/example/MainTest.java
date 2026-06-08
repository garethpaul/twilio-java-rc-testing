package org.example;

import org.junit.Test;

import java.net.URI;

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
    public void reportsMissingCallConfiguration() {
        assertEquals(
                "Missing required configuration: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER, NGROK_URL",
                Main.callConfigurationError(null, "", "   ", null)
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
                "NGROK_URL must start with https://",
                Main.callConfigurationError("sid", "token", "+123456", "http://example.ngrok.io")
        );
    }

    @Test
    public void acceptsCompleteCallConfiguration() {
        assertNull(Main.callConfigurationError("sid", "token", "+123456", "https://example.ngrok.io"));
    }

    @Test
    public void buildsTwimlUriWithSingleSlash() {
        URI uri = Main.twimlUri("https://example.ngrok.io/");
        assertEquals("https://example.ngrok.io/twiml", uri.toString());
    }
}
