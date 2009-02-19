
package engine;

import java.util.Vector;  

/** A fully public class; exists for structure. */
public class Rules
{
   // The following statics are used by more than one
   // rule.
   public static final int UNDEFINED      = 0;
   public static final int NONE           = 0;
   public static final int NEW_YORK       = 1;
   public static final int CAMBRIDGE      = 2;
   public static final int BUCKLIN        = 3; //not supported yet
   public static final int FRACTIONAL     = 4;
   public static final int RANDOM         = 5;
   public static final int IRELAND        = 6;
   public static final int BURLINGTON_IRV = 7;

   // Basic types of balloting supported
   // Also could be BUCKLIN
   public static final int STV          = 11;
   public static final int IRV          = 12;
   public static final int IR           = 13; //not supported yet
   public static final int PAIRWISE     = 14; //not supported yet
   public static final int WEIGHTED     = 15; //not supported yet
   public static final int MEASURE      = 16; //ballot measure
   public static final int SIMPLE_TALLY = 17;
   public int system = STV;

   // Types of thresholds
   // v = votes; s = seats
   // This can also be NONE, when using Bottoms-up, for example
   public static final int DROOP    = 11; // (v / (s + 1)) + 1, rounded down
   public static final int HARE     = 12; // v / s, rounded down
   ///public static final int DROOP_GT = 13; // not supported yet; Caleb K.'s > (v / s + 1)
   public int thresholdType         = DROOP;
   public int thresholdDecimals     = 0; // support for fractional thresholds

   // Transfer rules
   // NEW_YORK, CAMBRIDGE, IRELAND, FRACTIONAL
   public int surplusTransfer = FRACTIONAL;
   public boolean avoidExhaustedBallots = false;
   public boolean transferFromBegOfPile = true; // i.e. as a line (FIFO) ; if false, then a stack (LIFO)

   // More transfer rules.  When transferring to someone, and they meet
   // the threshold, do you keep giving them ballots?
   public static final int CONTINUE_FOR_THIS_ROUND = 11;  // the default
   public static final int IMMEDIATE_ELECT_AFTER_1ST_ROUND = 12; // Cambridge & NY
   public static final int IMMEDIATE_ELECT = 13; // not supported yet, is my simplified rule
   public static final int CONTINUE_TILL_2 = 14; // Burlington IRV
   public int onMeetingThreshold = CONTINUE_FOR_THIS_ROUND;

   // Simultaneous drops
   public int simultaneousDrop = NEW_YORK;
   public int cambridgeSimulDropLevel = 50; // if Cambridge-style simul drop is on, defaults to 50

   // Ties
   // Also could be RANDOM, BUCKLIN
   public static final int PREVIOUS_ROUND = 11;
   public static final int BY_ELECTION_OFFICIAL  = 12;
   public static final int ONE_PREVIOUS_ROUND = 13;
   public Vector tieBreakingRules = new Vector(); // initialized below

   // Duplicate rankings
   public static final int FULLY_LEGAL = 11; // CPPro default
   public static final int MUST_RESOLVE = 12; // NY rule
   public static final int SKIPPED = 13; // Cambridge rule
   public static final int EXHAUST_CONTINUING = 14; // Burlington IRV rule
   public static final int EXHAUST_ANY = 15; // Burlington Mod rule (not used?)
   public static final int NO_DUPLICATES = 16; // Invalidate ballot with duplicate rankings
   public int duplicateRankings = FULLY_LEGAL;

   // Misc.
   public boolean cambridgeVacancyRecount = false;
   public boolean distributedCount = false;
   public boolean doubleEntryVerification = false;
   public String textProperties = "text.properties";
   public int numberToElect = 1;

   // Used for plurality, x-voting, limited voting,
   // certain type of cumulative voting, etc.
   public int maxVotesAllowed = -1; // used in plurality tallies

   // Precision
   public static int DEFAULT_PRECISION = 5;
   public static int NON_FRACTIONAL_PRECISION = 0;
   public int reportPrecision = DEFAULT_PRECISION;

   /** Constructor
    */
   public Rules() {
      tieBreakingRules.addElement(new Integer(PREVIOUS_ROUND));
      tieBreakingRules.addElement(new Integer(BY_ELECTION_OFFICIAL));
      tieBreakingRules.addElement(new Integer(RANDOM));
   }

   /** Validate the rules for internal consistency */
   // TODO: add error messages and use this method! JL/SW 9/16/00
   public boolean areValid()
   {
      boolean valid = true;

      if (numberToElect < 1)
         valid = false;
      if (system == IRV && numberToElect != 1)
         valid = false;
      if (distributedCount && system != IRV)
         valid = false;
      if (distributedCount && IMMEDIATE_ELECT_AFTER_1ST_ROUND == onMeetingThreshold)
         valid = false;

      return valid;
   }

   /** Defines dump(), for debugging */
   public void dump()
   {
      StringBuffer buf = new StringBuffer();
      buf.append("Rules:");
      buf.append("\n\t Basic Type Of Rules (0==und., 11==STV, 12==IRV, 13==IR) = " + system);
      buf.append("\n\t Number to Elect = " + numberToElect);
      buf.append("\n\t Threshold Type (11=Droop, 12=Hare) = " + thresholdType);
      buf.append("\n\t Threshold Precision = " + thresholdDecimals);
      buf.append("\n\t Cambridge Vacancy Recount is " +
            (cambridgeVacancyRecount ? "ON" : "OFF"));
      buf.append("\n\t Double Entry Verification is " +
            (doubleEntryVerification ? "ON" : "OFF"));
      buf.append("\n\t Surplus Transfer Rule (1=NYC, 2=Camb., 4=Fract.) = " + surplusTransfer);
      buf.append("\n\t Imm. Elect? (11=no, 12=Yes after 1st) = " + onMeetingThreshold);
      buf.append("\n\t Transfers From Top of Pile? = " + transferFromBegOfPile);
      buf.append("\n\t Simul. Drop Rule (0=none, 1=NYC, 2=Camb.) = " + simultaneousDrop);
      buf.append("\n\t Camb. Drop Level = " + cambridgeSimulDropLevel);

      buf.append("\n\t Tie Breaks (Prev.=11, Official=12, Random=5, Bucklin=3) = ");
      for (int i=0; i < tieBreakingRules.size(); i++) {
         buf.append( (Integer)tieBreakingRules.elementAt(i) );
         if (i + 1 < tieBreakingRules.size())
            buf.append(", ");
      }

      System.out.println(buf);
   }

   public void useCambridgeRules()
   {
      system = STV;
      thresholdType = DROOP;
      thresholdDecimals = 0;
      surplusTransfer = CAMBRIDGE;
      avoidExhaustedBallots = true;
      onMeetingThreshold = IMMEDIATE_ELECT_AFTER_1ST_ROUND;
      transferFromBegOfPile = true;
      simultaneousDrop = CAMBRIDGE;
      cambridgeSimulDropLevel = 50;
/*
      tieBreakingRules = new Vector(); // initialized below
      tieBreakingRules.addElement(new Integer(PREVIOUS_ROUND_METHOD));
      tieBreakingRules.addElement(new Integer(BY_ELECTION_OFFICIAL));
*///FIXIT??
      // Misc.
      cambridgeVacancyRecount = true;
      doubleEntryVerification = false;
//      textProperties = "text_en_US_cambridge.properties"; // needs to be done in ChoicePlus.properties
      // public int numberToElect = 1; -- not set; must be set by input file

      duplicateRankings = SKIPPED;

      reportPrecision = NON_FRACTIONAL_PRECISION;
   }

   public void useIrelandRules()
   {
      system = STV;
      thresholdType = DROOP;
      thresholdDecimals = 0;

      surplusTransfer = CAMBRIDGE; // Later: implement IRELAND
      avoidExhaustedBallots = true;
      onMeetingThreshold = CONTINUE_FOR_THIS_ROUND;
      transferFromBegOfPile = false;
      simultaneousDrop = NEW_YORK;

      // Misc.
      cambridgeVacancyRecount = false;
      doubleEntryVerification = false;
//      textProperties = "text_en_US_Ireland.properties"; // needs to be done in ChoicePlus.properties

      duplicateRankings = SKIPPED;

      reportPrecision = NON_FRACTIONAL_PRECISION;
   }

   public void useNYCRules()
   {
      system = STV;
      thresholdType = DROOP;
      thresholdDecimals = 0;
      surplusTransfer = CAMBRIDGE;
      onMeetingThreshold = IMMEDIATE_ELECT_AFTER_1ST_ROUND;
      transferFromBegOfPile = true;
      simultaneousDrop = CAMBRIDGE;
      cambridgeSimulDropLevel = 50;

      // Misc.
      cambridgeVacancyRecount = false;
      doubleEntryVerification = false;
//      textProperties = "text_en_US_cambridge.properties"; // needs to be done in ChoicePlus.properties
      // public int numberToElect = 1; -- not set; must be set by input file

      duplicateRankings = SKIPPED;

      reportPrecision = NON_FRACTIONAL_PRECISION;
   }

   public void useBurlingtonIRVRules()
   {
      system = IRV;
      thresholdType = DROOP;
      thresholdDecimals = 0;
      onMeetingThreshold = CONTINUE_TILL_2;
      transferFromBegOfPile = true;
      simultaneousDrop = NEW_YORK; // mathematically inevitable...
      tieBreakingRules.removeAllElements();
      tieBreakingRules.addElement(new Integer(Rules.PREVIOUS_ROUND)); 
//      tieBreakingRules.addElement(new Integer(Rules.ONE_PREVIOUS_ROUND)); 
      tieBreakingRules.addElement(new Integer(Rules.BY_ELECTION_OFFICIAL)); 
//      tieBreakingRules.addElement(new Integer(Rules.RANDOM)); 

      // Misc.
      doubleEntryVerification = false;
//      textProperties = "text_en_US_Burlington.properties"; // needs to be done in ChoicePlus.properties

      duplicateRankings = EXHAUST_CONTINUING;

      reportPrecision = NON_FRACTIONAL_PRECISION;
   }

   
}


