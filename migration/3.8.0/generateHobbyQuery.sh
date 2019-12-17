text="MATCH (h:Hobby)<-[t]-(ub:UserBook)\n
WITH ub,COLLECT (DISTINCT {values:h.values,visibility:type(t),category:h.category}) as hobbies\n
WITH ub, "

for var in "$@"
do
    text=$text"\n FILTER(x IN hobbies WHERE x.category='$var')[0] as found_$var,"
done

text=${text::-1}
text=$text"\n SET"

for var in "$@"
do
    text=$text"\n ub.hobby_$var = [COALESCE(found_$var.visibility,'PRIVE'),COALESCE(found_$var.values,'')] ,"
done

text=${text::-1}"\n RETURN COUNT(ub)"

echo -e $text