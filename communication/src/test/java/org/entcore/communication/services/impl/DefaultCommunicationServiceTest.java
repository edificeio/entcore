package org.entcore.communication.services.impl;

import org.entcore.communication.services.CommunicationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DefaultCommunicationServiceTest {
    DefaultCommunicationService service;

    @Before
    public void prepare() {
        this.service = new DefaultCommunicationService();
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

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderNoneAndReceiverNone() {
        Assert.assertEquals(null,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.NONE, CommunicationService.Direction.NONE));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderBothAndReceiverBoth() {
        Assert.assertEquals(null,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.BOTH, CommunicationService.Direction.BOTH));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderBothAndReceiverOutgoing() {
        Assert.assertEquals(null,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.BOTH, CommunicationService.Direction.OUTGOING));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnEndgroupUsersCanCommunicate_GivenSenderBothAndReceiverIncoming() {
        Assert.assertEquals(CommunicationService.WARNING_ENDGROUP_USERS_CAN_COMMUNICATE,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.BOTH, CommunicationService.Direction.INCOMING));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderBothAndReceiverNone() {
        Assert.assertEquals(null,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.BOTH, CommunicationService.Direction.NONE));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnStartgroupUsersCanCommunicate_GivenSenderOutgoingAndReceiverNone() {
        Assert.assertEquals(CommunicationService.WARNING_STARTGROUP_USERS_CAN_COMMUNICATE,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.OUTGOING, CommunicationService.Direction.NONE));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnStartgroupUsersCanCommunicate_GivenSenderOutgoingAndReceiverBoth() {
        Assert.assertEquals(CommunicationService.WARNING_STARTGROUP_USERS_CAN_COMMUNICATE,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.OUTGOING, CommunicationService.Direction.BOTH));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnStartgroupUsersCanCommunicate_GivenSenderOutgoingAndReceiverOutgoing() {
        Assert.assertEquals(CommunicationService.WARNING_STARTGROUP_USERS_CAN_COMMUNICATE,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.OUTGOING, CommunicationService.Direction.OUTGOING));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnBothgroupUsersCanCommunicate_GivenSenderOutgoingAndReceiverIncoming() {
        Assert.assertEquals(CommunicationService.WARNING_BOTH_GROUPS_USERS_CAN_COMMUNICATE,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.OUTGOING, CommunicationService.Direction.INCOMING));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderIncomingAndReceiverNone() {
        Assert.assertEquals(null,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.INCOMING, CommunicationService.Direction.NONE));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderIncomingAndReceiverBoth() {
        Assert.assertEquals(null,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.INCOMING, CommunicationService.Direction.BOTH));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderIncomingAndReceiverOutgoing() {
        Assert.assertEquals(null,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.INCOMING, CommunicationService.Direction.OUTGOING));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnEndgroupUsersCanCommunicate_GivenSenderIncomingAndReceiverIncoming() {
        Assert.assertEquals(CommunicationService.WARNING_ENDGROUP_USERS_CAN_COMMUNICATE,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.INCOMING, CommunicationService.Direction.INCOMING));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderNoneAndReceiverOutgoing() {
        Assert.assertEquals(null,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.NONE, CommunicationService.Direction.OUTGOING));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnEndgroupUsersCanCommunicate_GivenSenderNoneAndReceiverIncoming() {
        Assert.assertEquals(CommunicationService.WARNING_ENDGROUP_USERS_CAN_COMMUNICATE,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.NONE, CommunicationService.Direction.INCOMING));
    }

    @Test
    public void computeWarningMessageForAddLinkCheck_shouldReturnNull_GivenSenderNoneAndReceiverBoth() {
        Assert.assertEquals(null,
                this.service.computeWarningMessageForAddLinkCheck(CommunicationService.Direction.NONE, CommunicationService.Direction.BOTH));
    }

    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderIncomingAndReceiverOutgoing_GivenSenderNoneAndReceiverNone() {
        Map<String, CommunicationService.Direction> expectedMap = new HashMap<>();
        expectedMap.put("1", CommunicationService.Direction.INCOMING);
        expectedMap.put("2", CommunicationService.Direction.OUTGOING);

        Assert.assertEquals(expectedMap, this.service.computeNewDirectionAfterAddingLink("1"
                , CommunicationService.Direction.NONE
                , "2"
                , CommunicationService.Direction.NONE));
    }

    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderIncomingAndReceiverBoth_GivenSenderNoneAndReceiverIncoming() {
        Map<String, CommunicationService.Direction> expectedMap = new HashMap<>();
        expectedMap.put("1", CommunicationService.Direction.INCOMING);
        expectedMap.put("2", CommunicationService.Direction.BOTH);

        Assert.assertEquals(expectedMap, this.service.computeNewDirectionAfterAddingLink("1"
                , CommunicationService.Direction.NONE
                , "2"
                , CommunicationService.Direction.INCOMING));
    }

    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderBothAndReceiverOutgoing_GivenSenderOutgoingAndReceiverNone() {
        Map<String, CommunicationService.Direction> expectedMap = new HashMap<>();
        expectedMap.put("1", CommunicationService.Direction.BOTH);
        expectedMap.put("2", CommunicationService.Direction.OUTGOING);

        Assert.assertEquals(expectedMap, this.service.computeNewDirectionAfterAddingLink("1"
                , CommunicationService.Direction.OUTGOING
                , "2"
                , CommunicationService.Direction.NONE));
    }

    @Test
    public void computeNewDirectionAfterAddingLink_shouldReturnSenderBothAndReceiverBoth_GivenSenderOutgoingAndReceiverIncoming() {
        Map<String, CommunicationService.Direction> expectedMap = new HashMap<>();
        expectedMap.put("1", CommunicationService.Direction.BOTH);
        expectedMap.put("2", CommunicationService.Direction.BOTH);

        Assert.assertEquals(expectedMap, this.service.computeNewDirectionAfterAddingLink("1"
                , CommunicationService.Direction.OUTGOING
                , "2"
                , CommunicationService.Direction.INCOMING));
    }
}
