

package engine;

/** BallotRanking
 * A pure public class; i.e. like a 'C' struct.
 * Each ballot has a number of rankings (0-x).
 * A ranking may be a duplicate ranking (e.g. 2 #3's)
 * For auditing purposes, the actual ranking the voter assigned
 *   will be retained, if it is supplied to CPPro.
 */
public class BallotRanking  
{
   public Candidate cand = null;
   public boolean nextIsDup = false;
   public int givenRanking = 0;

   BallotRanking(Candidate cand, boolean isDup, int givenRanking)
   {
      this.cand = cand;
      this.nextIsDup = isDup;
      this.givenRanking = givenRanking;
   }
} // end internal BallotRanking class

