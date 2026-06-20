package com.easetrading.api.alert;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Delivers alert notifications. For Prompt 1-4 this logs in-app (the frontend shows
 * TRIGGERED alerts). Email (SES/SMTP) and web push are slotted in here behind the
 * same method, so adding channels doesn't change the alert engine.
 */
@Service
public class NotificationService {

    public void notifyUser(UUID userId, String message) {
        // In-app: the alert is already marked TRIGGERED and visible in the UI.
        System.out.printf("[NOTIFY] user=%s : %s%n", userId, message);
        // TODO: emailSender.send(...) / webPush.send(...) — same method, more channels.
    }
}
