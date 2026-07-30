-- Message d'absence : garde-fou anti-spam « une réponse automatique par jour et par
-- expéditeur ». Une ligne par paire (personne absente, expéditeur), alimentée en upsert,
-- donc pas de croissance proportionnelle au nombre d'envois.
-- La PK porte le couple : elle sert à la fois le test avant émission et l'upsert après.
-- Le test du jour s'évalue dans le fuseau de l'expéditeur, fourni par le champ timezone
-- optionnel de POST /conversation/send, avec repli sur le fuseau serveur :
--   date_trunc('day', last_sent_at AT TIME ZONE $tz) = date_trunc('day', now() AT TIME ZONE $tz)
-- Pas d'index supplémentaire : la détection interroge un expéditeur unique contre un lot
-- de personnes absentes, ce que l'index de la PK couvre par sa colonne de tête.

CREATE TABLE IF NOT EXISTS conversation.absence_replies (
	"absent_user_id" VARCHAR(36) NOT NULL,
	"sender_id" VARCHAR(36) NOT NULL,
	"last_sent_at" TIMESTAMP WITH TIME ZONE NOT NULL,
	PRIMARY KEY ("absent_user_id", "sender_id")
);
