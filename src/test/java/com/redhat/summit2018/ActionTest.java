package com.redhat.summit2018;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.google.gson.JsonObject;

/**
 * Unit test for simple Action.
 */
public class ActionTest 
    extends TestCase
{
/**
 * Create the test case
 *
 * @param testName name of the test case
 */
   public ActionTest(String testName)
   {
      super(testName);
   }

/**
 * @return the suite of tests being tested
 */
   public static Test suite()
   {
      return new TestSuite(ActionTest.class);
   }

/**
 * Rigourous Test :-)
 */
   public void testAction()
   {
      JsonObject input = new JsonObject();
      input.addProperty("param", "value");
      Action action = new Action();
      JsonObject output = action.main(input);
      assertTrue(output.has("param"));
      assertTrue(output.getAsJsonPrimitive("param").getAsString() == "value");
   } 
}
