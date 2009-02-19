package engine;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

///import engine.Contest;
import gui.ICPProGUI;
import reporter.ICPProReporter;
import util.BCD;
import util.FatalDeveloperError;
import util.JSError;
import util.Library;
import util.LiteralString;
import util.Sorter;

/**
 *  This is an implementation of TallyEngine
 *
 *  @see engine.TallyEngine
 *  @author JLindsay 12/00
 */
public class TallySTV
   extends TallyEngine
{
   protected Vector transferSource = null;
   protected String transferReason = null;
   protected int numberElected = 0;
   protected int numberUndefeatable = 0;
   protected int numberContinuing = 0;
   protected int numberDefeated = 0;
   protected int currentRound = 0;
   protected RoundResult currentRoundResults = null;
   protected int thresholdPrecision = 0;
   protected boolean cambridgeSimulDropDone = false;
   protected Rules rules = null;
   protected Contest currentContest = null;
   protected ICPProReporter reporter = null;
   protected ICPProGUI  gui = null;
//   protected LiteralString LS = new LiteralString();


   public TallySTV()
   {
      super();
      transferSource = new Vector();
      transferReason = new String();
   }

   /** Implements TallyEngine.tally() */
   public void tally(Contest contest, ICPProReporter reporter, ICPProGUI gui)
   {
      this.reporter = reporter;
      this.gui = gui;
      currentContest = contest;
      rules = currentContest.getRules();
      boolean dataOK = true;
      calcThreshold();

      try {
         reporter.onContestStarted(currentContest);
      }
      catch (IOException e) {
         // "Could not open report file(s)!  You might be short of
         //  disk space.  Details: ..."
         gui.msgOK( JSError.buildMsg(JSError.FATAL, 1006, e.getMessage()) );
         dataOK = false;
      }

      if (dataOK) {
         numberElected = numberDefeated = 0;
         numberContinuing = currentContest.getNumberOfContinuingCandidates();
         numberUndefeatable = currentContest.getNumberOfUndefeatableCandidates();

         boolean continueTally = tallyFirstRound();
//System.out.println("continueTally from tallyFirstRound =" + continueTally);
		while (continueTally) {
            continueTally = tallyNextRound();
//System.out.println("continueTally from tallyNextRound =" + continueTally);
		}
		try {
            reporter.onContestComplete(isElectionTallySuccessful());
            gui.onContestComplete();
         }
         catch (IOException e) {
            gui.msgOK( JSError.buildMsg(JSError.WARNING, 2001, e.getMessage()) );
         }
      }
   }

   // Compute the threshold
   // Overridden in TallyBottomsUp
   protected BCD calcThreshold()
   {
      BCD t = calculateThreshold( rules.thresholdType, rules.thresholdDecimals,
         rules.numberToElect, currentContest.getVoteTotal() );

      currentContest.setThreshold(t);
      return(t);
   }

   /**
    *  Calculates the threshold
    */
   private BCD calculateThreshold(int thresholdType, int decimalPrecision, int seats, BCD votes_BCD)
   {
      // E.g. #1: If prec == 0, seats 9, votes 9000
      //   mult = 1, droopDivide = 900, hareDivide & hare = 1000, droop = 901
      // E.g. #2: If prec == 1, seats 7, votes 8765
      //   mult = 10, droopDivide = 10956.25, hareDivide = 12521.428...
      //   droop = 1095.7, hare = 1252.1
      double votes = votes_BCD.doubleValue();
      double mult = Math.pow((double)10, (double)decimalPrecision);
      double droopDivide = (votes * mult) / (seats + 1);
      double hareDivide = (votes * mult) / seats;
      double droop = (Math.floor(droopDivide+1) / mult);
      double hare = Math.floor(hareDivide) / mult;
      double threshold = (thresholdType == Rules.DROOP) ? droop : hare;

      // Handle pathological cases.
      // Recursively call this method, increasing the precision each time.
      // This is limited recursion -- the rare occassion it is necessary, one
      // recursive call should do the trick.
      if ((threshold * seats > votes) || // threshold too large! (e.g. 24 votes, 7 seats, DROOP)
          (threshold * (seats+1) <= votes) // threshold too small! (e.g. 24 votes, 7 seats, HARE)
         )
      {
         this.thresholdPrecision = decimalPrecision + 1;
         BCD BCDthreshold = calculateThreshold(thresholdType, thresholdPrecision, seats, votes_BCD);
         threshold = BCDthreshold.doubleValue();
      }

      ///return (float)threshold;
      BCD BCDThreshold = new BCD(threshold);
      return new BCD(BCDThreshold.setScale(thresholdPrecision, BCD.DEFAULT_ROUNDING));
   }

   // Tally the first round
   protected boolean tallyFirstRound()
   {
//System.out.println("Entering TallySTV.tallyFirstRound()");
      Vector ballots = currentContest.getBallots();
      Ballot ballot = null;

//rules.dump();
      currentRound = 1;
      reporter.onRoundStarted(currentContest, currentRound);
      currentRoundResults = new RoundResult(currentContest.getResults(), currentRound);
      currentRoundResults.setTransferExpl(LiteralString.gets("reason.xfer.1stRoundTally"));
      for (int ndx = 0; ndx < ballots.size(); ndx++) {
         ballot = (Ballot)ballots.elementAt(ndx);
         addBallotToPiles(null, ballot, BCD.ONE, null);
      } 
//System.out.print("Exiting tallyFirstRound via determineRoundresults\n");
      return determineRoundResults();
   }

   // Tally the next round
   // Loop over the 'transferSource' vector, transferring ballots
   // from each candidate one at a time.
   // * If the candidates are ELECTED, then transfer the
   //   surplus.
   // * If the candidates are DEFEATED, then transfer all
   //   of the ballots.
   // * The actual looping is in other methods.
   protected boolean tallyNextRound()
   {
//System.out.print("Entering tallyNextRound\n");
	   if (transferSource == null || transferSource.size() == 0) {
		   if (rules.onMeetingThreshold != Rules.CONTINUE_TILL_2)
			   throw new FatalDeveloperError("TransferSource should contain something!");
		   else {
		      currentRound++;
		      reporter.onRoundStarted(currentContest, currentRound);
		      currentRoundResults = new RoundResult(currentContest.getResults(), currentRound);
		      Candidate last = getContinuingCandidate();
              currentRoundResults.setTransferExpl(LiteralString.gets("reason.def.byElimination.1", last == null ? "Last candidate" : last.getName()));
              setRemainingStatuses(Candidate.DEFEATED, "reason.def.byElimination",
                       false);
              return determineRoundResults();
		   }
	   }
      currentRound++;
      reporter.onRoundStarted(currentContest, currentRound);
      currentRoundResults = new RoundResult(currentContest.getResults(), currentRound);

      // Look at zeroth candidate to tell whether the vector is full
      // of elected or defeated candidates.  Then, the code below, transfers
      // all the votes.
      Candidate cand = (Candidate)transferSource.elementAt(0);
//System.out.print("TallySTV.tallyNextRound considering candidate " + cand);
      switch (cand.getStatus()) {
         case Candidate.ELECTED: // should never enter here with IRV!
//System.out.println(" who is ELECTED");
            switch (rules.surplusTransfer) {
               case Rules.NEW_YORK:
                  transferSurplusNYC();
                  break;
               case Rules.CAMBRIDGE:
                  transferSurplusCambridge();
                  break;
               case Rules.FRACTIONAL:
                  transferSurplusFractional();
                  break;
               default:
                  throw new FatalDeveloperError();
            }
            if (transferSource.size() == 1)
               currentRoundResults.setTransferExpl(LiteralString.gets("reason.xfer.surplus", cand.getName()));
            else
               currentRoundResults.setTransferExpl(LiteralString.gets("reason.xfer.simulSurplus"));
            break;
         case Candidate.DEFEATED:
//System.out.println(" who is DEFEATED");
            tranferFromDefeatedCandidates();
            if (transferSource.size() == 1)
               currentRoundResults.setTransferExpl(LiteralString.gets("reason.xfer.defeated", cand.getName()));
            else
               currentRoundResults.setTransferExpl(LiteralString.gets("reason.xfer.simulDefeated"));
            break;
         case Candidate.CONTINUING:
         default:
            throw new FatalDeveloperError();
      }
//System.out.print("Exiting tallyNextRound\n");
     return determineRoundResults();
   }

   /** Transfer surplus NYC style */
   public void transferSurplusNYC()
   {
      System.out.println("DUMMY transferSurplusNYC()");
   }

   /** Transfer surplus Cambridge style */
   public void transferSurplusCambridge()
   {
      if (transferSource.size() > 1)
         throw new FatalDeveloperError();

      Candidate srcCand = (Candidate)transferSource.elementAt(0);
//System.out.println("Entering transferSurplusCambridge for candidate " + srcCand);
//      Candidate destCand = null;
      Vector ballots = srcCand.getBallots();
      BCD totalVote = srcCand.getVotes();
      BCD surplusBCD = totalVote.subtract(currentContest.getThreshold());

      // Number the ballots from 1 to ballots.size()
      for (int ballotNdx = 0; ballotNdx < ballots.size(); ballotNdx++) {
         Ballot ballot = (Ballot)ballots.elementAt(ballotNdx);
         ballot.workInt = ballotNdx + 1;
      }

      // Find which 'n' ballots will be transferred
      BCD everyNthBCD = totalVote.divide(surplusBCD);
      everyNthBCD = new BCD(everyNthBCD.setScale(0, BCD.DEFAULT_ROUNDING)); // convert to int
      int everyNth = everyNthBCD.intValue();
      int surplus = surplusBCD.intValue();
//System.out.println("    to transfer everyNth=" + everyNth + " ballot for " + surplus + " in total.");
      // Transfer every nth ballot.  Skip those which would become
      // exhausted.  On pass 2, start at ballot # n+1.  For example,
      // if transferring every 5th ballot, then we transfer #5, #10, etc.
      // On the 2nd pass, we do #6, #11, etc., until we've transferred
      // enough ballots.  The job will normally be done in 2 passes.
      // Note that the elements are accessed via a zero based index, while
      // the workInts are one-based.
      boolean xferExhaustedBallots = false;
      int transferCount = 0;
      boolean done = false;
      for (int pass = 0; !done && pass < everyNth; pass++) {
         for (int ballotNdx = everyNth - 1; !done && ballotNdx < ballots.size(); ballotNdx++) {
            Ballot ballot = (Ballot)ballots.elementAt(ballotNdx);
//System.out.println("      considering ballot " + ballot);
            if ( (ballot.workInt - pass) % everyNth == 0 ) {
               // transfer if not going to be exhausted
               Vector dest = ballot.getCandsToXferTo(rules, currentRound);
               if (!xferExhaustedBallots && (dest == null || dest.size() == 0))
                  done = false; // do nothing
               else {
//System.out.println("      and transferring it");
                  addBallotToPiles(srcCand, ballot, BCD.ONE, dest);
                  transferCount++;
                  if (transferCount == surplus)
                     done = true;
                  // Now, remove the ballot from the candidate's ballot pile,
                  // adjusting the ballotNdx because there is one less ballot
                  // in the pile.
                  ballots.removeElementAt(ballotNdx--);
               }
            }
         }
         // If we just completed the last path, and we *still* don't
         // have enough ballots transferred, repeat again, but this time
         // transferring surplus ballots is acceptable.
         if (pass == everyNth - 1 && transferCount < surplus) {
            pass = -1; // will be incremented to 0 by the for loop
            xferExhaustedBallots = true;
         }
      }
      srcCand.setTransferredFlag();
   }

   /** Transfer surplus in the fractional manner (default) */
   /// NOTE: "...ballots = ...clone();" is necessary, because otherwise, as
   ///    we remove old ballots and add new (reduced value) ballots from the
   ///    transferSource, we are changing the primary vector we are operating on!
   public void transferSurplusFractional()
   {
      for (int candNdx = 0; candNdx < transferSource.size(); candNdx++) {
         Candidate cand = (Candidate)transferSource.elementAt(candNdx);
//         Candidate dest = null;
         Vector ballots = cand.getBallots();
         BCD surplus = cand.getVotes().subtract(currentContest.getThreshold());
//System.out.print("TallySTV.transferSurplusFraction is transferring " + surplus.toString() + " votes from candidate " + cand);
         BCD xferValue = surplus.divide(cand.getVotes());
         if (xferValue.isNegative())
            throw new FatalDeveloperError();
         for (int ballotNdx = 0; ballotNdx < ballots.size(); ballotNdx++) {
            Ballot ballot = (Ballot)ballots.elementAt(ballotNdx);
            addBallotToPiles(cand, ballot, xferValue, null);
         }
         cand.setTransferredFlag();
      }
   }

   /** Transfer all ballots from the defeated candidates */
   public void tranferFromDefeatedCandidates()
   {
      int i;
//System.out.println("TallySTV.transferFromDefeatedCandidates is transferring from " + transferSource + "\n");
      for (int candNdx = 0; candNdx < transferSource.size(); candNdx++) {
         Candidate cand = (Candidate)transferSource.elementAt(candNdx);
         Vector ballots = cand.getBallots();
//System.out.println("TallySTV.transferFromDefeatedCandidates is transferring " + ballots.size() + " ballots\n");
         if (!rules.transferFromBegOfPile) { // bottom of vector
            for (i = ballots.size()-1; i >= 0; i--) {
               Ballot ball = (Ballot)ballots.elementAt(i);
//System.out.println("TallySTV.transferFromDefeatedCandidates is transferring " + ball + " up from bottom\n");
               addBallotToPiles( cand, ball, BCD.ONE, null );
            }
         } else {
            for (i = 0; i < ballots.size(); i++) {
               Ballot ball = (Ballot)ballots.elementAt(i);
//System.out.println("TallySTV.transferFromDefeatedCandidates is transferring " + ball + " down from top\n");
               addBallotToPiles( cand, ball, BCD.ONE, null );
            }
         }
         ballots.removeAllElements();
         cand.setVote(BCD.ZERO);
         cand.setTransferredFlag();
      }
   }

  /**
   * Determines the results of the round, including whom
   * is ELECTED, DEFEATED, etc., this round.  Also sets up the
   * transforSource Vector, and sends the results to the Results
   * class.
   * Called by tallyFirstRound() and tallyNextRound().
   * @return true if should continue the election
   */
   protected boolean determineRoundResults()
   {
      // Get the candidates
      Enumeration candidates = currentContest.getCandidates().elements();

      // Sort it, in STV strength order (classes Sorter and Sortable)
      Vector sortedCands = Sorter.sort(candidates,Sorter.DESCENDING);
//System.out.println("TallySTV.determineRoundResults got sorted candidates " + sortedCands.toString());
      // Set up the transferSource Vector
      // This also involves setting candidate statuses.
      getTransferSource(sortedCands);

      // Fix the final order, for those that do not have a previously
      // fixed order.  Once a candidate comes in 2nd for example,
      // he/she stays there.
      for (int i = 0; i < sortedCands.size(); i++)
      {
         Candidate cand = (Candidate)sortedCands.elementAt(i);
      // SCW 7/12/00 don't set final order for UNDEFEATABLE candidates either
         if ( cand.getStatus() != Candidate.CONTINUING
              && cand.getStatus() != Candidate.UNDEFEATABLE
              && cand.getFinalOrder() == 0 )
            cand.setFinalOrder(i + 1);
      }

      // Send results to the Results class
      setResults(sortedCands);

      return (transferSource != null && transferSource.size() > 0) ||
      		 (rules.onMeetingThreshold == Rules.CONTINUE_TILL_2 && 
      		 numberElected + numberContinuing + numberUndefeatable > rules.numberToElect) ;
   }

    // Depends very much on the rules of the election.
    // Decides who wins or loses in this round.
    // Output is class variable engine.transferSource (Vector).
   protected void getTransferSource(Vector sortedCands)
   {
      transferSource.removeAllElements();

      if (rules.distributedCount)
         getDistributedTransferSource(sortedCands); //If using a distributed count, per Ireland
      else
         getLocalTransferSource(sortedCands); //Default behavior, all data is local.
   }

   // Get distributed refers to distributed processing of elections (a la Ireland).
   private void getDistributedTransferSource(Vector sortedCands)
   {
      Vector chosen = new Vector();
      Candidate curr = null;

      for (int i = 0; i < sortedCands.size(); i++) {
         curr = (Candidate)sortedCands.elementAt(i);
         if (curr.getStatus() == Candidate.CONTINUING)
            chosen.addElement(curr);
      }

      if (!Library.isEmpty(chosen)) {
         int buttonPressed = gui.showRoundResults(currentRound, sortedCands, true);
         if (buttonPressed == ICPProGUI.SELECT_LOSER_BUTTON) {
            do {
               chosen = gui.selectCandidate("gui.selectLoser.title", "gui.selectLoser.instructions.",
                  chosen, false, true);
            } while (Library.isEmpty(chosen));
            setTransferSource(chosen, Candidate.DEFEATED, "reason.def.distributed");
            if (numberElected + numberContinuing + numberUndefeatable == rules.numberToElect)
               setRemainingStatuses(Candidate.ELECTED, "reason.ele.byElimination");
         } else {
            do {
               chosen = gui.selectCandidate("gui.selectWinner.title", "gui.selectWinner.instructions.",
                  chosen, false, true);
            } while (Library.isEmpty(chosen));
            setTransferSource(chosen, Candidate.ELECTED, "reason.ele.distributed");
            if ( (rules.system == Rules.IRV || rules.system == Rules.STV)
               && numberElected == rules.numberToElect
               && ( (rules.onMeetingThreshold != Rules.CONTINUE_TILL_2 && numberContinuing > 0) ||
            		numberContinuing > 1 
            	   )
            	)
            {
               // For Cambridge (and NYC, I think), there is an extra
               // transfer round so that all defeated candidates have no
               // ballots, and so that most likely, all elected candidates
               // have met the threshold (certainly none have more than the
               // threshold).
///System.out.println("TallySTV.getDistributedTransferSource is defeating remaining candidates"); 
               setRemainingStatuses(Candidate.DEFEATED, "reason.def.byElimination",
                        false);
               // force the end of election.
               transferSource.removeAllElements();
            }
         }
      }
   }

   //
   // "Get local" refers to non-distributed processing.
   private void getLocalTransferSource(Vector sortedCands)  {
      Vector haveSurplus = new Vector(); // later: simul. xfer of winners
      Candidate nextSurplus = null;
      BCD threshold = currentContest.getThreshold();

      // Pass over the candidates.  Make a vector of all those
      // with too many votes, and all those that have the fewest
      // number of votes.
      for (int i = 0; i < sortedCands.size(); i++) {
         Candidate curr = (Candidate)sortedCands.elementAt(i);
         BCD votes = curr.getVotes();
         // Find any candidates who've met the threshold
         if ( !curr.ballotsTransferred() && 
              ( checkIfElected(curr, votes, threshold) ||
                rules.surplusTransfer == Rules.CAMBRIDGE
              )
            ) {
            if (curr.getStatus() == Candidate.ELECTED && votes.compareTo(threshold) > 0) {
               //votes > threshold
//System.out.println("TallySTV.getLocalTransferSource is adding surplus candidate = " + curr);
               haveSurplus.addElement(curr);
            }
            if (curr.hasEarlierOrGreaterSurplus(nextSurplus))
               nextSurplus = curr;
         }
      }

      boolean transferDefeated = false;
      if (rules.system == Rules.STV || rules.system == Rules.IRV)
         // SCW - 8/25/2005 - Cambridge modification
         // we need to continue transferring from surplus until there are no more 
         // surpluses from elected candidates
         transferDefeated = haveSurplus.size() == 0 ||
            (rules.surplusTransfer != Rules.CAMBRIDGE && numberElected == rules.numberToElect) ;  
//System.out.println("TallySTV.getLocalTransferSource haveSurplus.size = " + haveSurplus.size() + ", transferDefeated = " + transferDefeated + ", rules.system = " + rules.system);
      if ( (rules.system == Rules.IRV || rules.system == Rules.STV) && 
           numberElected == rules.numberToElect && 
           (numberContinuing > 0 || rules.surplusTransfer == Rules.CAMBRIDGE) &&
           (rules.onMeetingThreshold != Rules.CONTINUE_TILL_2 || (numberContinuing + numberElected) > 2)
         ) {
         // For Cambridge (and NYC, I think), there is an extra
         // transfer round so that all defeated candidates have no
         // ballots, and so that most likely, all elected candidates
         // have met the threshold (certainly none have more than the
         // threshold).
         // 1/10/2006 - scw - also need to continue if CONTINUE_TILL_2 and there are more than 2 undeafeated
///System.out.println("TallySTV.getLocalTransferSource is defeating remaining candidates with "); 
         setRemainingStatuses(Candidate.DEFEATED, "reason.def.byElimination",
                  transferDefeated);
//System.out.println("TallySTV.getLocalTransferSource eliminating " + numberContinuing + " candidates with " + numberElected + " elected.");
      } else if (haveSurplus.size() == 0)
         findCandsToDefeat(sortedCands);
      if (!transferDefeated && nextSurplus != null)  // later: can support simul. xfer of winners
         transferSource.addElement(nextSurplus);

      // Note: in a Cambridge Vacancy Recount, even though it
      // is actually IRV, do the extra transfers.  If usung
      // CONTINUE_TILL_2, do transfers if more continuing than 
      if (!rules.cambridgeVacancyRecount && 
          (rules.system == Rules.IRV && numberElected == 1) &&
          (rules.onMeetingThreshold != Rules.CONTINUE_TILL_2 || (transferSource.size() == 1))
         ) {
//System.out.println("TallySTV.getLocalTransferSource is removing " + transferSource.size() + " candidates to transfer with onMeetingThreshold=" + rules.onMeetingThreshold + ".");
            transferSource.removeAllElements();
         }
//System.out.println("TallySTV.getLocalTransferSource got " + transferSource.toString() + " in round " + currentRound);
   }

   // Calc the threshold excluding exhausted ballots.  Used for IRV.
   public BCD calcNonExhaustedThreshold()
   {
      BCD t = calculateThreshold( rules.thresholdType, rules.thresholdDecimals,
         rules.numberToElect,
         currentContest.getVoteTotal().subtract(currentContest.getExhaustedPile().getVotes()));

      return t;
   }



   // Check if the current candidate is to be newly elected, and if so, do
   // everything necessary to properly set his/her status, etc.
   // Overridden by TallyBottomsUp
   protected boolean checkIfElected(Candidate curr, BCD votes, BCD threshold)
   {
      BCD lowerThreshold = null;
      String reason = null;
      boolean elected = false;

      if (curr.getType() == Candidate.NORMAL)
      {
         if (votes == null)
            votes = curr.getVotes();
         if (threshold == null)
            threshold = currentContest.getThreshold();
         if (rules.system == Rules.IRV)
            lowerThreshold = calcNonExhaustedThreshold();
         else lowerThreshold = threshold;

         // SCW 7/11/00: We cannot elect the current candidate even if
         // s/he is over the threshold if there is no room
         // when considering UNDEFEATABLEs.  Also, we must decrement correct counter.
         int stat = curr.getStatus();
         boolean canElect = (stat == Candidate.UNDEFEATABLE ||
                             numberElected + numberUndefeatable < rules.numberToElect);

         if (canElect && votes.compareTo(lowerThreshold) >= 0)
         {
            elected = true;
            if (stat != Candidate.ELECTED)
            {
               numberElected++;
               if (stat == Candidate.CONTINUING)
                  numberContinuing--;
               if (stat == Candidate.UNDEFEATABLE)
                  numberUndefeatable--;
               curr.setStatus(Candidate.ELECTED, currentRound);
               if (lowerThreshold.equals(threshold))
                  reason = "reason.ele.metThreshold";
               else reason = "reason.ele.metLowerThreshold";
               currentRoundResults.addStatusChangeExpl( LiteralString.gets(reason, curr.getName()) );
            }
         }
      }

      return elected;
   }

   // There is no one with a surplus, so find candidates to defeat
   private void findCandsToDefeat(Vector sortedCands)
   {
      if (numberElected == rules.numberToElect &&
          (rules.onMeetingThreshold != Rules.CONTINUE_TILL_2 || (numberElected + numberContinuing) <= 2) 
         ) { // election is over!
//System.out.println("TallySTV.findCandsToDefeat is ending with " + numberElected + " elected and " + numberContinuing + " continuing.");
      } else if ((numberElected + numberUndefeatable) > rules.numberToElect) {
         throw new FatalDeveloperError();
      } else if (numberElected + numberContinuing + numberUndefeatable == rules.numberToElect) {
//System.out.println("TallySTV.findCandsToDefeat is electing " + (numberContinuing + numberElected) + " candidates.");
         setRemainingStatuses(Candidate.ELECTED, "reason.ele.byElimination");
      } else { // continue the election; find whom to defeat
         if (rules.simultaneousDrop == Rules.NEW_YORK)
            findCandsToDefeatNYC(sortedCands);
         else if (rules.simultaneousDrop == Rules.CAMBRIDGE)
            findCandsToDefeatCambridgeStyle(sortedCands);
         else 
            findLowestContCand(sortedCands);
      }

      // In IRV, as soon as the 2nd to last candidate is defeated, then
      // declare the remaining one elected, without transferring #2's votes.
      // In STV, we do transfer votes from all losers.
      // This doesn't affect the outcome, it just *looks* better, more
      // intuitive.
      if (rules.system == Rules.IRV)
         if (numberElected + numberContinuing == rules.numberToElect)
            setRemainingStatuses(Candidate.ELECTED, "reason.ele.byElimination");
   }

   /**
      Using New York City's elegant rule, find the set of
      candidates whom it is safe to defeat.  The principle is
      to defeat as many candidates as possible, as long as they
      must all lose anyway, nor allow anyone remaining candidates
      to reach the threshold.
      E.g.: 200 votes, 1 to elect, threshold is 101.
         A = 79, B = 60, C = 50, D = 5, E = 4, F = 1, G = 1.
         D, E, F & G should be defeated, because they will all
         inevitably lose, and none of the candidates are within
         11 votes of winning.
   */
   private void findCandsToDefeatNYC(Vector sortedCands)
   {
      int found = -1; // Who to start simutaneous drop at.
//System.out.println("entering TallySTV.findCandsToDefeatNYC().");

      // Make an array of doubles, where below[0] is the sum of all
      // the votes of all the candidates below sortedCands[0].
      BCD below[] = new BCD[sortedCands.size()];
      BCD sum = new BCD(0.0D);
      for (int i = sortedCands.size() - 1; i >= 0; i--) {
         Candidate cand = (Candidate)sortedCands.elementAt(i);
         below[i] = sum;
         ///sum += cand.getVotes();
         sum = sum.add(cand.getVotes());
      }

      // Working forwards now, ask the question, if all the votes
      // below me are added to my total, could it place me above
      // whoever is above me?  If not, then I, and everyone below
      // me should be simultaneously defeated.
      // Also, avoid defeating people if that would not leave
      // enough winners.  E.g. if there is to be one winner, start
      // this check at index 2, the third candidate.
      for (int j = rules.numberToElect+1;
           found == -1 && j < sortedCands.size();
           j++)
      {
         Candidate cand = (Candidate)sortedCands.elementAt(j);
         Candidate prevCand = (Candidate)sortedCands.elementAt(j-1);
         if (cand.getStatus() == Candidate.CONTINUING) {
            BCD myMaxTotal = cand.getVotes().add( below[j] );
            if (myMaxTotal.lessThan(prevCand.getVotes()) &&
                myMaxTotal.lessThan(currentContest.getThreshold()))
               found = j; // FOUND SIMULTANEOUS DROP POINT
         }
      }

      if (found != -1) {
         for (int k = found; k < sortedCands.size(); k++) {
            Candidate cand = (Candidate)sortedCands.elementAt(k);
            if (cand.getStatus() == Candidate.CONTINUING) {
               transferSource.addElement(cand);
               if (rules.onMeetingThreshold != Rules.CONTINUE_TILL_2 || numberContinuing > 2) {
            	   cand.setStatus(Candidate.DEFEATED, currentRound);
            	   numberDefeated++;
            	   numberContinuing--;
               }
            }
         }
         // Set the proper reason for the defeat
         if (transferSource.size() > 1)
///            for (int k = found; k < transferSource.size(); k++) {
            for (int k = 0; k < transferSource.size(); k++) {
               Candidate cand = (Candidate)transferSource.elementAt(k);
               currentRoundResults.addStatusChangeExpl(
                           LiteralString.gets("reason.def.inevitable", cand.getName() ));
            }
         else {
            Candidate cand = (Candidate)transferSource.elementAt(0);
            currentRoundResults.addStatusChangeExpl(
                        LiteralString.gets("reason.def.fewestVotes", cand.getName() ));
         }
      }

      if (transferSource.size() == 0)
         findLowestContCand(sortedCands);
   }

   /**
      Defeat all candidates that have less than a certain
      number of votes.  In Cambridge, this is 50.
    */
   private void findCandsToDefeatCambridgeStyle(Vector sortedCands)
   {
//System.out.println("entering TallySTV.findCandsToDefeatCambridgeStyle().");
      BCD minVote = new BCD(rules.cambridgeSimulDropLevel);
      int numGreaterThanMinVote = 0;
      boolean done = false;
      boolean simulDropOK = false;

      if (!cambridgeSimulDropDone)
      {
         // First, find if we have enough candidates with the minimum
         // number of votes.
         for (int i = 0; i < sortedCands.size() && !simulDropOK; i++)
         {
            Candidate cand = (Candidate)sortedCands.elementAt(i);
            if (!cand.getVotes().lessThan(minVote))
               numGreaterThanMinVote++;
            if (numGreaterThanMinVote >= rules.numberToElect)
               simulDropOK = true;
         }

         // Work from the bottom up. We defeat all
         // continuing candidates with less than 50 (i.e. min-vote) votes.
         if (simulDropOK) {
            for (int i = sortedCands.size() - 1; i >= 0 && !done; i--)
            {
               Candidate cand = (Candidate)sortedCands.elementAt(i);
               if (cand.getVotes().lessThan(minVote)
                   && cand.getStatus() == Candidate.CONTINUING)
               {
                  defeatCand(cand, LiteralString.gets("reason.def.lessThanN",
                                  new Object[] {
                                    cand.getName(),
                                    new Integer(rules.cambridgeSimulDropLevel)
                                  } ));
                  transferSource.addElement(cand);
               }
            }
         }
      }

      if (transferSource.size() == 0)
         findLowestContCand(sortedCands);
   }

   private void defeatCand(Candidate cand, String reason)
   {
      cand.setStatus(Candidate.DEFEATED, currentRound);
      numberDefeated++;
      numberContinuing--;
      currentRoundResults.addStatusChangeExpl(reason);
   }

   /** There is no simultaneous defeat happening.
    *  Just find the continuing candidate with the lowest vote.
    */
   private void findLowestContCand(Vector sortedCands)
   {
//System.out.println("entering TallySTV.findLowestContCand().");
      Candidate curr = null;
      BCD leastVotes = null;
//      boolean tieExisted = false;

      // loop over all the candidates, find the bottom ones
      for (int i = sortedCands.size() - 1; i >= 0; i--) {
         curr = (Candidate)sortedCands.elementAt(i);
         if (curr.getStatus() == Candidate.CONTINUING) {
            if (leastVotes == null || curr.getVotes().lessThan(leastVotes)) {
               leastVotes = curr.getVotes();
               transferSource.removeAllElements();
            }
            if (curr.getVotes().equals(leastVotes))
               transferSource.addElement(curr);
         }
      }

      ///if (transferSource.size() == 0)
      ///   throw new FatalDeveloperError();
      ///else
      if (transferSource.size() > 1) {
         breakTie();
      }
      else
         setTransferSource(transferSource, Candidate.DEFEATED, "reason.def.fewestVotes");


      // set status to DEFEATED
/*      for (int i=0; i < transferSource.size(); i++) {
         Candidate cand = (Candidate)transferSource.elementAt(i);
         cand.setStatus(Candidate.DEFEATED, currentRound);
         if (!tieExisted)
            currentRoundResults.addStatusChangeExpl(
                       LiteralString.gets("reason.def.fewestVotes", cand.getName() ));
         numberDefeated++;
         numberContinuing--;
      }
*/
   }

   /**
    *  Tranfer the given ballot to the next set of candidates.
    *  E.g. from tallyFirstRound(): "addBallotToPiles(ballot);"
    */
   private void addBallotToPiles(Candidate src, Ballot ballot, BCD xferValue, Vector dest)
   {
      if (dest == null)
         dest = ballot.getCandsToXferTo(rules, currentRound);

     // Set the allocation for the ballot, for each continuing candidate.
      if (dest.size() == 0)
         dest.addElement(currentContest.getExhaustedPile());
      ballot.transfer(src, dest, xferValue, reporter);
      if (rules.onMeetingThreshold == Rules.IMMEDIATE_ELECT_AFTER_1ST_ROUND
          && currentRound > 1)
      {
         if (dest.size() > 1)
            throw new FatalDeveloperError("dest.size()=" + dest.size() + ", ballot=\"" + ballot + "\"");
         checkIfElected((Candidate)dest.elementAt(0), null, null);
      }
   }

   // Set the status of the continuing candidates
   private void setRemainingStatuses(int statusToSet, String reasonKey)
   {
      setRemainingStatuses(statusToSet, reasonKey, false);
   }

   // Set the rest of the continuing candidates as defeated candidates
   private void setRemainingStatuses(int statusToSet, String reasonKey,
               boolean addToTransferSource)
   {
      Vector cands = currentContest.getCandidates();
      Candidate candStatusSet = null;
      int count = 0;

// SCW 7/11/00 modify to process UNDEFEATABLE
      for (int i = 0; i < cands.size(); i++) {
         Candidate cand = (Candidate)cands.elementAt(i);
         BCD votes = cand.getVotes();
         int stat = cand.getStatus();
         if (stat == Candidate.UNDEFEATABLE) {
            cand.setStatus(Candidate.ELECTED, currentRound);
            numberElected++;
            numberUndefeatable--;
         }
         if (stat == Candidate.CONTINUING) {
            cand.setStatus(statusToSet, currentRound);
            if (addToTransferSource)
               transferSource.addElement(cand);
            if (statusToSet == Candidate.ELECTED)
               numberElected++;
            else if (statusToSet == Candidate.DEFEATED)
               numberDefeated++;
            else throw new FatalDeveloperError();
            numberContinuing--;
            candStatusSet = cand;
            count++;
         } else if (addToTransferSource && 
               stat != Candidate.ELECTED &&  
               rules.surplusTransfer == Rules.CAMBRIDGE && 
               votes.compareTo(BCD.ZERO) > 0)
          // need to add votes for already defeated candidates if using CAMBRIDGE 
             transferSource.addElement(cand);
      }

      if (count < 1)
         ; // do nothing
      else if (count == 1)
         currentRoundResults.addStatusChangeExpl( LiteralString.gets(reasonKey+".1", candStatusSet.getName()) );
      else
         currentRoundResults.addStatusChangeExpl( LiteralString.gets(reasonKey+".n") );
   }

   /** Send the results to the Results object. */
   private void setResults(Vector sortedCands)
   {
      for (int i = 0; i < sortedCands.size(); i++)
      {
         Candidate cand = (Candidate)sortedCands.elementAt(i);
         CandRoundResult crr = new CandRoundResult(currentRoundResults,cand);
         currentRoundResults.addCRR(crr);
      }

      // Set the exhausted pile results
      CandRoundResult crrExhausted = new CandRoundResult(currentRoundResults,currentContest.getExhaustedPile());
      currentRoundResults.setExhausted(crrExhausted);

      currentContest.getResults().addRR(currentRoundResults);
      try {
         reporter.onRoundComplete(currentRoundResults);
      }
      catch (IOException e) {
         // "Could not open report file(s)!  You might be short of
         //  disk space.  Details: ..."
         gui.msgOK( JSError.buildMsg(JSError.FATAL, 1006, e.getMessage()) );
      }
      gui.onRoundComplete(currentRoundResults);
   }

   /** Return 'true' if the election was successfully tallied. */
   public boolean isElectionTallySuccessful()
   {
// SCW 7/12/00 include numberUndefeatable in test
      boolean success = (numberElected + numberUndefeatable == rules.numberToElect);
      if (!success && Mode.developing()) {
         System.out.println("Election failed!?");
         System.out.println( "numberElected == " + numberElected +
                             ", numberUndefeatable == " + numberUndefeatable +
                             ", number to elect == " + rules.numberToElect );
      }
      return success;
   }

   /** Randomly pick one candidate to defeat, from the set of them
       in the transferSource vector.
    */
   private void breakTieRandomByComputer()
   {
      Candidate selected = null;
      String reasonID = null;

      // If autotesting, always pick the alphabetically last candidate.
      // We can't have any true randomness when auto-testing.
      // Note: result undetermined if two candidates have the same name,
      // which should be completely illegal anyway.
      if (Mode.autotesting) {
         selected = (Candidate)transferSource.elementAt(0);
         for (int i = 1; i < transferSource.size(); i++) {
            Candidate cand = (Candidate)transferSource.elementAt(i);
            if (cand.getName().compareTo(selected.getName()) > 0)
               selected = cand;
         }
         reasonID = "reason.def.dropTie.autotestLast";
      }
      else {
         Random rand = new Random();
         int r = rand.nextInt();
         if (r < 0)
            r *= -1;
         int randInt = r % transferSource.size();
         selected = (Candidate)transferSource.elementAt(randInt);
         reasonID = "reason.def.dropTie.randomComputer";
      }

      Vector chosen = new Vector();
      chosen.addElement(selected);
      setTransferSource(chosen, Candidate.DEFEATED, reasonID);
   }

   /**
      Given a set of tied candidates in the 'transferSource' vector,
      find whom to defeat.
    */
   private void breakTie()
   {
      boolean determined = false;
//System.out.println("TallySTV is entering breakTie()");
      for (int i = 0; !determined && i < rules.tieBreakingRules.size(); i++) {
//System.out.println("TallySTV.breakTie() is switching on " + ((Integer)rules.tieBreakingRules.elementAt(i)).intValue());
         switch (((Integer)rules.tieBreakingRules.elementAt(i)).intValue()) {
            case Rules.PREVIOUS_ROUND:
//System.out.println("TallySTV.breakTie() is using breakTiePreviousRoundMethod()");
               determined = breakTiePreviousRoundMethod();
               break;
            case Rules.ONE_PREVIOUS_ROUND:
//System.out.println("TallySTV.breakTie() is using breakTieOnePreviousRoundMethod()");
               determined = breakTieOnePreviousRoundMethod();
               break;
            case Rules.BY_ELECTION_OFFICIAL:
//System.out.println("TallySTV.breakTie() is asking for help");
               if (!Mode.autotesting) {
                  boolean cancelAllowed = (i != rules.tieBreakingRules.size());
                  Vector losers = gui.selectCandidate("gui.breakTie.title", "gui.breakTie.instructions.",
                     transferSource, cancelAllowed, true);
                  determined = !Library.isEmpty(losers);
                  if (determined)
                     setTransferSource(losers, Candidate.DEFEATED, "reason.def.dropTie.selectedByElectionOfficial");
               }
               break;
            case Rules.RANDOM:
            default:
               breakTieRandomByComputer();
               determined = true;
               break;
         }
      }
   }

   /**
      Once a vector of losers or winners is determined, set that vector
      as the transfer source, and add explanations to the round results.
   */
   void setTransferSource(Vector chosen, int status, String reasonID)
   {
      transferSource = chosen;
      for (int j = 0; j < transferSource.size(); j++) {
         Candidate cand = (Candidate)transferSource.elementAt(j);
         cand.setStatus(status, currentRound);
         currentRoundResults.addStatusChangeExpl(LiteralString.gets(reasonID, cand.getName()));
         if (status == Candidate.DEFEATED)
            numberDefeated++;
         if (status == Candidate.ELECTED)
            numberElected++;
         numberContinuing--;
      }
   }

   /**
      Break tie using the previous round method.
      You look at the previous round, and whover has the fewest votes
      will be eliminated.  In case this is still a tie, try the round
      before that.  It is possible to not be able to break the tie with
      this method.
      If the tie is successfully broken, then the transferSource vector
      will have the loser, and the proper reason will be set.
      @return 'true' if it broke the tie
    */
   private boolean breakTiePreviousRoundMethod()
   {
      boolean determined = false;

      if (currentRound > 1) {
         for (int round = currentRound - 1; !determined && round > 0; round--) {
            Vector newTransferSource = breakTieOnePreviousRound(round);

            if (newTransferSource != null && newTransferSource.size() == 1) {
               determined = true; //stop looping, thank you
               setTransferSource(newTransferSource, Candidate.DEFEATED,
                  "reason.def.dropTie.previousRoundMethod");
            }
         } // end of looping over the rounds
      } // end if

      return determined;
   }

   /**
      Break tie using the previous round method, but limited to one round look-back.
      You look at the previous round, and whover has the fewest votes
      will be eliminated.  The tie may not be broken.
      If the tie is successfully broken, then the transferSource vector
      will have the loser, and the proper reason will be set.
      @return 'true' if it broke the tie
    */
   private boolean breakTieOnePreviousRoundMethod()
   {
      boolean determined = false;
      Vector newTransferSource = null;

      if (currentRound > 1) {
         newTransferSource = breakTieOnePreviousRound(currentRound - 1);
         if (newTransferSource != null && newTransferSource.size() == 1) {
            determined = true; // we broke the tie
            setTransferSource(newTransferSource, Candidate.DEFEATED,
               "reason.def.dropTie.previousOneRoundMethod");
         }
      } // end if

      return determined;
   }

   private Vector breakTieOnePreviousRound(int round) {
      Results results = currentContest.getResults();
      Vector newTransferSource = new Vector();
      BCD lowestVote = null;
      BCD currentVote = null;
      Candidate cand = null;
      for (int candNdx = transferSource.size()-1; candNdx >= 0; candNdx--) {
         cand = (Candidate)transferSource.elementAt(candNdx);
         currentVote = results.getVotes(cand, round);
         if (lowestVote == null) {
            newTransferSource.addElement(cand);
            lowestVote = currentVote;
         }
         else if (currentVote.equals(lowestVote))
            newTransferSource.addElement(cand);
         else if (currentVote.lessThan(lowestVote)) {
            newTransferSource.removeAllElements();
            newTransferSource.addElement(cand);
            lowestVote = currentVote;
         }
      } // end of looping over 'transferSource'
      return newTransferSource;
   }
   
   private Candidate getContinuingCandidate() {
	   Candidate continuing = null;
	   Iterator candIter = currentContest.getCandidates().iterator();
	   while (continuing == null && candIter.hasNext()) {
		   Candidate cand = (Candidate) candIter.next();
		   if (cand.getStatus() == Candidate.CONTINUING)
			   continuing = cand;
	   }
	   return continuing;
   }
}
