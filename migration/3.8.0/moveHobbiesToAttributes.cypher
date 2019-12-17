//split in two because neo4j cant handle large amount of changes

MATCH (h:Hobby)<-[t]-(ub:UserBook)
 WITH ub,COLLECT (DISTINCT {values:h.values,visibility:type(t),category:h.category}) as hobbies
 WITH ub,
 FILTER(x IN hobbies WHERE x.category='music')[0] as found_music,
 FILTER(x IN hobbies WHERE x.category='places')[0] as found_places,
 FILTER(x IN hobbies WHERE x.category='books')[0] as found_books
 SET
 ub.hobby_music = [COALESCE(found_music.visibility,'PRIVE'),COALESCE(found_music.values,'')] ,
 ub.hobby_places = [COALESCE(found_places.visibility,'PRIVE'),COALESCE(found_places.values,'')] ,
 ub.hobby_books = [COALESCE(found_books.visibility,'PRIVE'),COALESCE(found_books.values,'')]
 RETURN COUNT(ub);

//second query
MATCH (h:Hobby)<-[t]-(ub:UserBook)
 WITH ub,COLLECT (DISTINCT {values:h.values,visibility:type(t),category:h.category}) as hobbies
 WITH ub,
 FILTER(x IN hobbies WHERE x.category='sport')[0] as found_sport,
 FILTER(x IN hobbies WHERE x.category='cinema')[0] as found_cinema,
 FILTER(x IN hobbies WHERE x.category='animals')[0] as found_animals
 SET
 ub.hobby_sport = [COALESCE(found_sport.visibility,'PRIVE'),COALESCE(found_sport.values,'')] ,
 ub.hobby_cinema = [COALESCE(found_cinema.visibility,'PRIVE'),COALESCE(found_cinema.values,'')] ,
 ub.hobby_animals = [COALESCE(found_animals.visibility,'PRIVE'),COALESCE(found_animals.values,'')] 
 RETURN COUNT(ub)