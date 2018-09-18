match (u1:User)-[r:DUPLICATE]->(u2:User) where has(u1.password) and has(u2.password) delete r;
