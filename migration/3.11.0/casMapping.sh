echo "var result = \`" > migrateCas.js
curl -H accept:application/json -H content-type:application/json -d '{"statements":[{"statement":"MATCH (a:Application) WHERE a.casType <> \"\" RETURN DISTINCT a.casType, a.pattern;"}]}' http://localhost:7474/db/data/transaction/commit  >> migrateCas.js 

script='\`;
var duplicates = []
var slug = function (s) {
    var res = original = s.replace(/[^\w\s]/gi, " ").replace(/https?/, "").replace(/\s\s+/g, " ").trim().replace(/\s/g, "-");
    var counter = 0;
    while (duplicates.indexOf(res) > -1) {
        console.log("already exists : ", res)
        res = original + "-" + (counter++)
    }
    duplicates.push(res);
    return res;
}
results.results[0].data.forEach(e => {
    db.casMapping.insert({ "casType": "${e.row[0]}", "pattern": "${e.row[1]}", "_id": "${slug(e.row[0]+" - "+e.row[1])}" })
});
'
echo $script >> migrateCas.js