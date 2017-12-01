/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.service.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;
import org.entcore.common.service.SearchService;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.sql.Sql;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.sql.SqlResult.validResultHandler;

/**
 * Created by dbreyton on 23/02/2016.
 */
public final class SqlSearchService implements SearchService {
    private final String resourceTable;
    private final String shareTable;
    private final String displayNameField;
    private final String userTable;
    private final Sql sql;
    private final String schema;
    private final String table;
    private final Boolean checkVisibility;
    private final List<String> searchFields;

    public SqlSearchService(String schema, String table, String shareTable, List<String> searchFields) {
        this(schema, table, shareTable, searchFields, null, null, null);
    }

    public SqlSearchService(String schema, String table, String shareTable, List<String> searchFields, String userTable, String displayNameField,
                            Boolean checkVisibility) {
        this.table = table;
        this.sql = Sql.getInstance();
        this.searchFields = searchFields;

        if (schema != null && !schema.trim().isEmpty()) {
            this.resourceTable = schema + "." + table;
            this.schema = schema + ".";
            this.shareTable = this.schema+((shareTable != null && !shareTable.trim().isEmpty()) ? shareTable : "shares");
            this.userTable = this.schema+((userTable != null && !userTable.trim().isEmpty()) ? userTable : "users");
        } else {
            this.schema = "";
            this.resourceTable = table;
            this.shareTable = (shareTable != null && !shareTable.trim().isEmpty()) ? shareTable : "shares";
            this.userTable = (userTable != null && !userTable.trim().isEmpty()) ? userTable : "users";
        }

        this.displayNameField = (displayNameField != null && !displayNameField.isEmpty()) ?
                this.userTable + "." + displayNameField : this.userTable + "." + "username";

        if (checkVisibility != null) {
            this.checkVisibility = checkVisibility;
        } else {
            this.checkVisibility = false;
        }
    }

    @Override
    public void search(String userId, List<String> groupIds, List<String> returnFields, List<String> searchWords,
                        Integer page, Integer limit, Handler<Either<String, JsonArray>> handler) {
        final int offset = page * limit;

        final String fields = displayNameField + ", " + resourceTable + "." + Joiner.on(", " + resourceTable + ".").join(returnFields);
        final List<String> gu = new ArrayList<String>();
        gu.add(userId);
        if (groupIds != null) {
            gu.addAll(groupIds);
        }
        final Object[] groupsAndUserIds = gu.toArray();

        final String rightsWhere = "member_id IN " + Sql.listPrepared(groupsAndUserIds) +
        " OR owner = ?" + (checkVisibility ? " OR visibility IN (?,?)" : "");

        final String query = "SELECT " + fields + " FROM " + resourceTable +
                " LEFT JOIN " + shareTable + " ON " + resourceTable + ".id = resource_id" +
                " LEFT JOIN " + userTable + " ON " + resourceTable + ".owner = "+ userTable + ".id" +
                " LEFT JOIN " + schema + "members ON (member_id = " + schema + "members.id AND group_id IS NOT NULL) " +
                "WHERE (" + rightsWhere + ")  AND (" + searchWherePrepared() + ")" +
                " GROUP BY " + resourceTable + ".id, " + displayNameField +
                " ORDER BY modified DESC " +
                " LIMIT ? OFFSET ?";
        final JsonArray values = new JsonArray(gu).add(userId);
        if (checkVisibility) {
            values.add(VisibilityFilter.PROTECTED.name()).add(VisibilityFilter.PUBLIC.name());
        }

        final String textSearchedValue = textSearchedComposition(searchWords);
        for (int i=0;i<this.searchFields.size();i++) {
            values.add(textSearchedValue);
        }

        values.add(limit).add(offset);
        sql.prepared(query, values, validResultHandler(handler));
    }

    private String searchWherePrepared() {
        StringBuilder sb = new StringBuilder();
        if (this.searchFields != null && this.searchFields.size() > 0) {
            for (String s : this.searchFields) {
                sb.append(s).append(" @@ to_tsquery(unaccent(?)) AND ");
            }
            sb.delete(sb.length() - 4, sb.length());
        }
        return sb.toString();
    }

    public static String textSearchedComposition(List<String> wordsLst) {
        final StringBuilder words = new StringBuilder();
        for (String word : wordsLst) {
           //conjunction
           words.append(word).append(" & ");
        }
        words.delete(words.length() - 2, words.length());
        return words.toString();
    }
}