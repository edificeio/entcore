package org.entcore.communication.services.impl;

import org.entcore.communication.services.CommunicationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultCommunicationServiceTest {
    DefaultCommunicationService service;

    @Before
    public void prepare() {
        service = new DefaultCommunicationService();
    }

    @Test
    public void computeDirectionToRemove_shouldReturnNull_WhenTheGroupHasBothIncomingAndOutgoingRelationship() {
        Assert.assertEquals(null,
                service.computeDirectionToRemove(true, true));
    }

    @Test
    public void computeDirectionToRemove_shouldReturnDirectionIncoming_WhenTheGroupHasIncomingRelationship() {
        Assert.assertEquals(DefaultCommunicationService.Direction.INCOMING,
                service.computeDirectionToRemove(true, false));
    }

    @Test
    public void computeDirectionToRemove_shouldReturnDirectionOutgoing_WhenTheGroupHasOutgoingRelationship() {
        Assert.assertEquals(DefaultCommunicationService.Direction.OUTGOING,
                service.computeDirectionToRemove(false, true));
    }

    @Test
    public void computeDirectionToRemove_shouldReturnBoth_WhenTheGroupHasNoRelationship() {
        Assert.assertEquals(DefaultCommunicationService.Direction.BOTH,
                service.computeDirectionToRemove(false, false));
    }

    @Test
    public void computeNextDirection_shouldReturnNull_GivenDirectionBoth() {
        Assert.assertEquals(null,
                service.computeNextDirection(DefaultCommunicationService.Direction.BOTH));
    }

    @Test
    public void computeNextDirection_shouldReturnIncoming_GivenDirectionOutgoing() {
        Assert.assertEquals(CommunicationService.Direction.INCOMING,
                service.computeNextDirection(DefaultCommunicationService.Direction.OUTGOING));
    }

    @Test
    public void computeNextDirection_shouldReturnOutgoing_GivenDirectionIncoming() {
        Assert.assertEquals(CommunicationService.Direction.OUTGOING,
                service.computeNextDirection(DefaultCommunicationService.Direction.INCOMING));
    }

    @Test
    public void computeNextDirection_shouldReturnBoth_GivenNull() {
        Assert.assertEquals(CommunicationService.Direction.BOTH,
                service.computeNextDirection(null));
    }
}
