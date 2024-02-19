package org.entcore.audience.reaction.service.impl;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.audience.reaction.dao.impl.ReactionDaoImpl;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.common.sql.Sql;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

@RunWith(VertxUnitRunner.class)
public class ReactionServiceImplTest {

  private static ReactionService reactionService;
  private static final TestHelper test = TestHelper.helper();


  @BeforeClass
  public static void setUp(TestContext context) throws Exception {
    final ReactionDaoImpl reactionDao = new ReactionDaoImpl(null);
    reactionService = new ReactionServiceImpl(reactionDao);
  }

  @Test
  public void testGetReactionsSummaryWhenModuleDoesNotExist(final TestContext context) {
    //reactionService.getReactionDetails()
  }
}
