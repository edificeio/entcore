/*
 * Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.auth.users;

import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;

import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.pgclient.PgRowSet;
import io.reactiverse.pgclient.Row;
import io.reactiverse.pgclient.Tuple;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.util.ElementScanner6;

import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.http.request.JsonHttpServerRequest;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class NewDeviceWarningTask implements Handler<Long>
{
    private static final String LOGIN_EVENTS_TABLE = "events.login_events";
    private static final String DEVICES_INFO_TABLE = "utils.ua_device";
    private static final String DEVICE_CHECK_TABLE = "apps_auth.login_device_change_check";

    private static final String LOGIN_ID_FIELD      = "id";
    private static final String PLATFORM_ID_FIELD   = "platform_id";
    private static final String USER_ID_FIELD       = "user_id";
    private static final String USER_PROFILE_FIELD  = "profile";
    private static final String USER_ADMIN_FIELD    = "useradmin";
    private static final String USER_AGENT_FIELD    = "ua";
    private static final String IP_FIELD            = "ip";
    private static final String DATE_FIELD          = "date";
    private static final String SCORE_FIELD         = "score";
    private static final String NOTIFICATION_FIELD  = "notification_sent";

    private static final String DEVICE_TYPE_FIELD       = "device";
    private static final String DEVICE_DEVICE_BRAND     = "device_brand";
    private static final String DEVICE_DEVICE_MODEL     = "device_model";
    private static final String DEVICE_OS_NAME          = "os_name";
    private static final String DEVICE_OS_FAMILY        = "os_family";
    private static final String DEVICE_OS_PLATFORM      = "os_platform";
    private static final String DEVICE_OS_VERSION       = "os_version";
    private static final String DEVICE_CLIENT_TYPE      = "client_type";
    private static final String DEVICE_CLIENT_NAME      = "client_name";
    private static final String DEVICE_CLIENT_VERSION   = "client_version";

    private static final String ADMC_STRING = "SuperAdmin";
    private static final String ADML_STRING = "AdminLocal";

    private static final int SCORE_LOW = 1;
    private static final int SCORE_MID = 2;
    private static final int SCORE_HIGH = 3;
    private static final int SCORE_MAX = 100;

    private static final AtomicBoolean locked = new AtomicBoolean(false);

    private EmailSender sender;
    private String mailFrom;
    private PgClient pgClient = null;
    private String platformId;
    private String adminFilter;
    private int scoreThreshold;
    private int batchLimit;

    private static final Logger log = LoggerFactory.getLogger(NewDeviceWarningTask.class);

	public NewDeviceWarningTask(Vertx vertx, EmailSender sender, String mailFrom, boolean includeADMC, boolean includeADML, boolean includeUsers, int scoreThreshold, int batchLimit)
    {
		final String eventStoreConf = (String) vertx.sharedData().getLocalMap("server").get("event-store");
		if (eventStoreConf != null)
        {
			final JsonObject eventStoreConfig = new JsonObject(eventStoreConf);
			this.platformId = eventStoreConfig.getString("platform");
			final JsonObject eventStorePGConfig = eventStoreConfig.getJsonObject("postgresql");
			if (eventStorePGConfig != null)
            {
				final PgPoolOptions options = new PgPoolOptions()
					.setPort(eventStorePGConfig.getInteger("port", 5432))
					.setHost(eventStorePGConfig.getString("host"))
					.setDatabase(eventStorePGConfig.getString("database"))
					.setUser(eventStorePGConfig.getString("user"))
					.setPassword(eventStorePGConfig.getString("password"))
					.setMaxSize(eventStorePGConfig.getInteger("pool-size", 5));
				this.pgClient = PgClient.pool(vertx, options);
			}
		}

        this.sender = sender;
        this.mailFrom = mailFrom != null ? mailFrom : "noreply@one1d.fr";
        this.scoreThreshold = scoreThreshold;
        this.batchLimit = batchLimit;

        if(includeUsers == true)
            this.adminFilter = "1 = 1";
        else if(includeADML == true)
        {
            if(includeADMC == true)
                this.adminFilter = "e." + USER_ADMIN_FIELD + " IS NOT NULL";
            else
                this.adminFilter = "e." + USER_ADMIN_FIELD + " = '" + ADML_STRING + "'";
        }
        else if(includeADMC == true)
            this.adminFilter = "e." + USER_ADMIN_FIELD + " = '" + ADMC_STRING + "'";
        else
            this.adminFilter = "1 = 0";
	}

	@Override
	public void handle(Long event)
    {
        if(this.pgClient == null && locked.compareAndSet(false, true) == false)
            return;

        String getNewLoginEvents = "SELECT e.*, e." + IP_FIELD + "::varchar(64), d.*" +
                                    " FROM " + LOGIN_EVENTS_TABLE + " e" +
                                    " INNER JOIN " + DEVICES_INFO_TABLE + " d USING (" + USER_AGENT_FIELD + ")" +
                                    " LEFT JOIN " + DEVICE_CHECK_TABLE + " c USING (" + PLATFORM_ID_FIELD + "," + USER_ID_FIELD + "," + USER_AGENT_FIELD + "," + IP_FIELD + ")" +
                                    " WHERE e." + PLATFORM_ID_FIELD + " = $1 AND " + this.adminFilter +
                                    " AND c." + LOGIN_ID_FIELD + " IS NULL" +
                                    " LIMIT $2";
        this.pgClient.preparedQuery(getNewLoginEvents, Tuple.of(this.platformId, this.batchLimit), new Handler<AsyncResult<PgRowSet>>()
        {
            @Override
            public void handle(AsyncResult<PgRowSet> pgRes)
            {
                if(pgRes.succeeded() == false)
                {
                    log.error("Failed to get new login events " + pgRes.cause());
                    locked.set(false);
                }
                else
                {
                    JsonArray rows = pgRowSetToJsonArray(pgRes.result());
                    if(rows.size() > 0)
                        loadUsers(rows);
                }
            }
        });
	}

    private void loadUsers(JsonArray events)
    {
        Map<String, LoginEventUser> users = new HashMap<String, LoginEventUser>();
        for(int i = events.size(); i-- > 0;)
        {
            JsonObject row = events.getJsonObject(i);
            String userId = row.getString(USER_ID_FIELD);

            LoginEventUser user = users.get(userId);
            if(user == null)
            {
                user = new LoginEventUser(userId, row.getString(USER_PROFILE_FIELD), row.getString(USER_ADMIN_FIELD));
                users.put(userId, user);
            }

            Device device = new Device(
                row.getString(DEVICE_TYPE_FIELD),
                row.getString(DEVICE_DEVICE_BRAND),
                row.getString(DEVICE_DEVICE_MODEL),
                row.getString(DEVICE_OS_NAME),
                row.getString(DEVICE_OS_FAMILY),
                row.getString(DEVICE_OS_PLATFORM),
                row.getString(DEVICE_OS_VERSION),
                row.getString(DEVICE_CLIENT_TYPE),
                row.getString(DEVICE_CLIENT_NAME),
                row.getString(DEVICE_CLIENT_VERSION));
            Connection conn = new Connection(row.getString(LOGIN_ID_FIELD), row.getString(DATE_FIELD), row.getString(USER_AGENT_FIELD), row.getString(IP_FIELD), device);
            user.addNewConnection(conn);
        }

        String getUsersEmail = "MATCH (u:User) WHERE u.id IN {users} RETURN u.id AS id, u.email AS email, u.displayName AS displayName";
		Neo4j.getInstance().execute(getUsersEmail, new JsonObject().put("users", new JsonArray(new ArrayList(users.keySet()))), new Handler<Message<JsonObject>>()
        {
			@Override
			public void handle(Message<JsonObject> r)
            {
                if("ok".equals(r.body().getString("status")))
                {
                    JsonArray res = r.body().getJsonArray("result");
                    for(int i = res.size(); i-- > 0;)
                    {
                        JsonObject neoUser = res.getJsonObject(i);
                        users.get(neoUser.getString("id")).setInfos(neoUser.getString("displayName"), neoUser.getString("email"));
                    }
                    scoreConnections(users);
                }
                else
                {
                    log.error("Failed to get user mails " + r.body().getString("message"));
                    locked.set(false);
                }
			}
		});
    }

    private void scoreConnections(Map<String, LoginEventUser> users)
    {
        String getKnownConnections = "SELECT *, e." + IP_FIELD + "::varchar(64)" +
                                        " FROM " + DEVICE_CHECK_TABLE + " e" +
                                        " INNER JOIN " + DEVICES_INFO_TABLE + " d USING (" + USER_AGENT_FIELD + ")" +
                                        " WHERE e." + PLATFORM_ID_FIELD + " = $1 AND " + USER_ID_FIELD + " = ANY($2)";
        Tuple userIdsTuple = Tuple.of(platformId);
        userIdsTuple.addStringArray(users.keySet().toArray(new String[users.keySet().size()]));
        this.pgClient.preparedQuery(getKnownConnections, userIdsTuple, new Handler<AsyncResult<PgRowSet>>()
        {
            @Override
            public void handle(AsyncResult<PgRowSet> pgRes)
            {
                if(pgRes.succeeded() == false)
                {
                    log.error("Failed to get old connections " + pgRes.cause());
                    locked.set(false);
                }
                else
                {
                    JsonArray rows = pgRowSetToJsonArray(pgRes.result());

                    for(int i = rows.size(); i-- > 0;)
                    {
                        JsonObject row = rows.getJsonObject(i);
                        LoginEventUser user = users.get(row.getString(USER_ID_FIELD));

                        Device device = new Device(
                            row.getString(DEVICE_TYPE_FIELD),
                            row.getString(DEVICE_DEVICE_BRAND),
                            row.getString(DEVICE_DEVICE_MODEL),
                            row.getString(DEVICE_OS_NAME),
                            row.getString(DEVICE_OS_FAMILY),
                            row.getString(DEVICE_OS_PLATFORM),
                            row.getString(DEVICE_OS_VERSION),
                            row.getString(DEVICE_CLIENT_TYPE),
                            row.getString(DEVICE_CLIENT_NAME),
                            row.getString(DEVICE_CLIENT_VERSION));
                        Connection conn = new Connection(row.getString(LOGIN_ID_FIELD), row.getString(DATE_FIELD), row.getString(USER_AGENT_FIELD), row.getString(IP_FIELD), device);
                        user.addKnownConnection(conn);
                    }

                    String insertNewConnections = "INSERT INTO " + DEVICE_CHECK_TABLE +
                                                    " (" + LOGIN_ID_FIELD + "," + DATE_FIELD + "," + USER_AGENT_FIELD + "," + IP_FIELD +
                                                    " ," + USER_ID_FIELD + "," + USER_PROFILE_FIELD + "," + USER_ADMIN_FIELD +
                                                    " ," + PLATFORM_ID_FIELD + "," + SCORE_FIELD + "," + NOTIFICATION_FIELD + ")" +
                                                    " VALUES ($1,$2,$3,CAST(lower($4) AS INET),$5,$6,$7,$8,$9,$10)";

                    List<Tuple> insertTuples = new ArrayList<Tuple>();
                    for(LoginEventUser user : users.values())
                    {
                        user.scoreNewConnections();
                        for(Connection c : user.newConnections)
                        {
                            UUID id = UUID.fromString(c.id);
                            LocalDateTime date = LocalDateTime.parse(c.date);
                            boolean sendEmail = user.email != null && c.score >= scoreThreshold;

                            insertTuples.add(Tuple.of(id, date, c.userAgent, c.ip, user.id, user.profile, user.admin, platformId, c.score, sendEmail));

                            if(sendEmail == true)
                                sendEmail(user, c);
                        }
                    }

                    pgClient.preparedBatch(insertNewConnections, insertTuples, new Handler<AsyncResult<PgRowSet>>()
                    {
                        @Override
                        public void handle(AsyncResult<PgRowSet> pgRes)
                        {
                            if(pgRes.succeeded() == false)
                            {
                                log.error("Failed to insert new connections " + pgRes.cause());
                                locked.set(false);
                            }
                            else
                                locked.set(false);
                        }
                    });
                }
            }
        });
    }

    private void sendEmail(LoginEventUser user, Connection c)
    {
        JsonObject params = new JsonObject()
            .put("displayName", user.displayName)
            .put("device", c.device.toString())
            .put("date", LocalDateTime.parse(c.date).toEpochSecond(ZoneOffset.UTC) * 1000)
            .put("ip", c.ip);
		sender.sendEmail(
            new JsonHttpServerRequest(new JsonObject()),
            user.email,
            this.mailFrom,
            null,
            null,
            "email.new.device.subject",
            "email/newDeviceWarning.html",
            params,
            true,
            handlerToAsyncHandler(new Handler<Message<JsonObject>>()
            {
                public void handle(Message<JsonObject> event)
                {
                    if("ok".equals(event.body().getString("status")) == false)
                        log.error("Failed to send email to user " + user.id + " : " + event.body().getString("message"));
                }
            }));
    }

    public void clearUsersDevices(String[] userIds)
    {
        String removeKnownDevices = "DELETE FROM " + DEVICE_CHECK_TABLE + " WHERE " + PLATFORM_ID_FIELD + " = $1 AND "+ USER_ID_FIELD + " = ANY($2)";

        Tuple removeUsersTuple = Tuple.of(this.platformId);
        removeUsersTuple.addStringArray(userIds);

        this.pgClient.preparedQuery(removeKnownDevices, removeUsersTuple, new Handler<AsyncResult<PgRowSet>>()
        {
            @Override
            public void handle(AsyncResult<PgRowSet> pgRes)
            {
                if(pgRes.succeeded() == false)
                    log.error("Failed to remove known devices for users " + userIds);
            }
        });
    }

    private JsonArray pgRowSetToJsonArray(PgRowSet rows)
    {
        final List<String> columns = rows.columnsNames();
        final JsonArray res = new JsonArray();

        for (Row row: rows)
        {
            final JsonObject j = new JsonObject();
            for (int i = 0; i < columns.size(); i++)
            {
                final Object val = row.getValue(i);

                if(val == null)
                    j.putNull(columns.get(i));
                else if (val instanceof LocalDateTime)
                    j.put(columns.get(i), val.toString());
                else if(val instanceof UUID)
                    j.put(columns.get(i), val.toString());
                else
                    j.put(columns.get(i), val);
            }
            res.add(j);
        }

        return res;
    }

    private class Device
    {
        String deviceType;
        String deviceBrand;
        String deviceModel;
        String osName;
        String osFammily;
        String osPlatform;
        String osVersion;
        String clientType;
        String clientName;
        String clientVersion;

        public Device(String deviceType, String deviceBrand, String deviceModel, String osName, String osFammily, String osPlatform, String osVersion, String clientType, String clientName, String clientVersion)
        {
            this.deviceType = deviceType;
            this.deviceBrand = deviceBrand;
            this.deviceModel = deviceModel;
            this.osName = osName;
            this.osFammily = osFammily;
            this.osPlatform = osPlatform;
            this.osVersion = osVersion;
            this.clientType = clientType;
            this.clientName = clientName;
            this.clientVersion = clientVersion;
        }

        @Override
        public String toString()
        {
            String deviceStr = this.deviceBrand != null ? (this.deviceBrand + (this.deviceModel != null ? " " + this.deviceModel : "")) : "Appareil inconnu";
            String osStr = this.osName != null ? ("(" + this.osName + " " + this.osVersion + ")") : "OS inconnu";
            String clientStr = this.clientName != null ? (this.clientName + (this.clientVersion != null ? " " + this.clientVersion : "") + " (" + this.clientType + ")") : "Client inconnu";

            return (deviceStr + " " + osStr + " " + clientStr).replaceAll("null", "");
        }

        private boolean _cmp(String a, String b)
        {
            return a == null ? b == null : a.equals(b);
        }

        private int compare(Device o)
        {
            if(o == null)
                return -1;

            int score = 0;
            score += (this._cmp(this.deviceBrand, o.deviceBrand)) ? 0 : SCORE_HIGH;
            score += (this._cmp(this.deviceModel, o.deviceModel)) ? 0 : SCORE_HIGH;
            score += (this._cmp(this.osName, o.osName)) ? 0 : SCORE_HIGH;
            score += (this._cmp(this.osFammily, o.osFammily)) ? 0 : SCORE_HIGH;
            score += (this._cmp(this.osPlatform, o.osPlatform)) ? 0 : SCORE_HIGH;
            score += (this._cmp(this.osVersion, o.osVersion)) ? 0 : SCORE_HIGH;
            score += (this._cmp(this.clientType, o.clientType)) ? 0 : SCORE_HIGH;
            score += (this._cmp(this.clientName, o.clientName)) ? 0 : SCORE_HIGH;

            score += (this._cmp(this.clientVersion, o.clientVersion)) ? 0 : SCORE_LOW;

            return score;
        }

        private int _hash(String a)
        {
            return a == null ? 1 : a.hashCode();
        }

        @Override
        public int hashCode()
        {
            return
                this._hash(deviceType) *
                this._hash(deviceBrand) *
                this._hash(deviceModel) *
                this._hash(osName) *
                this._hash(osFammily) *
                this._hash(osPlatform) *
                this._hash(osVersion) *
                this._hash(clientType) *
                this._hash(clientName) *
                this._hash(clientVersion);
        }

        @Override
        public boolean equals(Object o)
        {
            return o != null && o instanceof Device && this.compare((Device)o) == 0;
        }
    }

    private class Connection
    {

        String id;
        String date;
        String userAgent;
        String ip;

        Device device;

        int score = 0;

        public Connection(String id, String date, String userAgent, String ip, Device device)
        {
            this.id = id;
            this.date = date;
            this.userAgent = userAgent;
            this.ip = ip;
            this.device = device;
        }

        public void checkSuspicious()
        {
            if(this.userAgent == null || this.userAgent.isEmpty())
                this.score += SCORE_MAX;
        }

        public int compare(Connection c)
        {
            int score = this.device.compare(c.device);

            score += this.ip == null ? c.ip == null ? 0 : SCORE_MID : this.ip.equals(c.ip) ? 0 : SCORE_MID;

            return score;
        }

        @Override
        public boolean equals(Object o)
        {
            if(o instanceof Connection)
            {
                boolean sameUA = this.userAgent == null ? ((Connection)o).userAgent == null : this.userAgent.equals(((Connection)o).userAgent);
                if(sameUA)
                    return this.ip == null ? ((Connection)o).ip == null : this.ip.equals(((Connection)o).ip);
                else
                    return false;
            }
            else
                return false;
        }

        @Override
        public int hashCode()
        {
            return this.userAgent == null ? 0 : this.ip == null ? 1 : this.userAgent.hashCode() * this.ip.hashCode();
        }
    }

    private class LoginEventUser
    {
        String id;
        String profile;
        String admin;
        String displayName;
        String email;

        Set<Connection> knownConnections = new LinkedHashSet<Connection>();
        Set<Connection> newConnections = new HashSet<Connection>();

        public LoginEventUser(String id, String profile, String admin)
        {
            this.id = id;
            this.profile = profile;
            this.admin = admin;
        }

        public void setInfos(String displayName, String email)
        {
            this.displayName = displayName;
            this.email = email;
        }

        public void addKnownConnection(Connection c)
        {
            this.knownConnections.add(c);
        }

        public void addNewConnection(Connection c)
        {
            this.newConnections.add(c);
        }

        public void scoreNewConnections()
        {
            for(Connection c : this.newConnections)
            {
                if(this.knownConnections.size() > 0)
                {
                    c.checkSuspicious();
                    if(c.score == 0)
                    {
                        c.score = Integer.MAX_VALUE;
                        if(this.knownConnections.contains(c) == false)
                        {
                            for(Connection old : this.knownConnections)
                            {
                                int compareScore = c.compare(old);
                                if(compareScore < c.score)
                                    c.score = compareScore;
                            }
                        }
                    }
                }
            }
        }
    }

}
