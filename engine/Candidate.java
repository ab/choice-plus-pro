

package engine;

import java.util.Date;
import java.util.Vector;
import util.*;

public class Candidate implements Sortable
{
   // status
   public static final int UNDEFINED      = StringIds.UNDEFINED;
   public static final int CONTINUING     = StringIds.CONTINUING;
   public static final int ELECTED        = StringIds.ELECTED;
   public static final int DEFEATED       = StringIds.DEFEATED;
   public static final int EXCLUDED       = StringIds.EXCLUDED;
   public static final int UNDEFEATABLE   = StringIds.UNDEFEATABLE;
   public static final int SPECIAL        = StringIds.SPECIAL; //e.g. the Exhausted pile
   private int status = UNDEFINED;

   // type
   public static final int NORMAL      = 0;
   public static final int WRITE_IN    = 1;
   public static final int EXHAUSTED_PILE = 2;
   private int type = NORMAL;

   private Contest contest = null; //what Contest this candidate is running in
   private String name = null;
   private String abbr = null;
   private BCD totalVoteRcvd = null;
   private int roundStatusDetermined = 0;
   private long timeStatusDetermined = 0;
   private Vector ballots = null;
   private boolean isWriteIn = false;
   private boolean hasTransferredBallots = false;
   private int finalOrder = 0;

   public Candidate(Contest contest)
   {
      init(null, null, 0.0D, CONTINUING, 0, contest, NORMAL);
   }
   public Candidate(String name, String abbr, Contest contest)
   {
      init(name, abbr, 0.0D, CONTINUING, 0, contest, NORMAL);
   }
   public void init(String name, String abbr, String totalVoteRcvd, int status,
      int roundDetermined, Contest contest, int type)
   {
      this.name = name;
      this.abbr = abbr;
      this.totalVoteRcvd = new BCD(totalVoteRcvd);
      this.status = status;
      this.roundStatusDetermined = roundDetermined;
      this.timeStatusDetermined = 0;
      this.contest = contest;
      this.type = type;
      this.ballots = new Vector();
      this.isWriteIn = false;
   }
   public void init(String name, String abbr, double totalVoteRcvd, int status,
      int roundDetermined, Contest contest, int type)
   {
      this.init(name,abbr,String.valueOf(totalVoteRcvd),status,roundDetermined,
                  contest,type);
   }


   public String getName() { return name; }
   public String getAbbr() { return abbr; }
   public int getStatus()  { return status; }

   public Vector getBallots() { return ballots; }
   public void appendBallot(Ballot ballot) { ballots.addElement(ballot); }
   public void removeBallot(Ballot ballot) { ballots.removeElement(ballot); }

   public int getRoundDetermined() { return roundStatusDetermined; }
   public int getType()    { return type; }
   public boolean isWriteIn() { return isWriteIn; }
   public boolean ballotsTransferred() { return hasTransferredBallots; }

   public BCD getVotes(){ return totalVoteRcvd; }
   public void setVote(BCD vote)   { totalVoteRcvd = new BCD(vote); }
   public void addVotes(BCD amount) { totalVoteRcvd = totalVoteRcvd.add(amount); }
   public void subtractVotes(BCD amount) { totalVoteRcvd = totalVoteRcvd.subtract(amount); }

   public void setWriteIn(boolean isWriteIn)  { this.isWriteIn = isWriteIn; }
   public void setTransferredFlag() { hasTransferredBallots = true; }

   public void setFinalOrder(int order) { this.finalOrder = order; }
   public int getFinalOrder()       { return finalOrder; }

   public void setStatus(int status, int round)
   {
      this.status = status;
      this.roundStatusDetermined = round;

      Date now = new Date();
      this.timeStatusDetermined = now.getTime();
   }

   public String dump()
   {
      StringBuffer buf = new StringBuffer();
      buf.append('\"' + name + "\" (" + abbr + "): ");
      buf.append("Status is " + LiteralString.get(status) + "; ");
      buf.append(totalVoteRcvd.toString(Rules.DEFAULT_PRECISION) + " votes received");
      if (status != CONTINUING)
         buf.append("; determined in round " + roundStatusDetermined);
      if (isWriteIn)
         buf.append(" (write-in)");
      return buf.toString();
   }  // end of toString()

   /** Turn a status (e.g. Candidate.ELECTED) into its matching string.
       Note that the status is passed in, because we sometimes want a
       past status, not the most recent status.
       @see PRMasterReporter
    */
   public static String statusToString(int statusParam)
   {
      String s = null;
      
      switch(statusParam) {
         case CONTINUING:  s = LiteralString.gets("CONTINUING");  break;
         case ELECTED:     s = LiteralString.gets("ELECTED");     break;
         case DEFEATED:    s = LiteralString.gets("DEFEATED");    break;
         case UNDEFEATABLE:   s = LiteralString.gets("UNDEFEATABLE");   break;
         case EXCLUDED:    s = LiteralString.gets("EXCLUDED");    break;
         case UNDEFINED:
         default:
            if (Mode.developing())
               throw new FatalDeveloperError("Bad candidate status: "+statusParam);
            else s = new String("");
            break;
      }

      return s;
   }

   /** Implements Sortable.compare(Object) */
   public int compare(Object other)
   {
      int compareResult = Sortable.UNKNOWN;
      Rules rules = contest.getRules();

      switch (rules.system) {
         case Rules.STV:
         case Rules.IRV:
            if (rules.distributedCount)
               compareResult = compareAlpha((Candidate)other);
            else
               compareResult = compareSTV((Candidate)other);
            break;
         case Rules.IR:
            compareResult = compareSTV((Candidate)other);
            break;
         default:
            throw new FatalDeveloperError("No system??!!");
      }

      return compareResult;
   }


   /** Compare this candidate v.s. the other alphabetically, by name
    */

   private int compareAlpha(Candidate other)
   {
      int result = Sortable.EQUAL;
      result = this.name.compareTo(other.name);
      if (result > 0)
         result = Sortable.LESSTHAN;
      else if (result < 0)
         result = Sortable.GREATERTHAN;
      else
         result = Sortable.EQUAL;
      return result;
   }


   /** Compare this candidate v.s. the other.
    *    1. Status: ELECTED,CONTINUING,DEFEATED,EXCLUDED,SPECIAL,UNDEFINED
    *    2. If both ELECTED, whomever was elected in an earlier round is better.
    *       2a. If both ELECTED in same round, if it is the first round or
    *           the immediate-elect option is on, then whomever has more votes is better.
    *           If the immediate-elect option is on, and it is not the 1st round,
    *           then go by whoever was elected first.
    *    3. If both CONTINUING, go by whomever has the most votes.  If still tied,
    *       return them as Sortable.EQUAL.
    *    4. If both DEFEATED, whomever was defeated *later* is better.
    */
   private int compareSTV(Candidate other)
   {
      int result = EQUAL;

      if (other == null)
         result = GREATERTHAN;
      else {
         // First, compare the status (ELECTED, DEFEATED, etc.)
         result = compareStatus(other.status);
         if (result == EQUALS) {
            result = BETTERTHAN; //default
// SCW 7/11/00 add UNDEFEATABLE to logic
            if (status == CONTINUING || status == UNDEFEATABLE || status == SPECIAL)
               // Now check the vote total if CONTINUING
               // (I'm not sure why SPECIAL is here...)
               result = compareVote(other);
            else if (status == DEFEATED || status == ELECTED) {
               // Next check the round the status was determined
               if (this.roundStatusDetermined == other.roundStatusDetermined)
                  result = EQUALS;
               else if ((status == ELECTED) &&
                   (this.roundStatusDetermined > other.roundStatusDetermined))
                  result = WORSETHAN;
               else if ((status == DEFEATED) &&
                        (this.roundStatusDetermined < other.roundStatusDetermined))
                  result = WORSETHAN;

               // They have the same status and same roundStatusDetermined.
               // So next check the finalOrder -- once an order is
               // set, we keep it.
               if (result == EQUALS)
                  if (this.finalOrder < other.finalOrder)
                     result = BETTERTHAN;
                  else if (this.finalOrder > other.finalOrder)
                     result = WORSETHAN;

               if (result == EQUALS && contest.getRules().
                        onMeetingThreshold != Rules.CONTINUE_FOR_THIS_ROUND)
               {
                  // Next check the time the statuses were determined;
                  // This matters only if we are beyond round #1, and
                  // using an immediate elect system.
                  if (this.timeStatusDetermined == other.timeStatusDetermined)
                     result = EQUALS;
                  else if ((status == ELECTED) &&
                      (this.timeStatusDetermined > other.timeStatusDetermined))
                     result = WORSETHAN;
                  else if ((status == DEFEATED) &&
                           (this.timeStatusDetermined < other.timeStatusDetermined))
                     result = WORSETHAN;
                  else result = BETTERTHAN;
               }
            }
            else result = EQUALS;

         }
      }

      return result;
   }

   public boolean hasEarlierOrGreaterSurplus(Candidate other)
   {
	   boolean hasEoGS = other == null;
	   int rDiff = 0;
	   long tDiff = 0L;
	   if (!hasEoGS) {
		   hasEoGS = this.status == ELECTED && other.status != ELECTED;
		   if (!hasEoGS && this.status == ELECTED) {
			   rDiff = other.roundStatusDetermined - this.roundStatusDetermined;
			   if (rDiff == 0)
				   tDiff = other.timeStatusDetermined - this.timeStatusDetermined;
			   hasEoGS = rDiff > 0 || tDiff > 0 || (rDiff == 0 && tDiff > 0 && this.compareVote(other) == GREATERTHAN);
		   }
	   }
	   return hasEoGS;
   }
   
   private int compareVote(Candidate other)
   {
      int result = -1;

      int voteDiff = this.totalVoteRcvd.compareTo(other.totalVoteRcvd);
      if (voteDiff == 0)
         result = EQUALS;
      else if (voteDiff < 0)
         result = WORSETHAN;
      else result = GREATERTHAN;

      return result;
   }

   /** Compare the status of this v.s. the other */
   private int compareStatus(int otherStatus)
   {
      int result = Sortable.GREATERTHAN; //default is GREATER THAN -- i.e. that I am better than 'other'

      if (this.status == otherStatus)
         result = Sortable.EQUALS;
      else {
         // Well, we know we aren't equal, and GREATERTHAN
         // is the default.  So just determine if we are LESSTHAN
         // the other status.
         switch (this.status) {
            case ELECTED: // best
               // do nothing, let default GREATERTHAN stand
               break;
// SCW 7/11/00 add UNDEFEATABLE in this testing
            case UNDEFEATABLE: // #2
               if (otherStatus == ELECTED)
                  result = Sortable.LESSTHAN;
               break;
            case CONTINUING: // #3
               if (otherStatus == ELECTED || otherStatus == UNDEFEATABLE)
                  result = Sortable.LESSTHAN;
               break;
            case DEFEATED: // almost the worst
               if (otherStatus == ELECTED || otherStatus == UNDEFEATABLE || otherStatus == CONTINUING)
                  result = Sortable.LESSTHAN;
               break;
            case EXCLUDED:
               if (otherStatus != SPECIAL && otherStatus != UNDEFINED)
                  result = Sortable.LESSTHAN;
               break;
            case SPECIAL:
               if (otherStatus != UNDEFINED)
                  result = Sortable.LESSTHAN;
               break;
            case UNDEFINED:
               result = Sortable.LESSTHAN;
               break;
            default:
               throw new FatalDeveloperError();
         } // end switch()
      } // end if()

      return result;
   }

   /** Do an STV compare, which ignores some values sometimes */
   public boolean equals(Candidate other)
   {
      return compareSTV(other) == Sortable.EQUALS;
   }

   /** Equals for autotesting purposes */
   public boolean equalsAutotest(Candidate other)
   {
      BCD otherVote = other.totalVoteRcvd.round(Rules.DEFAULT_PRECISION);
      int voteCompare = totalVoteRcvd.compareTo(otherVote);
      boolean equal =
         (
            abbr.equals(other.abbr) &&
            voteCompare == 0 &&
            status == other.status &&
            roundStatusDetermined == other.roundStatusDetermined
         );

      if (!equal)
         equal = false;

      return equal;
   }

   /** toString() override of Object.toString() */
   public String toString()
   {
      return getName();
   }
} // end of class Candidate
