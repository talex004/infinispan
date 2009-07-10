package org.infinispan.distribution;

import org.infinispan.commands.write.WriteCommand;
import org.infinispan.test.AbstractCacheTest;
import org.infinispan.test.ReplListener;

// For some reason I had to comment this out to ensure the test was disabled!

//@Test(groups = "functional", testName = "distribution.AsyncAPIAsyncDistTest", enabled = false)
public class AsyncAPIAsyncDistTest extends AsyncAPISyncDistTest {

   ReplListener rl;

   public AsyncAPIAsyncDistTest() {
      cleanup = AbstractCacheTest.CleanupPhase.AFTER_METHOD;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      super.createCacheManagers();
      rl = new ReplListener(c2, true);
   }

   @Override
   protected boolean sync() {
      return false;
   }

   @Override
   protected void resetListeners() {
      rl.resetEager();
   }

   @Override
   protected void asyncWait(boolean tx, Class<? extends WriteCommand>... cmds) {
      if (tx) {
         if (cmds == null || cmds.length == 0)
            rl.expectAnyWithTx();
         else
            rl.expectWithTx(cmds);
      } else {
         if (cmds == null || cmds.length == 0)
            rl.expectAny();
         else
            rl.expect(cmds);
      }


      rl.waitForRpc();
   }
}