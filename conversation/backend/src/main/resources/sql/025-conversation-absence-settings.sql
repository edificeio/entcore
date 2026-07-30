-- Message d'absence : paramétrage par utilisateur (période + texte riche).
-- La PK sur user_id porte le principe d'unicité de la FS (un seul message d'absence actif
-- ou programmé à la fois) : aucune règle applicative à écrire pour la garantir.
-- La désactivation passe enabled à FALSE sans supprimer la ligne, pour permettre une
-- réactivation sans ressaisie (US-3).
-- Pas d'index supplémentaire : user_id étant la clé primaire, son index sert déjà la
-- détection en lot de US-2 (WHERE user_id = ANY($1)).

CREATE TABLE IF NOT EXISTS conversation.absence_settings (
	"user_id" VARCHAR(36) NOT NULL PRIMARY KEY,
	"start_at" TIMESTAMP WITH TIME ZONE NOT NULL,
	"end_at" TIMESTAMP WITH TIME ZONE NOT NULL,
	"enabled" BOOLEAN NOT NULL DEFAULT FALSE,
	"body_json" JSONB NOT NULL,
	"body_html" TEXT NOT NULL,
	-- DEFAULT ne s'applique qu'à l'INSERT : la branche DO UPDATE de l'upsert
	-- PUT /conversation/absence doit poser updated_at = now() explicitement.
	"updated_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
