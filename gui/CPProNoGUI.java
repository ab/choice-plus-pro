

package gui;

import java.io.IOException;
import java.util.Vector;

import engine.RoundResult;
import util.*;
import engine.Mode;

/** <pre>
   This is CPPro's standard GUI.
   (Empty as of 3/99)
   @see ICPProGUI, CPProNoGui
 */
public class CPProNoGUI implements ICPProGUI
{
   JSFileWriter errorLog = null;

   public CPProNoGUI()
   {
       try {
          String title = LiteralString.gets("report.title.errorLogStd");
          errorLog = new JSFileWriter(title + ".txt");
          errorLog.println(title);
          errorLog.newlines(2);
       }
       catch (IOException e) {
         msgOK( JSError.buildMsg(JSError.FATAL, 1006, e.getMessage()) );
       }
   }

   /** Emits an error msg */
   public void msgOK(String str)
   {
      ///System.out.println("DUMMY CPProNoGui.errorMsg(): " + str);
      System.out.println(str);
   }

/* -- old one (7/3/99)
   public void errorMsg(int type, int msgId, Object[] params)
   {
      DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
      String dateAndTime = df.format(new Date());
      String key = null;
      StringBuffer buf = new StringBuffer();

      buf.append(dateAndTime);
      buf.append(":  ");
      if (type == JSError.FATAL) {
         buf.append(LiteralString.gets("error.title"));
         key = "error." + msgId;
      }
      else if (type == JSError.WARNING) {
         buf.append(LiteralString.gets("warning.title"));
         key = "warning." + msgId;
      }
      buf.append(" #" + msgId + ": ");
      buf.append( LiteralString.gets(key, params) );

      errorLog.println(buf.toString());
      errorLog.flush();

      if (type == JSError.FATAL) {
         errorLog.println(LiteralString.gets("aborting"));
         errorLog.flush();

         System.out.println(buf);
         System.out.println(LiteralString.gets("aborting"));
         System.exit(-1);
      }
   }
*/

   /** Emit a dialog, in which the user is to pick
    *  the loser in a tie or the loser or winner in a distributed
    *  election -- the next candidate or candidates
    *  to defeat.
    *  Implements method in ICPProGUI
    */
   public Vector selectCandidate(String resFileTitle, String resFileInst, Vector candidates,
      boolean cancelAllowed, boolean multiSelect)
   {
   	Vector chosen = new Vector();
      chosen.addElement(candidates.elementAt(candidates.size() -1));
      return chosen;
   }

   /**
    *  When a round is complete, this method is called.
    */
   public boolean onRoundComplete(RoundResult results)
   {
      if (Mode.developing())
         System.out.println("Round " + results.getRound() + " complete.");
      return true;
   }

   /**
    *  This method is called when the election is complete.
    *  For this class, intentionally do nothing.
    */
   public void onContestComplete()
   {
      try {
         errorLog.close();
      }
      catch (Exception e) {
         //Do something later!
      }
   }

   /**
    * REQUIRED METHOD.
    * This method is called each time a ballot is loaded, with
    * the total number of ballots that have been loaded as the
    * parameter.
    */
   public void ballotLoaded(int totalCount, boolean done)
   {
      final int SHOW_COUNT = 100;
      StringBuffer buf = new StringBuffer();

      if (done && totalCount > 0) {
         System.out.println();
         buf.append(totalCount + " ballots loaded.  Load complete.");
/*
         if (Mode.developing()) {
            buf.append("; Free memory == " + runtime.freeMemory());
            buf.append("; Total memory == " + runtime.totalMemory());
         }
*/
         System.out.println(buf);
      }
      else if (totalCount > 0 && totalCount % SHOW_COUNT == 0) {
         if (totalCount == SHOW_COUNT)
            buf.append("Loaded " + totalCount + " ballots...");
         else buf.append(totalCount + "...");
/*
         if (Mode.developing()) {
            buf.append("; Free memory == " + runtime.freeMemory());
            buf.append("; Total memory == " + runtime.totalMemory());
         }
*/
         System.out.println(buf);
      }

   }

   /** Get the initial file to load */
   ///public String initialDataFileDialog() {return null;/*do nothing*/}
   public void start() {} // do nothing

   /**
    * IMPLEMENTS ICPProGUI.
    *	Here, we just want to ignore the request to show the results, and
    * continue with the election.
    */
   public int showRoundResults(int currentRound, Vector sortedCands, boolean isDistributed)
   {
   	return CONTINUE_BUTTON;
   }

}


