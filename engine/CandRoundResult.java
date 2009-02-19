package engine;

import util.BCD;

/**
 * A public class (like a 'C' struct).
 * Represents one candidate's results for one round.
 */
public class CandRoundResult  
{                                 
   public RoundResult roundResults = null;   // a pointer to its parent
   public Candidate cand = null;              // which candidate is this for?
   public BCD voteDiff = null;              // what is the difference this round; votes gained or lost
   public BCD voteTotal = null;             // what is the total vote for this candidate at this point?
   public int status = Candidate.UNDEFINED; // what is the candidates status at this point?

   // convenient constructor
   public CandRoundResult(RoundResult roundResult, Candidate cand)
   {
      this.roundResults = roundResult;
      this.cand = cand;
      voteTotal = cand.getVotes();
      Results results = roundResults.getParent();
      voteDiff = voteTotal.subtract(results.getVotes(cand, roundResults.getRound()-1));
      status = cand.getStatus();
   }
}
