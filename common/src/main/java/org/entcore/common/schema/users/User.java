package org.entcore.common.schema.users;

import org.entcore.common.utils.Id;
import org.entcore.common.utils.IdObject;
import org.entcore.common.utils.Identifier;
import org.entcore.common.utils.ExternalId;
import org.entcore.common.schema.structures.Structure;
import org.entcore.common.schema.utils.matchers.NodeMatcher;
import org.entcore.common.schema.utils.matchers.IdentifierMatcher;

import org.entcore.common.neo4j.TransactionHelper;

import io.vertx.core.Promise;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Collection;

public class User<P extends Profile> implements IdObject
{
    public final Id<User, String> id;
    public final ExternalId<User> externalId;

    private P profile;
    private Account account;

    public User(String id)
    {
        this(null, id, null);
    }

    public User(ExternalId<User> externalId)
    {
        this(null, null, externalId);
    }

    public User(P profile, String id, ExternalId<User> externalId)
    {
        this.id = new Id<User, String>(id);
        this.externalId = externalId;

        this.profile = profile;
    }

    public Future<JsonArray> attach(TransactionHelper tx, Identifier<Structure, ?>... structures)
    {
        return this.attach(tx, Arrays.asList(structures));
    }

    public Future<JsonArray> attach(TransactionHelper tx, Collection<Identifier<Structure, ?>> structures)
    {
        return Structure.attach(tx, new IdentifierMatcher<User>(this.id.get() == null ? this.externalId : this.id), new IdentifierMatcher<Structure>(structures));
    }

    public Future<JsonArray> dettach(TransactionHelper tx, Identifier<Structure, ?>... structures)
    {
        return this.dettach(tx, new IdentifierMatcher<Structure>(structures));
    }

    public Future<JsonArray> dettach(TransactionHelper tx, NodeMatcher<Structure> structuresMatcher)
    {
        return Structure.dettach(tx, new IdentifierMatcher<User>(this.id.get() == null ? this.externalId : this.id), structuresMatcher);
    }

    public void setAccount(Account a)
    {
        if(this.account != null)
            this.account.remove(this);

        if(a != null)
            a.add(this);

        this.account = a;
    }

    @Override
    public Id getId()
    {
        return this.id;
    }
}