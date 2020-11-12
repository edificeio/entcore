echo "var results = " > migrateCas.js
curl -H accept:application/json -H content-type:application/json -d '{"statements":[{"statement":"MATCH (a:Application) WHERE a.casType <> \"\" RETURN DISTINCT a.casType, a.pattern;"}]}' http://localhost:7474/db/data/transaction/commit  >> migrateCas.js 

script=';\n
var mapping = {"EliotRegisteredService":"Eliot","PronoteRegisteredService":"Pronote","KneRegisteredService":"KNE/CNS","LeSiteTvRegisteredService":"LeSite.tv","UniversalisRegisteredService":"Universalis","EnglishAttackRegisteredService":"English Attack","CIDJRegisteredService":"CIDJ","WebclasseursRegisteredService":"Webclasseur","ProEPSRegisteredService":"ProEPS","EducagriRegisteredService":"Educagri","EduMediaRegisteredService":"EduMedia","GepiRegisteredService":"Gepi","MSELRegisteredService":"MSEL","UidRegisteredService":"Uid","DefaultRegisteredService":"Defaut","GRRRegisteredService":"GRR","LabomepRegisteredService":"LaboMEP","AcademicSuffixRegisteredService":"OGIL/Césame","UuidRegisteredService":"Moodle","UaiRegisteredService":"UAI","PearltreesRegisteredService":"Pearltrees","ExplorateurDeMetiersRegisteredService":"Explorateur de Métiers","SalvumRegisteredService":"Salvum"};\n
var duplicates = [];\n
var slug = function (s) {\n
    var res = original = s.replace(/[^\w\s]/gi, " ").replace(/https?/, "").replace(/\s\s+/g, " ").trim().replace(/\s/g, "-");\n
    var counter = 0;\n
    while (duplicates.indexOf(res) > -1) {\n
        print("already exists : ", res);\n
        res = original + "-" + (counter++);\n
    };\n
    duplicates.push(res);\n
    return res;\n
};\n
results.results[0].data.forEach(e => {\n
    var nname = e.row[0]+" - "+e.row[1];\n
    for(var i in mapping){\n
        if(nname.indexOf(i) > -1){\n
            nname = nname.replace(i,mapping[i]);\n
        }\n
    }\n
    db.casMapping.insert({ "casType": e.row[0]+"", "pattern": e.row[1]+"", "_id": slug(nname)+"" });\n
});\n
'
echo -e $script >> migrateCas.js