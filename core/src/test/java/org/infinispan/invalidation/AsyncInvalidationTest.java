package org.infinispan.invalidation;

import org.testng.annotations.Test;

@Test(groups = "functional", testName = "invalidation.AsyncInvalidationTest")
public class AsyncInvalidationTest extends BaseInvalidationTest {
   public AsyncInvalidationTest() {
      isSync = false;
   }
}
