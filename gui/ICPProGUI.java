
package gui;

import java.util.Vector;
import engine.RoundResult;

/**
 * This interface defines the methods that must be defined
 * for a PRMaster GUI.
 *
 * PRMaster consists of three primary modules -- the GUI
 * (gui/IPRMasterGUI), the engine (engine/IPRMasterEngine),
 * and the reporter (reporter/IPRMasterEngine).
 *
 * Note: The "I" at the beginning of a class indicates it
 * is an interface.
 */
public interface ICPProGUI
{
   // Buttons to show.  Returns the button pressed.
   // Use + for multiple buttons.
   public static int OK = 1;
   public static int CANCEL = 2;

   // Used in the ShowRoundResults dialogs.
   public static final int SELECT_WINNER_BUTTON = 0;
   public static final int SELECT_LOSER_BUTTON = 1;
   public static final int CONTINUE_BUTTON = 2;


   /** Emits an error msg
    *    Returns OK, CANCEL, etc.
    *    Type: OK, CANCEL, OK + CANCEL, etc.
    */
   public void msgOK(String str);

   /** Emit a dialog, in which the user is to pick
    *  the loser in a tie or the loser or winner in a distributed
    *  election -- the next candidate or candidates
    *  to defeat.
    */
   public Vector selectCandidate(String title, String text, Vector candidates,
      boolean cancelAllowed, boolean multiSelect);

   /**
    *  When a round is complete, this method is called.
    *  This gives the UI a chance to show round-by-round results,
    *  and, if desired, to hold up the election until ready to
    *  continue.
    *  The program will only continue when this method is done
    *  and returns 'true'.  A return of 'false' means to abort the
    *  election.
    */
   public boolean onRoundComplete(RoundResult results);

   /**
    *  This method is called when the election is complete.
    */
   public void onContestComplete();

   /**
    * This method is called each time a ballot is loaded, with
    * the total number of ballots that have been loaded as the
    * 1st parameter, and whether or not the load is done as the
    * 2nd parameter.
    */
   public void ballotLoaded(int totalCount, boolean done);

   /** Get the initial file to load */
   ///public String initialDataFileDialog();

   /** Start the GUI */
   public void start();

   /**
    *	Shows results for the current round.
    * In PRMasterGUI, shows candidate name, change this round, and total.
	 *
    * If it is a distributed election, then show "Select Winner" and "Select Loser"
    * buttons.  Otherwise show "Continue" button (a feature Cambridge wanted).
    *
    * IMPLEMENTS IPRMasterGUI.
    */
   public int showRoundResults(int currentRound, Vector sortedCands, boolean isDistributed);

}
