package org.entcore.common.schema.structures;

import org.entcore.common.utils.Id;
import org.entcore.common.utils.IdObject;
import org.entcore.common.utils.ExternalId;
import org.entcore.common.schema.Source;
import org.entcore.common.schema.users.User;
import org.entcore.common.schema.utils.matchers.Matcher;
import org.entcore.common.schema.utils.matchers.NodeMatcher;
import org.entcore.common.schema.utils.matchers.SourceMatcher;
import org.entcore.common.schema.utils.matchers.UniversalMatcher;
import org.entcore.common.schema.utils.matchers.CompoundMatcher;


import org.entcore.common.neo4j.TransactionHelper;

import io.vertx.core.Promise;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Structure implements IdObject
{
    public final Id<Structure, String> id;
    public final ExternalId<Structure>externalId;

    public Structure(String id)
    {
        this(id, null);
    }

    public Structure(ExternalId externalId)
    {
        this(null, externalId);
    }

    public Structure(String id, ExternalId<Structure> externalId)
    {
        this.id = new Id<Structure, String>(id);
        this.externalId = externalId;
    }

    public static Future<JsonArray> attach(TransactionHelper tx, NodeMatcher<User> usersMatcher, NodeMatcher<Structure> structuresMatcher)
    {
        Promise<JsonArray> promise = Promise.promise();
        JsonObject params = new JsonObject();

        usersMatcher.setNodeName("u");
        structuresMatcher.setNodeName("s");
        String inSource = "{source}";
        params.put("source", tx.source.toString());
        //TODO: Modify request to not attach to a removeFromStructure for other sources
        if(tx.source == Source.MANUAL)
            inSource = "CASE WHEN length([rs IN u.removedFromStructures WHERE rs = s.externalId]) > 0 THEN '" + Source.UNKNOWN + "' ELSE {source} END";

        String profileMatch = "p.name = HEAD(u.profiles)";

        String query =
            "MATCH (s:Structure)<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile), " +
            "(u:User)-[:MERGED*0..1]->(uu:User) " +
            "WHERE " + structuresMatcher + " AND " + usersMatcher + " AND NOT(HAS(uu.mergedWith)) AND " + profileMatch + " " +
            "WITH uu, g, s, " + inSource + " AS inSource " +
            "MERGE uu-[r:IN]->g " +
            "ON CREATE SET r.source = inSource ";

        if(tx.source == Source.MANUAL)
        {
            query +=
                "SET uu.structures = CASE WHEN s.externalId IN uu.structures THEN " +
                "uu.structures ELSE coalesce(uu.structures, []) + s.externalId END, " +
                "uu.removedFromStructures = [removedStruct IN uu.removedFromStructures WHERE removedStruct <> s.externalId] ";
        }

        query +=
            "WITH uu " +
            "OPTIONAL MATCH (uu)-[indpg:IN]-(:DefaultProfileGroup) " +
            "DELETE indpg ";

        usersMatcher.addParams(params);
        structuresMatcher.addParams(params);

        tx.add(query, params, promise);

        return promise.future();
    }

    public static Future<JsonArray> dettach(TransactionHelper tx, NodeMatcher<User> usersMatcher, NodeMatcher<Structure> structuresMatcher)
    {
        Promise<JsonArray> promise = Promise.promise();

        structuresMatcher.setNodeName("s");
        usersMatcher.setNodeName("u");
        Matcher sourceMatcher = tx.source == Source.MANUAL ? new UniversalMatcher() : new SourceMatcher("attached", Source.UNKNOWN, tx.source);
        CompoundMatcher fullMatcher = new CompoundMatcher(usersMatcher, structuresMatcher, sourceMatcher);

        String query =
            "MATCH (u:User)-[attached:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
            "WHERE " + fullMatcher + " " +  // Match users that are attached to this structure (but exclude people who are only in the manual groups)
            "WITH u, s " +
            "MATCH (u)-[r:IN]-(g:Group)-[:DEPENDS*1..2]-(s) " +
            "WHERE NOT(g:ManualGroup) ";

        JsonObject params = new JsonObject();
        fullMatcher.addParams(params);
        sourceMatcher.addParams(params);

        if(tx.source == Source.MANUAL)
        {
            SourceMatcher notManual = new SourceMatcher(Matcher.Operation.EXCLUDE, "r", Source.MANUAL);
            query +=
                "WITH u, r, g, s, MAX(CASE WHEN " + notManual + " THEN s.externalId ELSE null END) AS sID " +
			    "SET u.removedFromStructures = [rsId IN coalesce(u.removedFromStructures, []) WHERE rsId <> coalesce(sID, '')] + coalesce(sID, []), " +
                "u.structures = FILTER(sId IN u.structures WHERE sId <> s.externalId), " +
				"u.classes = FILTER(cId IN u.classes WHERE NOT(cId =~ (s.externalId + '.*'))) " +
                "WITH u, r, g, s ";
            notManual.addParams(params);
        }

        query +=
            "OPTIONAL MATCH (u)-[c:COMMUNIQUE]-(g) " +
            "DELETE r, c " + // Remove user from groups
            "WITH u, s " +
            "MATCH (u)-[r:HAS_FUNCTION]->() " +
            "WHERE s.id IN r.scope " +
            "OPTIONAL MATCH (s)-[:HAS_ATTACHMENT*1..]->(ss:Structure) " +
            "WITH s, r, count(CASE WHEN ss.id IN r.scope THEN 1 ELSE NULL END) as parentADML " +
            "WHERE parentADML = 0 " +
            "SET r.scope = FILTER(sId IN r.scope WHERE sId <> s.id) " + // Remove user function for the structure but keep inherited functions
            "WITH r " +
            "WHERE LENGTH(r.scope) = 0 " +
            "DELETE r"; // Remove the function if no structures are left

        tx.add(query, params, promise);

        String dpgQuery =
            "MATCH (u:User) WHERE " + usersMatcher + " " +
            "OPTIONAL MATCH (u)-[:IN]->(pg:ProfileGroup) " +
            "WITH u, COUNT(pg) AS nbProfileGroups " +
            "WHERE nbProfileGroups = 0 " +
            "MATCH (dpg:DefaultProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
            "WHERE p.name = HEAD(u.profiles) " +
            "MERGE (u)-[:IN]->(dpg)"; // Add users with no structures left to the default groups

        tx.add(dpgQuery, params);

        return promise.future();
    }

    @Override
    public Id getId()
    {
        return this.id;
    }
}
