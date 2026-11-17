package com.safayet.afsos_nama.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PilotAccess {

    private final String inviteSecret;
    private final Set<String> allowedEmails;
    private final String privacyContact;

    public PilotAccess(
            @Value("${app.pilot.invite-secret:}") String inviteSecret,
            @Value("${app.pilot.allowed-emails:}") String allowedEmails,
            @Value("${app.pilot.privacy-contact:}") String privacyContact
    ) {
        this.inviteSecret = inviteSecret;
        this.allowedEmails = Arrays.stream(allowedEmails.split(","))
                .map(PilotAccess::normalizeEmail)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.privacyContact = privacyContact.strip();
    }

    public boolean allowsRegistration(String email, String suppliedCode) {
        String recipient = normalizeEmail(email);
        if (inviteSecret.isBlank() || suppliedCode == null || !allowedEmails.contains(recipient)) {
            return false;
        }
        try {
            Mac signer = Mac.getInstance("HmacSHA256");
            signer.init(new SecretKeySpec(inviteSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = signer.doFinal(recipient.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(signature);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                                         suppliedCode.strip().getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Invitation signing is unavailable", exception);
        }
    }

    public String privacyContact() {
        return privacyContact;
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
