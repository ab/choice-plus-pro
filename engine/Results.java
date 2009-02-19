

package engine;

import java.util.Vector;
import util.BCD;

public class Results
{
   //public static final int AUTOTEST_FINAL_RESULT = -1;
   Vector results = null; // Vector of RoundResults

   public Results()
   {
      results = new Vector();
   }

   /** Add a RoundResults object to the 'results' vector */
   public void addRR(RoundResult rr)
   {
      results.addElement(rr);
   }

   public String toString()
   {
      return ("\nDUMMY Results.toString()");
   }

   /** Get the number of votes for a candidate for a given round.
       Note: the round parameter is 1-based, but it is stored
       internally zero-based.
    */
   public BCD getVotes(Candidate cand, int round)
   {
      BCD numVotes = null;
      if (round > 0) {
         //
         RoundResult res = (RoundResult)results.elementAt(round - 1);
         numVotes = res.getVotes(cand);
      }

      if (numVotes == null) {
         numVotes = new BCD(0.0D);
         ///throw new FatalDeveloperError(); -- round 0 is OK to check on
      }

      return numVotes;
   }

   /** Get final results */
   public RoundResult getFinalResult()
   {
      return (RoundResult)results.elementAt( results.size() - 1 );
   }

   /** Get number of rounds */
   public int getNumberOfRounds()
   {
      return results.size();
   }
}
