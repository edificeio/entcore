package org.entcore.communication.services.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.communication.services.CommunicationService;
import org.entcore.communication.services.CommunicationService.Direction;
import org.entcore.test.TestHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.util.HashMap;

@RunWith(VertxUnitRunner.class)
public class DefaultCommunicationServiceTest {
    protected static final TestHelper test = TestHelper.helper();
    DefaultCommunicationService service;
    private UserInfos userNoAdmin;

    private io.vertx.core.json.JsonObject mockGroupWithDirection( String id, String type, String subType, CommunicationService.Direction direction ) {
        return new io.vertx.core.json.JsonObject()
            .put("id", id).put("internalCommunicationRule", direction.name())
            .put("type", type).put("subType", subType).put("filter", (String) null);
    }
    private io.vertx.core.json.JsonObject mockManualGroupWithDirection( String id, CommunicationService.Direction direction ) {
        return mockGroupWithDirection(id, "ManualGroup", null, direction);
    }
    private io.vertx.core.json.JsonObject mockBroadcastGroupWithDirection( String id, CommunicationService.Direction direction ) {
        return mockGroupWithDirection(id, "ManualGroup", "BroadcastGroup", direction);
    }
    private UserInfos mockUserNoAdmin() {
        return this.userNoAdmin;
    }

    @Before
    public void prepare() {
        this.service = new DefaultCommunicationService(new TimelineHelper(Vertx.vertx(), Vertx.vertx().eventBus(), new JsonObject()), new JsonArray());
        this.userNoAdmin = test.directory().generateUser("notused");
        this.userNoAdmin.setFunctions(new HashMap<String, UserInfos.Function>(0));
    }

    @Test
    public void computeDirectionToRemove_shouldReturnNull_WhenTheGroupHasBothIncomingAndOutgoingRelationship() {
        Assert.assertEquals(null,
                this.service.computeDirectionToRemove(true, true));
    }

    @Test
    public void computeDirectionToRemove_shouldReturnDirectionIncoming_WhenTheGroupHasIncomingRelationship() {
        Assert.assertEquals(CommunicationService.Direction.INCOMING,
                this.service.computeDirectionToRemove(true, false));
    }

    @Test
    public void computeDirectionToRemove_shouldReturnDirectionOutgoing_WhenTheGroupHasOutgoingRelationship() {
        Assert.assertEquals(CommunicationService.Direction.OUTGOING,
                this.service.computeDirectionToRemove(false, true));
    }

    @Test
    public void computeDirectionToRemove_shouldReturnBoth_WhenTheGroupHasNoRelationship() {
        Assert.assertEquals(CommunicationService.Direction.BOTH,
                this.service.computeDirectionToRemove(false, false));
    }

    @Test
    public void computeNextDirection_shouldReturnNull_GivenDirectionBoth() {
        Assert.assertEquals(null,
                this.service.computeNextDirection(CommunicationService.Direction.BOTH));
    }

    @Test
    public void computeNextDirection_shouldReturnIncoming_GivenDirectionOutgoing() {
        Assert.assertEquals(CommunicationService.Direction.INCOMING,
                this.service.computeNextDirection(CommunicationService.Direction.OUTGOING));
    }

    @Test
    public void computeNextDirection_shouldReturnOutgoing_GivenDirectionIncoming() {
        Assert.assertEquals(CommunicationService.Direction.OUTGOING,
                this.service.computeNextDirection(CommunicationService.Direction.INCOMING));
    }

    @Test
    public void computeNextDirection_shouldReturnBoth_GivenNull() {
        Assert.assertEquals(CommunicationService.Direction.BOTH,
                this.service.computeNextDirection(null));
    }

    //-----------------------------------------------------------------------------------------------------
    private void computeWarningMessageForAddLinkCheckManualGroups(
        TestContext context, Direction fromDir, Direction toDir, String expected
        ) {
        final Async later = context.async();
        io.vertx.core.json.JsonObject from = mockManualGroupWithDirection( "1", fromDir );
        io.vertx.core.json.JsonObject to   = mockManualGroupWithDirection( "2", toDir );
        service.computeWarningMessageForAddLinkCheck( mockUserNoAdmin(), 
            from, service.computeDirectionForAddLinkCheck(from, true),
			to,   service.computeDirectionForAddLinkCheck(to, false))
        .onSuccess( msg -> { Assert.assertEquals(expected, msg); later.complete(); } )
        .onFailure( err -> { Assert.fail(err.getMessage()); context.fail(); } );
    }


    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderNoneAndReceiverNone(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.NONE, Direction.NONE, null );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderBothAndReceiverBoth(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.BOTH, Direction.BOTH, null );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderBothAndReceiverOutgoing(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.BOTH, Direction.OUTGOING, null );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnEndgroupUsersCanCommunicate_GivenSenderBothAndReceiverIncoming(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.BOTH, Direction.INCOMING, CommunicationService.WARNING_ENDGROUP_USERS_CAN_COMMUNICATE );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderBothAndReceiverNone(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.BOTH, Direction.NONE, null );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnStartgroupUsersCanCommunicate_GivenSenderOutgoingAndReceiverNone(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.OUTGOING, Direction.NONE, CommunicationService.WARNING_STARTGROUP_USERS_CAN_COMMUNICATE );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnStartgroupUsersCanCommunicate_GivenSenderOutgoingAndReceiverBoth(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.OUTGOING, Direction.BOTH, CommunicationService.WARNING_STARTGROUP_USERS_CAN_COMMUNICATE );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnStartgroupUsersCanCommunicate_GivenSenderOutgoingAndReceiverOutgoing(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.OUTGOING, Direction.OUTGOING, CommunicationService.WARNING_STARTGROUP_USERS_CAN_COMMUNICATE );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnBothgroupUsersCanCommunicate_GivenSenderOutgoingAndReceiverIncoming(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.OUTGOING, Direction.INCOMING, CommunicationService.WARNING_BOTH_GROUPS_USERS_CAN_COMMUNICATE );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderIncomingAndReceiverNone(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.INCOMING, Direction.NONE, null );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderIncomingAndReceiverBoth(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.INCOMING, Direction.BOTH, null );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderIncomingAndReceiverOutgoing(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.INCOMING, Direction.OUTGOING, null );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnEndgroupUsersCanCommunicate_GivenSenderIncomingAndReceiverIncoming(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.INCOMING, Direction.INCOMING, CommunicationService.WARNING_ENDGROUP_USERS_CAN_COMMUNICATE );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderNoneAndReceiverOutgoing(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.NONE, Direction.OUTGOING, null );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnEndgroupUsersCanCommunicate_GivenSenderNoneAndReceiverIncoming(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.NONE, Direction.INCOMING, CommunicationService.WARNING_ENDGROUP_USERS_CAN_COMMUNICATE );
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderNoneAndReceiverBoth(TestContext context) {
        computeWarningMessageForAddLinkCheckManualGroups( context, Direction.NONE, Direction.BOTH, null );
    }


    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderIncomingAndReceiverOutgoing_GivenSenderNoneAndReceiverNone() {
        io.vertx.core.json.JsonObject from = mockManualGroupWithDirection( "1", Direction.NONE );
        io.vertx.core.json.JsonObject to   = mockManualGroupWithDirection( "2", Direction.NONE );

        Assert.assertEquals(Direction.INCOMING, this.service.computeDirectionForAddLinkCheck(from, true));
        Assert.assertEquals(Direction.OUTGOING, this.service.computeDirectionForAddLinkCheck(to,   false));
    }

    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderIncomingAndReceiverBoth_GivenSenderNoneAndReceiverIncoming() {
        io.vertx.core.json.JsonObject from = mockManualGroupWithDirection( "1", Direction.NONE );
        io.vertx.core.json.JsonObject to   = mockManualGroupWithDirection( "2", Direction.INCOMING );

        Assert.assertEquals(Direction.INCOMING, this.service.computeDirectionForAddLinkCheck(from, true));
        Assert.assertEquals(Direction.BOTH,     this.service.computeDirectionForAddLinkCheck(to,   false));
    }

    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderBothAndReceiverOutgoing_GivenSenderOutgoingAndReceiverNone() {
        io.vertx.core.json.JsonObject from = mockManualGroupWithDirection( "1", Direction.OUTGOING );
        io.vertx.core.json.JsonObject to   = mockManualGroupWithDirection( "2", Direction.NONE );

        Assert.assertEquals(Direction.BOTH,     this.service.computeDirectionForAddLinkCheck(from, true));
        Assert.assertEquals(Direction.OUTGOING, this.service.computeDirectionForAddLinkCheck(to,   false));
    }

    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderBothAndReceiverBoth_GivenSenderOutgoingAndReceiverIncoming() {
        io.vertx.core.json.JsonObject from = mockManualGroupWithDirection( "1", Direction.OUTGOING );
        io.vertx.core.json.JsonObject to   = mockManualGroupWithDirection( "2", Direction.INCOMING );

        Assert.assertEquals(Direction.BOTH, this.service.computeDirectionForAddLinkCheck(from, true));
        Assert.assertEquals(Direction.BOTH, this.service.computeDirectionForAddLinkCheck(to,   false));
    }

    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderIncomingAndReceiverNone_GivenSenderNoneAndBroadcastReceiverNone() {
        io.vertx.core.json.JsonObject from = mockManualGroupWithDirection( "1", Direction.NONE );
        io.vertx.core.json.JsonObject to   = mockBroadcastGroupWithDirection( "2", Direction.NONE );

        Assert.assertEquals(Direction.INCOMING, this.service.computeDirectionForAddLinkCheck(from, true));
        Assert.assertEquals(Direction.NONE,     this.service.computeDirectionForAddLinkCheck(to,   false));
    }

    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderBothAndReceiverNone_GivenSenderOutgoingAndBroadcastReceiverNone() {
        io.vertx.core.json.JsonObject from = mockManualGroupWithDirection( "1", Direction.OUTGOING );
        io.vertx.core.json.JsonObject to   = mockBroadcastGroupWithDirection( "2", Direction.NONE );

        Assert.assertEquals(Direction.BOTH, this.service.computeDirectionForAddLinkCheck(from, true));
        Assert.assertEquals(Direction.NONE, this.service.computeDirectionForAddLinkCheck(to,   false));
    }
}
