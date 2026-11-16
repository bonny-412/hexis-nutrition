package com.hexisnutrition.backend.notifiche;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Un dispositivo (browser) del professionista registrato per ricevere le Web Push. */
@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "professionista_id", nullable = false)
    private UUID professionistaId;

    @Column(nullable = false, unique = true)
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "creata_il", nullable = false)
    private Instant creataIl = Instant.now();

    protected PushSubscription() {
    }

    public PushSubscription(UUID professionistaId, String endpoint, String p256dh, String auth, String userAgent) {
        this.professionistaId = professionistaId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.userAgent = userAgent;
    }

    public UUID getId() { return id; }
    public UUID getProfessionistaId() { return professionistaId; }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuth() { return auth; }

    public void aggiorna(UUID professionistaId, String p256dh, String auth, String userAgent) {
        this.professionistaId = professionistaId;
        this.p256dh = p256dh;
        this.auth = auth;
        this.userAgent = userAgent;
    }
}
