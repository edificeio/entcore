package org.entcore.common.neo4j;

import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.BaseServer;

import java.net.InetAddress;
import java.util.*;

public class Neo4jRestClientCheckNotifier implements  Neo4jRestClientCheck{
    static final String NEO4J_CHANGE_EVENT = "NEO4J_CHANGE";
    private int nbCheck = 0;
    private final String hostName;
    private final EventStore eventStore;
    private final EmailSender emailSender;
    private final int emailAlertMinDown;
    private final String emailAlertSubject;
    private final Optional<HttpClient> slackClient;
    private final Optional<String> slackHook;
    private final List<Object> emailAlertDest = new ArrayList<>();
    private final Set<String> previousUnavailableUris = new HashSet<>();
    private Handler<Void> beforeSendMail;
    private Handler<AsyncResult<Message<JsonObject>>> onSendMail;
    private String moduleName;

    Neo4jRestClientCheckNotifier(final Vertx vertx, final JsonObject neo4jConfig){
        final EventStoreFactory eventFac = EventStoreFactory.getFactory();
        eventFac.setVertx(vertx);
        eventStore = eventFac.getEventStore(BaseServer.getModuleName());
        emailSender = new EmailFactory(vertx).getSenderWithPriority(EmailFactory.PRIORITY_VERY_HIGH);
        //mails
        this.emailAlertSubject = neo4jConfig.getString("email-alerts-subject", "[NEO4J] Noeuds down: ");
        this.emailAlertMinDown = neo4jConfig.getInteger("email-alerts-mindown", 2);
        final JsonArray mailAlerts = neo4jConfig.getJsonArray("email-alerts-dest",new JsonArray());
        for(final Object m : mailAlerts){
            if(m instanceof  String){
                this.emailAlertDest.add(m);
            }
        }
        //slack
        if(neo4jConfig.getBoolean("slack-enable", false)){
            slackHook = Optional.ofNullable(neo4jConfig.getString("slack-hook"));
            if(slackHook.isPresent()){
                slackClient = Optional.of(vertx.createHttpClient());
            }else{
                slackClient = Optional.empty();
            }
        }else{
            slackClient = Optional.empty();
            slackHook = Optional.empty();
        }
        String tempHostname;
        try {
            final InetAddress addr = InetAddress.getLocalHost();
            tempHostname = addr.getHostName();
        } catch (Exception e) {
            tempHostname = "";
        }
        this.hostName = tempHostname;
    }

    @Override
    public void start(Neo4jRestClientNodeManager manager) {
        nbCheck = 0;
    }

    @Override
    public Future<Void> check(Neo4jRestClientNodeManager manager) {
        return Future.succeededFuture();
    }

    @Override
    public void stop(Neo4jRestClientNodeManager manager) {
        if(slackClient.isPresent()){
            slackClient.get().close();
        }
    }

    @Override
    public void afterCheck(Neo4jRestClientNodeManager manager) {
        nbCheck ++;
        final List<Neo4jRestClientNode> becameAvailable = new ArrayList<>();
        final List<Neo4jRestClientNode> becameUnavailable = new ArrayList<>();
        for(final Neo4jRestClientNode node : manager.getClients()){
            boolean wasAvailable = !previousUnavailableUris.contains(node.getUrl());
            if(node.isAvailable()){
               if(!wasAvailable){
                   //emit event: is available now
                   becameAvailable.add(node);
               }
               previousUnavailableUris.remove(node.getUrl());
            }else{
                previousUnavailableUris.add(node.getUrl());
                if(wasAvailable){
                    //emit event: is unavailable now
                    becameUnavailable.add(node);
                }
            }
        }
        //
        alertByMail(manager, becameAvailable, becameUnavailable);
        alertBySlack(manager, becameAvailable, becameUnavailable);
        emitEvent(manager, becameAvailable, becameUnavailable);
    }

    private void alertByMail(Neo4jRestClientNodeManager manager, List<Neo4jRestClientNode> becameAvailable, List<Neo4jRestClientNode> becameUnavailable){
        if(emailAlertDest.isEmpty()){
            return;
        }
        if(becameUnavailable.size() > 0){
            final long count = manager.getClients().stream().filter(e->!e.isAvailable()).count();
            if(emailAlertMinDown <= count){
                final String subject = emailAlertSubject+" "+count +"/"+manager.getClients().size();
                final StringBuilder body = new StringBuilder();
                for(final Neo4jRestClientNode node : manager.getClients()){
                    final String now = becameUnavailable.contains(node)? " (just now)":"";
                    body.append(node.getUrl()+" => "+ (node.isAvailable()?"up": "down"+now)).append(" <br/>\n");
                }
                if(this.beforeSendMail != null){
                    this.beforeSendMail.handle(null);
                }
                emailSender.sendEmail(null, emailAlertDest, null, null, subject, body.toString(), null, false, onSendMail);
            }
        }
    }

    private void alertBySlack(Neo4jRestClientNodeManager manager, List<Neo4jRestClientNode> becameAvailable, List<Neo4jRestClientNode> becameUnavailable){
        if(slackClient.isPresent() && slackHook.isPresent() && !becameUnavailable.isEmpty()){
            final StringBuilder body = new StringBuilder("NEO4j down: ");
            for(final Neo4jRestClientNode node : becameUnavailable){
                body.append(node.getUrl()).append(" ");
            }
            final HttpClient client = slackClient.get();
            client.request(new RequestOptions()
                .setMethod(HttpMethod.POST)
                .setAbsoluteURI(slackHook.get()))
            .flatMap(req -> req.send(new JsonObject().put("text", body.toString()).toString()))
            .onSuccess(response -> {
                if (response.statusCode() != 200) {
                    log.error("NEO4J Slack notifier bad status: " + response.statusCode() + "/" + response.statusMessage());
                }
            })
            .onFailure(th -> {
                log.error("NEO4J Slack notifier failed: ", th);
            });
        }
    }

    private void emitEvent(Neo4jRestClientNodeManager manager, List<Neo4jRestClientNode> becameAvailable, List<Neo4jRestClientNode> becameUnavailable){
        final JsonObject event = new JsonObject().put("event_type", NEO4J_CHANGE_EVENT).put("hostname", this.hostName).put("module", this.moduleName);
        if(isFirstCheck()){
            for(final Neo4jRestClientNode node : manager.getClients()){
                eventStore.storeCustomEvent(NEO4J_CHANGE_EVENT, event.copy().put("node_type",node.getTypeName()).put("state",node.isAvailable()? "up":"down").put("url", node.getUrl()).put("starting", true));
            }
        }else{
            for(final Neo4jRestClientNode node : becameAvailable){
                eventStore.storeCustomEvent(NEO4J_CHANGE_EVENT, event.copy().put("node_type",node.getTypeName()).put("state", "up").put("url", node.getUrl()).put("starting", false));
            }
            for(final Neo4jRestClientNode node : becameUnavailable){
                eventStore.storeCustomEvent(NEO4J_CHANGE_EVENT, event.copy().put("node_type",node.getTypeName()).put("state", "down").put("url", node.getUrl()).put("starting", false));
            }
        }
    }

    private boolean isFirstCheck(){
        return nbCheck == 1;
    }

    public void setBeforeSendMail(Handler<Void> beforeSendMail) {
        this.beforeSendMail = beforeSendMail;
    }

    public void setOnSendMail(Handler<AsyncResult<Message<JsonObject>>> onSendMail) {
        this.onSendMail = onSendMail;
    }

    public Set<String> getPreviousUnavailableUris() {
        return previousUnavailableUris;
    }

    public Neo4jRestClientCheckNotifier setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }
}
