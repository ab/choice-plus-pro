package engine;

import java.util.*;
import java.lang.NumberFormatException;
import util.*;
import gui.ICPProGUI;

public class Contest
{
   private Vector titleLines;      // Vector of Strings
   private Rules rules;
   private Hashtable candidatesAlphaHash;   // Hashtable of Candidates -- hashed by candidate abbreviation (alphanumeric)
   private Hashtable candidatesNumHash;   // Hashtable of Candidates -- hashed by candidate abbreviation (numeric, Integers)
   private Vector candidatesVect;
   private Vector ballots;         // Vector of all Ballots for this contest
   private Vector invalidBallots;
   private Results results;
   private Vector candidateAutotestResults; // expected AUTOTEST results
   private BCD threshold;
   private Candidate exhaustedPile; // Ballots in the "Exhausted" pile
   private BCD voteTotal;
   private String name = null;             // contest name
   private String importFile;       // importFile, if there was one
   private ICPProGUI gui;
   private boolean showStats = false;

   /** Standard constructor */
   public Contest(String importFile, ICPProGUI gui)
   {
      this.gui = gui;
      this.importFile = importFile;
      titleLines = new Vector();
      rules = new Rules();
      candidatesAlphaHash = new Hashtable();
      candidatesNumHash = new Hashtable();
      candidatesVect = new Vector();
      ballots = new Vector();
      invalidBallots = new Vector();
      results = new Results();
      candidateAutotestResults = new Vector();
      threshold = new BCD(0.0D);
      voteTotal = new BCD(0.0D);

      exhaustedPile = new Candidate(this);
      exhaustedPile.init(  LiteralString.gets("exhausted.pile.name"),
                           LiteralString.gets("exhausted.pile.abbr"),
                           0.0D, Candidate.SPECIAL,
                           0, this, Candidate.EXHAUSTED_PILE);
   }

   public Rules getRules()     { return rules; }
   public Vector getBallots()  { return ballots; }
   public Vector getInvalidBallots() { return invalidBallots; }
   public BCD getThreshold() { return threshold; }
   public Results getResults() { return results; }
   public int numberOfCandidates()  { return candidatesVect.size(); }
   public Vector getTitleLines() { return titleLines; }
   public String getName()       { return name; }
   public void setName(String str)  { this.name = new String(str); }
   public Candidate getExhaustedPile() { return exhaustedPile; }
   public BCD getVoteTotal()     { return voteTotal; }

   public void showStatistics()  { showStats = true; }
   public boolean areStatisticsOn() { return showStats; }


   public int numberOfBallots()
   {
      if (ballots == null)
         return 0;
      else return ballots.size();
   }
   public int numberOfInvalidBallots()         
   {
      if (invalidBallots == null)
         return 0;
      else return invalidBallots.size();
   }
   public String getReportTitle()
   {
      String title = null;
      if (name != null)
         title = name;
      else if (titleLines.size() > 0)
         title = (String)titleLines.elementAt(0);
      else if (importFile != null)
         title = importFile;
      else title = "";

      return title;
   }

   public void setThreshold(BCD threshold) { this.threshold = threshold; }

   /** Add a title line.
    */
   public void addTitleLine(String nextLine)
   {
      titleLines.addElement(nextLine);
   }

   /** Add a candidate */
   public void addCandidate(String abbreviation, String name, boolean isWriteIn)
   {
      // check if the candidate already exists
      Candidate cand = null;
      cand = (Candidate)candidatesAlphaHash.get(abbreviation);

      if (cand == null) { // Not a dup
         cand = new Candidate(name, abbreviation, this);
         candidatesAlphaHash.put(abbreviation, cand);
         candidatesVect.addElement(cand);
         try {
            candidatesNumHash.put( new Integer(abbreviation), cand );
         }
         catch (NumberFormatException e ) {} // don't put it in this case

         cand.setWriteIn(isWriteIn);
      }
      else {  // The new one is notIs a duplicate
         if (isWriteIn)
            if (cand.isWriteIn())
               ; //no error: both the new and old are write-ins
            else //the new is a write-in and the old is a regular candidate!
               gui.msgOK(
                  JSError.buildMsg(JSError.WARNING, 2003, abbreviation, null));
         else
            if (cand.isWriteIn()) // the new is a reg. cand; the original is a write-in
               gui.msgOK(
                  JSError.buildMsg(JSError.WARNING, 2004, abbreviation, null));
            else //both .CANDIDATE's (reegular)
               gui.msgOK(
                  JSError.buildMsg(JSError.WARNING, 2002, abbreviation, null));
      }
   }

   /** Add an autotest result
    *  The information is stored in a Candidate object.
    *  Then, at the end of the contest, we compare the actual
    *  candidate results against the autotest results to make
    *  sure they are the same.
    */
   public void addAutotestResult(String abbr, String voteTotal, int finalStatus, int roundStatusDetermined)
      throws DataFormatException
   {
      int type = Candidate.NORMAL;
      Candidate newCand = new Candidate(this);
      Candidate cand = (Candidate)candidatesAlphaHash.get(abbr);
      if ( cand != null ||
           (LiteralString.gets("exhausted.pile.abbr").equals(abbr)) )
      {
         type = Candidate.EXHAUSTED_PILE;
         candidateAutotestResults.addElement(newCand);
      }
      else
         throw new DataFormatException(LiteralString.gets("exc.badCandidateAbbr", abbr));
      newCand.init("(autotest candidate)", abbr, voteTotal,
                          finalStatus, roundStatusDetermined, this,type);
   }

   /** Add a ballot to this contest. Don't assign to a candidate yet. */
   public void addValidBallot(Ballot ballot)
   {
      ballots.addElement(ballot);
      voteTotal = voteTotal.add(ballot.getOrigValue());
   }

   /** Add an invalid ballot to this contest. */
   public void addInvalidBallot(Ballot ballot)
   {
      invalidBallots.addElement(ballot);
   }

   /** Add an exhausted ballot */
/*
   public void addExhaustedBallot(Ballot ballot)
   {
      exhaustedPile.addBallot(ballot);
   }
*/
   /** Get a candidate, given an abbreviation */
   public Candidate getCandidate(String abbreviation, boolean isAlpha)
   {
   	if (isAlpha)
	      return (Candidate)candidatesAlphaHash.get(abbreviation);
      else try {
      	Integer i = new Integer(abbreviation);
      	return (Candidate)candidatesNumHash.get(i);
      }
      catch (NumberFormatException e) {
      	return null;
      }
   }

   /** Get the candidates as a Vector */
   public Vector getCandidates()
   {
      return candidatesVect;
   }

   /** For debugging; dumps the contest */
   public void dump()
   {
      dumpDataSet("Title Lines:", titleLines.elements());
      rules.dump();
      ///dumpDataSet("Autotest Expected Results:", candidateAutotestResults.elements());
      System.out.println("Autotest Expected Results:");
      Enumeration elements = candidateAutotestResults.elements();
      while (elements.hasMoreElements())
         System.out.println("\t" + ( ((Candidate)elements.nextElement())).dump());
      ///System.out.println("\t" + exhaustedPile.dump());

      ///dumpDataSet("Candidates (& actual results):", candidatesVect.elements());
      System.out.println("Candidates (& actual results):");
      elements = candidatesVect.elements();
      while (elements.hasMoreElements())
         System.out.println("\t" + ( ((Candidate)elements.nextElement())).dump());
      System.out.println("\t" + exhaustedPile.dump());

      // Don't actually print the ballots; it isn't worth it.
      System.out.println(ballots.size() + " ballot(s) processed");
   }

   // For debugging; dumps a Vector or Hashtable
   private void dumpDataSet(String header, Enumeration elements)
   {
      System.out.println(header);
      while (elements.hasMoreElements())
         System.out.println("\t" + (elements.nextElement()).toString());
   }

   /** Compare expected against actual results */
   // The expected results are in the form of an array
   // of pseudo candidates.  The actual results are the
   // last element in the array in the Results object.  But
   // we can also use the current array of candidates to get
   // the final results, and indeed that *is* what we use.
   public void compareAutotestResults(String fileName)
   {
      boolean success = true;
      Vector expected = candidateAutotestResults;
      Hashtable actual = candidatesAlphaHash;
      // Loop over the expected array, check against the
      // actual.
      for (int i = 0; i < expected.size() && success; i++) {
         Candidate expCand = (Candidate)expected.elementAt(i);
         Candidate actCand = (Candidate)actual.get(expCand.getAbbr());
         if (actCand == null && expCand.getType() == Candidate.EXHAUSTED_PILE)
            actCand = exhaustedPile;
         if (!expCand.equalsAutotest(actCand)) {
            success = false;
// SCW 7/16/00 - added report of where failure occurred
            System.out.println("Autotest failed on candidate "+actCand.getName());
         }
      }

      String buf = null;
      String passFail = LiteralString.gets(success ? "autotest.passed" : "autotest.failed");
//      Object[] params = new Object[]{ fileName,passFail };
//      buf = LiteralString.getString("autotest.mainline", params);
      buf = LiteralString.gets("autotest.mainline",
            new Object[]{ fileName,passFail } );
      System.out.println(buf);
      if (success == false)
         dump();
   }

   public int getNumberOfContinuingCandidates()
   {
      int count = 0;
      for (int i = 0; i < candidatesVect.size(); i++) {
         Candidate cand = (Candidate)candidatesVect.elementAt(i);
         if (cand.getStatus() == Candidate.CONTINUING)
            count++;
      }

      return count;
   }

   public int getNumberOfUndefeatableCandidates()
   // SCW 7/11/00 add method
   {
      int count = 0;
      for (int i = 0; i < candidatesVect.size(); i++) {
         Candidate cand = (Candidate)candidatesVect.elementAt(i);
         if (cand.getStatus() == Candidate.UNDEFEATABLE)
            count++;
      }

      return count;
   }
}
