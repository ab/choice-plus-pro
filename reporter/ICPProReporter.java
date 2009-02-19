

package reporter;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import engine.*;
import util.BCD;


public interface ICPProReporter
{
   public static final int DETAILED_REPORT      = 1;
   public static final int SUMMARY_REPORT       = 2;
   public static final int CHART_REPORT         = 3;
   public static final int TRANSFER_REPORT      = 4;
   public static final int RANDOMIZATION_REPORT = 5;
   public static final int FINAL_PILES_REPORT   = 6;

   /** Get Set the directory where the output is to go */
   public File getOutputDirectory();
   public void setOutputDirectory(File dir);

   /** Call when a transfer occurred. */
   public void onTransferOccurred(Candidate from, Candidate to, Ballot ballot, BCD amount);

   /** Call when a contest tally starts. */
   public void onContestStarted(Contest contest) throws IOException;

   /** Call at the beginning of each round. */
   public void onRoundStarted(Contest contest, int currentRound);

   /** Call at the end of each round. */
   public void onRoundComplete(RoundResult round) throws IOException;

   /** Call when the contest is over. */
   public void onContestComplete(boolean success) throws IOException;

   /** Print a report */
   public void print(int report);

   /** Call when ballots randomized */
   public void onBallotsRandomized(Vector newOrder);

   /** Call when a simple-tally election is complete */
   public void produceSimpleTallyReport(Contest contest) throws IOException;

}
