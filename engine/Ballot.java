package engine;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import reporter.ICPProReporter;
import util.BCD;
import util.Sortable;

/** Represents one ballot -- for now within one Contest */
public class Ballot implements Sortable
{
   // NOTE: If a member variable is added, modify constructor, toString()
   // and clone().
   private String ballotId = null;
   boolean ballotIdIsAlpha = false;
   private Vector rankings = new Vector();    // Vector of BallotRankings
   private int currRankingNdx = 0;   // The current ndx within the ranking array

   // This is a record of what vote allocations there are.
   // E.g., that "A" has .5 of the vote, "B" has .38, and the
   // exhausted pile .12.  The key is the candidate, while
   // the contents are BCD's that are the actual allocated vote.
   private Hashtable voteAllocations = new Hashtable();

   // The original value is its original value, typically 1.0.
   private BCD origValue = new BCD(1.0D);

   // A temporary work integer that can be used for any purpose.
   // Is used by Engine.transferSurplusCambridge().
   public int workInt = 0;

   public BCD getOrigValue()     { return origValue; }
   public String getIdAlpha() { return ballotId; }
   public int getIdNumeric()  { return Integer.parseInt(ballotId); }
   public Vector getRankings()   { return rankings; }

   public void setOrigValue(BCD value)
   {
      origValue = value;
   }

   /** Standard constructor */
   public Ballot()
   {
   }

   /** toString() override */
   public String toString()
   {
      StringBuffer buf = new StringBuffer();

      buf.append(ballotId + ") ");
      for (int i = 0; i < rankings.size(); i++)
      {
         BallotRanking ranking = (BallotRanking)rankings.elementAt(i);
         if (ranking.cand != null)
            buf.append(ranking.cand.getAbbr());
         else buf.append("null");
         if (ranking.givenRanking != 0)
            buf.append("[" + ranking.givenRanking + "]");
         if (ranking.nextIsDup)
            buf.append('=');
         else if (i != rankings.size() - 1) // not the last one
            buf.append(',');
      }
      return buf.toString();
   }

   /** Set the ballot id */
   public void setBallotId(String id)
   {
      ballotId = id;
      ballotIdIsAlpha = true;
   }

   /** Set the ballot id */
   public void setBallotIdNumeric(String id)
   {
      ballotId = id;
      ballotIdIsAlpha = false;
   }

   /** Append a ballot ranking */
   public void addRanking(Candidate cand, boolean isDup, int givenRanking)
   {
      try {
         BallotRanking ranking = new BallotRanking(cand, isDup, givenRanking);
         rankings.addElement(ranking);
      }
      catch (OutOfMemoryError outOfMem) {
         Runtime runtime = Runtime.getRuntime();
         System.out.println("Free memory == " + runtime.freeMemory());
         System.out.println("Total memory == " + runtime.totalMemory());
         throw outOfMem;
      }
   }

   /**
    * E.g. from Engine: Vector xferTo = ballot.getCandsToXferTo();
    * Uses rules.duplicateRankings.
    * @return Empty Vector to EXHAUST a ballot.
    */
   public Vector getCandsToXferTo(Rules rules, int round)
   {
      Vector dest = new Vector(); // where this ballot is to go to?
      BallotRanking currRanking = null;
      int newRankingNdx = -1;             //store next starting point
      boolean done = false;
      boolean skipping = false;
      int ndx = -2;
      try {
         BallotRanking lastRanking = null;
         for (ndx = currRankingNdx; ndx >= 0 && ndx < rankings.size() && !done; ndx++) {
            BallotRanking continuingRanking = null;
            skipping = false;
            currRanking = (BallotRanking)rankings.elementAt(ndx);
            if (currRanking == null || currRanking.cand == null)
               done = true;
            else if (rules.duplicateRankings == Rules.SKIPPED && 
                       (currRanking.nextIsDup || (lastRanking != null && lastRanking.nextIsDup) )
                    ) {
               done = false;     // Normal case; Just note that we aren't done,
               skipping = true;  // and don't add it to the 'dest' vector.
            } else if (rules.duplicateRankings == Rules.EXHAUST_ANY &&
                       (currRanking.nextIsDup || (lastRanking != null && lastRanking.nextIsDup) )
                    ) { // We have hit a duplicate and need to exhaust this ballot - leave vector empty
               done = true;
            } else if (rules.duplicateRankings == Rules.EXHAUST_CONTINUING && currRanking.nextIsDup) { 
               // We have hit a duplicate and need to look for continuing candidates
///System.out.print("Ballot.getCandsToXfer got dupe " + currRanking.cand + " in ballot " + ballotId + ": " + this.toString() + " under EXHAUST_CONTINUING!\n");
               boolean inDupes = true;
               for (; ndx < rankings.size() && !done && inDupes; ndx += !done && inDupes ? 1 : 0) {
            	   currRanking = (BallotRanking)rankings.elementAt(ndx);
                   inDupes = currRanking != null && currRanking.nextIsDup;
                   if (currRanking == null || currRanking.cand == null) {
                	   if ( continuingRanking != null ) { // no more rankings and only one continuing
                		   dest.addElement(continuingRanking.cand);
                		   done = true;
                		   newRankingNdx = ndx;
                	   }
                   } else if (currRanking.cand.getStatus() == Candidate.CONTINUING ||
                              currRanking.cand.getStatus() == Candidate.UNDEFEATABLE ) {
                	   if (continuingRanking == null) {
                		   continuingRanking = currRanking;
                	   } else { // second continuing candidate
                    		 done = true; // need to exhaust without transferring
                	   }
                   }
               } // end of loop for dupes
               if (!done && continuingRanking != null) {
                	  dest.addElement(continuingRanking.cand);
                	  done = true;
                	  newRankingNdx = ndx;
                  }
            } // end of found dupe 
            if ( !done &&
            	 !skipping &&
                 ( currRanking.cand.getStatus() == Candidate.CONTINUING ||
                   currRanking.cand.getStatus() == Candidate.UNDEFEATABLE 
                 )
              	) {
            	if (newRankingNdx == -1)
            		newRankingNdx = ndx;
				dest.addElement(currRanking.cand);
            }
			if (dest.size() > 0 && !currRanking.nextIsDup)
				done = true;
            lastRanking = currRanking;
         }
         currRankingNdx = newRankingNdx;
      }
      catch (Exception e) {
         if (Mode.developing()) {
            Thread.dumpStack();
            System.out.println(e + "\nrankings.size() == " + rankings.size());
            System.out.println("ndx == " + ndx);
            System.out.println("ballot == " + this);
         }
      }
      return dest;
   }

   /** Get the final allocation for a candidate. */
   public BCD getAllocation(Candidate cand)
   {
      BCD alloc = (BCD)voteAllocations.get(cand);
      return alloc;
   }

   public Enumeration getAllocationKeys() { return voteAllocations.keys(); }

   /** Set the allocation for the receiving candidates.
       'Dest' must not be null, and must contain at least one Candidate.
    */
   public void transfer(Candidate src, Vector dest, BCD xferValue, ICPProReporter reporter)
   {
      // How much should we transfer
      BCD currAllocSrc = null;   // current allocation to the source candidate
      if (src == null)        // this is in the first round
         currAllocSrc = new BCD(origValue);
      else currAllocSrc = getAllocation(src);
      BCD amountToXfer = currAllocSrc.multiply(xferValue);

      // Remove votes from the 'src' Candidate
      if (src != null) {
         if (xferValue.equals(BCD.ONE)) { // remove all from this person
            voteAllocations.remove(src);
            // OK, look dummy -- you CAN'T remove this ballot now,
            // because in Engine, in the transferring methods, you
            // are iterating over this array.  The ballot must be
            // removed in the Engine class, probably after the looping.
            ///src.removeBallot(this);   //Fixes Bug #128
         }
         else {
            BCD newValue = currAllocSrc.subtract(amountToXfer);
            voteAllocations.put(src, newValue);
         }
         src.subtractVotes(amountToXfer);
      }

      // Now add the proper amount to each destination Candidate.
      BCD amountToXferToEach = amountToXfer.divide( new BCD(dest.size()) );
      for (int i = 0; i < dest.size(); i++)
      {
         Candidate cand = (Candidate)dest.elementAt(i);
         BCD currAllocDest = getAllocation(cand);
         BCD newValue = amountToXferToEach;
         if (currAllocDest != null)   //already has some of this vote
            newValue = currAllocDest.add(amountToXferToEach);
         else cand.appendBallot(this);   // he had none of this ballot; so add it to his list
         voteAllocations.put(cand, newValue);
         cand.addVotes(amountToXferToEach);
         reporter.onTransferOccurred(src,cand,this,amountToXferToEach);
      }
   }

   /** Return true if the ballot starts out duplicated */
   public boolean startsDuplicated()
   {
      BallotRanking start = (BallotRanking)rankings.elementAt(0);
      return start.nextIsDup;
   }

   /** @return negative number if this is < other; 0 if equal,
            and postive number if this is > other.
      Implements Sortable.
    */
   public int compare(Object other)
   {
      int ireturn = 0;
      if (ballotIdIsAlpha)
          ireturn = ballotId.compareTo(((Ballot)other).ballotId);
      else {
          long thisId = Long.parseLong(ballotId);
         long otherId = Long.parseLong( ((Ballot)other).ballotId );
          ireturn = (thisId == otherId) ? 0 : (thisId > otherId) ? 1 : -1;
      }
      return ireturn;
   }
   
   public boolean hasNoDuplicates() {
	   boolean noDupes = true;
	   for (int i = 0; noDupes && i < rankings.size(); i++) 
		   noDupes = !((BallotRanking) rankings.get(i)).nextIsDup;
		   
	   return noDupes;
   }

} // end Ballot class

