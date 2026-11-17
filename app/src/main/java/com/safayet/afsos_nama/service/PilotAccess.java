package com.safayet.afsos_nama.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Shared contact settings and email normalization. Registration is public. */
@Component
public class PilotAccess {

    private final String privacyContact;

    public PilotAccess(@Value("${app.pilot.privacy-contact:}") String privacyContact) {
        this.privacyContact = privacyContact.strip();
    }

    public String privacyContact() {
        return privacyContact;
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
