 match (u:User) where has(u.deleteDate) AND has(u.IDPN) set u.IDPN = null;

