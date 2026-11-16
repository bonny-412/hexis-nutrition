CREATE TABLE notifiche (
    id UUID PRIMARY KEY,
    professionista_id UUID NOT NULL REFERENCES professionisti(id),
    appuntamento_id UUID NOT NULL REFERENCES appuntamenti(id) ON DELETE CASCADE,
    scatta_il TIMESTAMPTZ NOT NULL,
    creata_il TIMESTAMPTZ NOT NULL DEFAULT now(),
    letta_il TIMESTAMPTZ,
    agenda_vista_il TIMESTAMPTZ,
    CONSTRAINT uq_notifiche_appuntamento_scatto UNIQUE (appuntamento_id, scatta_il)
);

CREATE INDEX idx_notifiche_professionista ON notifiche(professionista_id);

CREATE TABLE push_subscriptions (
    id UUID PRIMARY KEY,
    professionista_id UUID NOT NULL REFERENCES professionisti(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL UNIQUE,
    p256dh TEXT NOT NULL,
    auth TEXT NOT NULL,
    user_agent VARCHAR(300),
    creata_il TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_push_subscriptions_professionista ON push_subscriptions(professionista_id);
