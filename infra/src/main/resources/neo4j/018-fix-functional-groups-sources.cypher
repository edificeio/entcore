MATCH (s:Structure)<-[:DEPENDS]-(fg)
WHERE (fg:FunctionalGroup OR fg:FunctionGroup) AND NOT(HAS(fg.source))
SET fg.source = CASE WHEN s.timetable = '' THEN s.source ELSE coalesce(s.timetable, s.source) END;