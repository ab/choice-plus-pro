

package engine;

import java.util.Hashtable;
import java.util.Vector;
import util.BCD;

// A class that represents all of the results for
// one round.
public class RoundResult
{
   private Results parent = null;     // What Results set do I belong to?
   private int round = 0;             // which round is this for?
   private Hashtable byCand = null;   // of CandRoundResults -- indexed by candidate
   private Vector byStanding = null;  // of CandRoundResults -- sorted by standing
   private String transferFromExplanation = null;
   private Vector statusChangeExplanation = null; // of Strings
   private CandRoundResult exhaustedPile = null;

   /** Constructor */
   public RoundResult(Results results, int round)
   {
      this.parent = results;
      this.round = round;
      byCand = new Hashtable();
      byStanding = new Vector();
      statusChangeExplanation = new Vector();
   }

   public void setTransferExpl(String s) { transferFromExplanation = s; }
   public void addStatusChangeExpl(String s)
                                 { statusChangeExplanation.addElement(s); }
   public void setExhausted(CandRoundResult exh) { exhaustedPile = exh; }

   public Results getParent() { return parent; }
   public int getRound() { return round; }
   public Hashtable getByCand() { return byCand; }
   public Vector getByStanding() { return byStanding; }
   public String getTransferExpl() { return transferFromExplanation; }
   public Vector getStatusChangeExpl() { return statusChangeExplanation; }
   public CandRoundResult getExhausted() { return exhaustedPile; }

   public BCD getVotes(Candidate cand)
   {
      CandRoundResult crr = null;
      if (cand.getType() == Candidate.EXHAUSTED_PILE)
         crr = exhaustedPile;
      else crr = (CandRoundResult)byCand.get(cand);
      return crr.voteTotal;
   }

   /** Add a candidate round result to this*/
   public void addCRR(CandRoundResult crr)
   {
      byStanding.addElement(crr);
      byCand.put(crr.cand, crr);
   }

   /** Get a candidate round result */
   public CandRoundResult getCRR(Candidate cand)
   {
      return (CandRoundResult)byCand.get(cand);
   }

}




