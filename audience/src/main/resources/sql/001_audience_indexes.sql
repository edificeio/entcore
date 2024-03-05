CREATE INDEX ON audience.reactions (module);
CREATE INDEX ON audience.reactions (resource_type);
CREATE INDEX ON audience.reactions (resource_id);
CREATE INDEX ON audience.reactions (user_id);
CREATE INDEX ON audience.reactions (reaction_date);

CREATE INDEX ON audience.views (module);
CREATE INDEX ON audience.views (resource_type);
CREATE INDEX ON audience.views (resource_id);
CREATE INDEX ON audience.views (user_id);
CREATE INDEX ON audience.views (last_view);