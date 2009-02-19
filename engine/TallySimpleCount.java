package engine;

import java.io.IOException;
import java.util.Vector;

import engine.Contest;
import gui.ICPProGUI;
import util.JSError;
import reporter.ICPProReporter;

public class TallySimpleCount
	extends TallyEngine
{

	public TallySimpleCount()
   {
   	super();
	}
   
   /** Do a simple tally for plurality and ballot measures.
    *  Implements TallyEngine.tally().
    */
   public void tally(Contest currentContest, ICPProReporter reporter, ICPProGUI gui)
   {
      Vector ballots = currentContest.getBallots();
      Ballot ballot = null;
      Candidate cand = null;
      Rules rules = currentContest.getRules();

      for (int ndx = 0; ndx < ballots.size(); ndx++) {
         ballot = (Ballot)ballots.elementAt(ndx);
         Vector dest = ballot.getCandsToXferTo(rules, 1);
         for (int candNdx = 0; candNdx < dest.size(); candNdx++) {
            cand = (Candidate)dest.elementAt(candNdx);
            cand.addVotes(ballot.getOrigValue());
         }
      }

      try {
         reporter.onContestStarted(currentContest);
         reporter.produceSimpleTallyReport(currentContest);
         gui.onContestComplete();
      }
      catch (IOException e) {
         // "Could not open report file(s)!  You might be short of
         //  disk space.  Details: ..."
         gui.msgOK( JSError.buildMsg(JSError.FATAL, 1006, e.getMessage()) );
      }
   }

} 