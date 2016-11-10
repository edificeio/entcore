MATCH (s:Structure) WHERE not has(s.quota) SET s.quota = 0  return s;
MATCH (s:Structure) WHERE not has(s.storage) SET s.storage = 0  return s;
MATCH (s:Structure) WHERE not has(s.maxquota) SET s.maxquota = 0  return s;

MATCH (p:ProfileGroup) WHERE not has(p.quota) SET p.quota = 0 return p;
MATCH (p:ProfileGroup) WHERE not has(p.storage) SET p.storage = 0 return p;
MATCH (p:ProfileGroup) WHERE not has(p.maxquota) SET p.maxquota = 0 return p;