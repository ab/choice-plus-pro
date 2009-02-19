
package util;

import java.io.*;
import java.math.*;
import java.text.*;
import java.util.*;
import engine.Engine;
import engine.Rules;
import util.*;

/**
 * Code to test
 */
public class Tester
{
   private boolean invokedStandalone = false;
   private static final int SUCCESS = 0;

   static Tester tester = null;

   public static void main(String[] args)
      throws FileNotFoundException
   {
      tester = new Tester();
      tester.invokedStandalone = true;
      ///tester.testPRMaster();
      tester.runJavaTest();
   }

   private static void runJavaTest()
   {
///      try {
         //tester.testFileOutput();      //Test PrintWriter, io streams
         //tester.testBCD();
         tester.testDateFormat();
///      }
/*      catch (Exception e) {
         // do nuttin' here...
         System.out.println(e.getMessage());
         Thread.dumpStack();
         int x = 1; // a place to put a breakpoint...
      }
*/
   }

   private void testBCD()
   {
      BCD x = new BCD("0.123456789");
      System.out.println("Testing BCD -- start");
      for (int i=0; i < 7; i++)
         System.out.println( x.toString(i) );
      System.out.println("Testing BCD -- done");
   }

   public Tester()
   {
   }

   public void testPRMaster()
      throws FileNotFoundException
   {
      int successCode = SUCCESS;

      System.out.println("\n\n***Starting Tester***");

      // Test LiteralString.java
      System.out.println("Testing LiteralString -- Start");
      ///LiteralString.load();
      String s = LiteralString.get(StringIds.ELIMINATED);
      if (s == null || !s.equals("eliminated"))
         System.out.println("\tFAILURE 1");
      int stringId = LiteralString.get(".THRESHOLD");
      if (stringId != StringIds.COM_THRESHOLD)
         System.out.println("\tFAILURE 1-b");
      s = LiteralString.get(StringIds.FILE_NOT_FOUND, "Foo.txt");
      if (s == null || !s.equals("Could not find file: Foo.txt."))
         System.out.println("\tFAILURE 1-c");

      // Test Model.java
      System.out.println("Testing class Model");
      Model model = new Model();
      int votes = model.getMaxVotes();
      if (votes != 10)
         System.out.println("\tFAILURE 2");

      // Test DataLoader.java
      /*
      System.out.println("Testing Class DataLoader -- Start");
      DataLoader loader = new DataLoader("AutotestAll.txt");
      Vector tests = (Vector)loader.load();
      if (tests == null || tests.size() <= 2)
         System.out.println("\tFAILURE 3");
      */
      
      // Testing threshold calculator
      System.out.println("Testing Threshold Calculation -- Start");
      Engine engine = new Engine();
      // Parameters: rules, decimals, seats, votes
      if ( !engine.calculateThreshold(Rules.DROOP, 0, 1, 100).equals("51.0") )
         System.out.println("\tFAILURE 4-a");
      if ( !engine.calculateThreshold(Rules.DROOP, 0, 5, 100).equals("17.0") )
         System.out.println("\tFAILURE 4-b");
      if ( !engine.calculateThreshold(Rules.HARE, 0, 2, 100).equals("50.0"))
         System.out.println("\tFAILURE 4-c");
      if ( !engine.calculateThreshold(Rules.HARE, 0, 3, 100).equals("33.0"))
         System.out.println("\tFAILURE 4-d");
      if ( !engine.calculateThreshold(Rules.HARE, 1, 3, 100).equals("33.3"))
         System.out.println("\tFAILURE 4-e");
      if ( !engine.calculateThreshold(Rules.DROOP, 0, 7, 27).equals("3.4"))
         System.out.println("\tFAILURE 4-f");
      if ( !engine.calculateThreshold(Rules.DROOP, 0, 7, 2).equals("0.26"))
         System.out.println("\tFAILURE 4-g");

      // Testing Sorter
      System.out.println("Testing Sorter -- Start");
      Vector vec = new Vector();
      vec.addElement("simple");
      vec.addElement("days");
      vec.addElement("can");
      vec.addElement("be");
      vec.addElement("fine");
      vec = Sorter.sort(vec.elements(),Sorter.ASCENDING);
      if ( !(vec.elementAt(0).equals("be")) ||
           !(vec.elementAt(1).equals("can")) ||
           !(vec.elementAt(2).equals("days")) ||
           !(vec.elementAt(3).equals("fine")) ||
           !(vec.elementAt(4).equals("simple"))
          )
         System.out.println("\tFAILURE 5");

      System.out.println("\n***Ending Tester***\n");
   }

   private void testFileOutput()
      throws IOException
   {
/*
      byte[] byteBuf = {'t','e','s','t',' ','1'};//"test 1"
      String buf = "testing...1,2,3";

      FileOutputStream stream = new FileOutputStream("FileOutputStream.txt");
      stream.write(byteBuf);
      stream.close();

      stream = new FileOutputStream("OutputStreamWriter.txt");
      OutputStreamWriter osw = new OutputStreamWriter(stream);
      osw.write(buf);
      stream.close();

      ///stream = new FileOutputStream("Test3.txt");
      FileWriter fw = new FileWriter("BufferedWriter.txt");
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write("test 3");
      bw.close();

      stream = new FileOutputStream("PrintWriter.txt");
      PrintWriter pw = new PrintWriter(stream);
      pw.write("test 4");
      stream.close();

      JSBufferedWriter jpw = new JSBufferedWriter("JSBufferedWriter.txt");
      jpw.println("test 5");
      jpw.close();
*/
      JSFileWriter jfw = new JSFileWriter("JSFileWriter.txt");
      jfw.println("println()");
      jfw.print("print()1;");
      jfw.print("print()2;");
      jfw.print("newline: \n");
      jfw.print("newline rn: \r\n");
      jfw.println("last");
      jfw.close();
/*
      fw = new FileWriter("FileWriter.txt");
      fw.write("test 3");
      fw.close();
*/
  }

   private void testDateFormat()
   {
      DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
      System.out.println("format(Date) FULL FULL == " + df.format(new Date()));

      df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
      System.out.println("format(Date) LONG LONG == " + df.format(new Date()));

      df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
      System.out.println("format(Date) MEDIUM MEDIUM == " + df.format(new Date()));

      df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
      System.out.println("format(Date) SHORT SHORT == " + df.format(new Date()));

      df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
      System.out.println("format(Date) DEFAULT DEFAULT == " + df.format(new Date()));

      df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);
      System.out.println("format(Date) FULL SHORT == " + df.format(new Date()));
   }
}
