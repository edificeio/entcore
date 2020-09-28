echo "var results = " > migrateCas.js
curl -H accept:application/json -H content-type:application/json -d '{"statements":[{"statement":"MATCH (a:Application) WHERE a.casType <> \"\" RETURN DISTINCT a.casType, a.pattern;"}]}' http://localhost:7474/db/data/transaction/commit  >> migrateCas.js 

script=';\n
var duplicates = [];\n
var slug = function (s) {\n
    var res = original = s.replace(/[^\w\s]/gi, " ").replace(/https?/, "").replace(/\s\s+/g, " ").trim().replace(/\s/g, "-");\n
    var counter = 0;\n
    while (duplicates.indexOf(res) > -1) {\n
        console.log("already exists : ", res);\n
        res = original + "-" + (counter++);\n
    };\n
    duplicates.push(res);\n
    return res;\n
};\n
results.results[0].data.forEach(e => {\n
    db.casMapping.insert({ "casType": e.row[0]+"", "pattern": e.row[1]+"", "_id": slug(e.row[0]+" - "+e.row[1])+"" });\n
});\n
'
echo -e $script >> migrateCas.js