
package engine;

import java.io.*;
import java.lang.NumberFormatException;
import java.util.Stack;
import java.util.Vector;

import util.*;
import reporter.ICPProReporter;

/**
 * Loads the data
 */
public class DataLoader
{
   private static boolean DEBUG = true;
   private static void DMSG(String s) {if (DEBUG) System.out.println(s);}

//   private static final int NOT = -1;
   private static final char COMMENT_CHAR = '#';
   private static final char COMMAND_CHAR = '.';

   private static final int ERR_UNRECOGNIZED_COMMAND = 2005;

   private Vector ballotFormatFields = null;    // vector of ints
   private String extraSeparators = null; // That is, other than whitespace
   private Vector ignoreCommands = null; // vector of Strings; commands to ignore

   private JSBufferedReader currInputFile = null;
   private Stack inputFileStack = new Stack();  // stack of input files

//   private Object loadedObject = null; // Vector of filenames, or Contest
//   private int tokenNumber = 0;
   private Contest contest = null;
   private Rules rules = null;
   private Vector autotestFiles = null;
//   private boolean doAutotest = false;
   private boolean candidateIdsAreAlpha = true;

   private String currLine = null;
   private int currTokenIndex = 0;
   private String currToken = null;
   private int ballotCount = 0;

   // Cambridge recount support
   private String finalPileCandAbbr = null;
   private static final int NOT_RECOUNT = 0;
   private static final int WRONG_PILE = 1;
   private static final int RIGHT_PILE = 2;
   private int cambridgeRecountState = NOT_RECOUNT;

   // other
   ICPProReporter reporter = null;

   /**
    * Create a new DataLoader object, given a file-name.
    */
   public DataLoader(String importFile, ICPProReporter reporter)
      throws FileNotFoundException
   {
      currInputFile = new JSBufferedReader(importFile);
      contest = new Contest(importFile, Engine.gui);
      rules = contest.getRules();
      autotestFiles = new Vector();
      extraSeparators = ",);";   // whitespace is always a separator
      ignoreCommands = new Vector();

      finalPileCandAbbr = null;
      cambridgeRecountState = NOT_RECOUNT;

      // set default ballot format fields
      ballotFormatFields = new Vector();
      ballotFormatFields.addElement(new Integer(StringIds.MOD_BAL_ID_ALPHA)); // 1st ballot id alphanumeric
      ballotFormatFields.addElement(new Integer(StringIds.MOD_RANKINGS_ALPHA));

      // other
      this.reporter = reporter;
   }

   //==========================================================================
   /**
    * Import a contest (or Vector) in CPPro format
    */
   public Object load()
   {
      DMSG("DataLoader.load() called");

      try
      {
         currLine = readLine();
         currTokenIndex = 0;
         while (currLine != null) {
            try {
               currLine.trim();
               if (currLine.length() > 0) {
                  char firstChar = currLine.charAt(0);
                  switch (firstChar) {
                     case COMMENT_CHAR: // '#'
                        // comment line; skip it
                        ignoreLine();
                        break;
                     case COMMAND_CHAR: // period, '.'
                        // command line, parse the command
                        parseCommand();
                        break;
                     default:
                        // ballot line
                        if (cambridgeRecountState == WRONG_PILE)
                           ignoreLine();
                        else if (!Character.isLetterOrDigit(firstChar))
                           ignoreLine();
                        else {
                           parseBallot();
                           Engine.gui.ballotLoaded(ballotCount++, false);
                        }

                        break;
                  } // end switch
               }  // end if
               else ignoreLine(); // blank line
               currLine = readLine();
               currTokenIndex = 0;
            }
            catch (DataFormatException e) {
//               System.out.println("Data Format Exception!");
//               System.out.println(e);
//               Thread.dumpStack();
               Engine.gui.msgOK(
                  JSError.buildMsg( JSError.FATAL,
                     StringIds.DATA_FORMAT_EXCEPTION,
                     new Object[] {
                        e.getMessage(),
                        currInputFile.getFileName()
                     } ) );
               System.exit(-1);
               //todo
            }
         } // end while
         Engine.gui.ballotLoaded(ballotCount, true);
      }  // end try
      catch (IOException e)
      {
         if (e.getMessage() == null)
            Engine.gui.msgOK(
               JSError.buildMsg(JSError.FATAL, StringIds.FILE_NOT_FOUND,
                           currInputFile.getFileName()) );
         else
            Engine.gui.msgOK(
               JSError.buildMsg(JSError.FATAL, e.getMessage()) );
      }

      if (autotestFiles != null && autotestFiles.size() > 0)
         return autotestFiles;
      else return contest;
   } // end load();

   //read a line
   private String readLine()
      throws IOException
   {
      StringBuffer buf = null;

      String curr = currInputFile.readLine();
      if (curr == null)
         currInputFile.close(); // Fixes bug #133, I hope
      while (curr == null && !inputFileStack.empty()) {
         currInputFile = (JSBufferedReader)inputFileStack.pop();
         curr = currInputFile.readLine();
         if (curr == null)
            currInputFile.close(); // Fixes bug #133, I hope
      }

      if (curr != null)
      {
         if ( !Library.isEmpty(curr) &&
              curr.charAt(curr.length()-1) == '\\')
         {
            buf = new StringBuffer(curr);
            do {
               buf.setLength(buf.length()-1); // remove the backslash
               buf.append(currInputFile.readLine());
            } while (buf.charAt(buf.length()-1) == '\\');
            curr = buf.toString();
         }
      }
      return curr;
   }

   //==========================================================================
   // Parse a CPPro command.
   private void parseCommand()
      throws DataFormatException, FileNotFoundException
   {
	   int command = getCommand(); 
      switch (command)
      // keep this list alphabetized
      {
         case StringIds.COM_AUTOTEST:            addAutotest(); break;
         case StringIds.COM_AUTOTEST_RESULTS:    setAutotestResults(); break;
         case StringIds.COM_BALFORM_FIELDS:      setBallotFormatFields(); break;
         case StringIds.COM_BALFORM_SEPS:        setBallotFormatSeparators(); break;
         case StringIds.COM_CAMB_VAC_RECOUNT:    setupCambridgeVacancyRecount(); break;
         case StringIds.COM_CANDIDATE:           addCandidate(false); break;
         case StringIds.COM_COMPLY_WITH:         setComplianceRules(); break;
         case StringIds.COM_CONTEST:             setName(); break;
         case StringIds.COM_DBL_ENTRY_VER:       rules.doubleEntryVerification = true; break;
         case StringIds.COM_DISTRIBUTED_COUNT:   rules.distributedCount = true; break;
         case StringIds.COM_ELECT:               setNumberToElect(); break;
///      case StringIds.COM_ELIMINATE:           setEliminationRules(); break;
         case StringIds.COM_EXCLUDE_CAND:        excludeCandidate(); break;
         case StringIds.COM_FINAL_PILE:          finalPileListStarting(); break;
         case StringIds.COM_IGNORE_COMMAND:      ignoreLine(); break;
         case StringIds.COM_IGNORE_COMMANDS:     setIgnoreCommands(); break;
         case StringIds.COM_INCLUDE:             includeFile(); break;
         case StringIds.COM_INITDROP:            setInitialDropRules(); break;
         case StringIds.COM_NON_CAND:            ignoreLine(); break;  // used by Steve W., not by CPPro
         case StringIds.COM_OFFICE:              ignoreLine(); break;  // used by Steve W., not by CPPro
//       case COM_SORT_BALLOTS:        setSortingOptions(); break;
         case StringIds.COM_SIMULTANEOUS_DROP:   setSimultaneousDrop(); break;
         case StringIds.COM_SPECIAL:             ignoreLine(); break;  // used by Steve W.??
         case StringIds.COM_START_VERIF:         startVerifyingHere(); break;
         case StringIds.COM_STATISTICS:          contest.showStatistics(); break;
         case StringIds.COM_SURPLUS:             setSurplusRules(); break;
         case StringIds.COM_SYSTEM:              setSystem(); break;
         case StringIds.COM_THRESHOLD:           setThresholdRules(); break;
         case StringIds.COM_TIES:                setTiebreakingRules(); break;
         case StringIds.COM_TITLE:               addTitleLine(); break;
         case StringIds.COM_TRANSFER:            setTransferRules(); break;
         case StringIds.COM_UNDEFEATABLE:        undefeatableCandidate(); break;
         case StringIds.COM_VALID:               setValidBallotRules(); break;
         case StringIds.COM_WRITE_IN:            addWriteIn(); break;
         default:
            String msgText = JSError.buildMsg(JSError.WARNING,
                     ERR_UNRECOGNIZED_COMMAND,
                     new Object[] {
                        currLine + command,
                        new Integer(currInputFile.getLineCount()),
                        currInputFile.getFileName()
                     } );
            Engine.gui.msgOK(msgText);
      }
   }

   //==========================================================================
   // Return the string number if the token is a CPPro command,
   // else returns NOT.
   private int getCommand()
   {
      int command = getTranslatedToken();
      if (command == StringIds.NULL_TOKEN) {
         if (ignoreCommands.contains(currToken))
            command = StringIds.COM_IGNORE_COMMAND;
      }
      return command;
   }

   private int getTranslatedToken()
   {
      return LiteralString.get( getNextToken() );
   }

/*
 //   { MOD_ALLBALLOTS, "ALL-BALLOTS" },            //.SURPLUS FRACTIONAL ALL-BALLOTS
   { MOD_ASCENDING,  "ASCENDING" },              //.SORT {field-name} ASCENDING
   { MOD_AVOID_EXH,  "AVOID-EXHAUSTED-BALLOTS" },//.SURPLUS ...
   { MOD_BAL_ID_A,      "BALLOT-ID-ALPHA" },        //.BALLOT-FORMAT-FIELDS or .SORT
   { MOD_BAL_ID_N,      "BALLOT-ID-NUMERIC" },     //          "
   { MOD_BAL_TOP_A,  "BALLOT-TOP-ALPHA" },       //         "
   { MOD_BAL_VALUE,  "BALLOT-VALUE" },          //          "
// { MOD_BUCKLIN,    "BUCKLIN" },            //.TIES BUCKLIN
   { MOD_CAMBRIDGE,  "CAMBRIDGE" },          //.COMPLY-WITH CAMBRIDGE
   { MOD_CANDIDS_NR,    "CAND-IDS-NUM-RANKED" },   //.BALLOT-FORMAT-FIELDS ...
   { MOD_CANDIDS_NR,    "RANKINGS-NUMERIC" },      //.BALLOT-FORMAT-FIELDS ...
   { MOD_CANDIDS_AVB,  "RANKINGS-ALPHA" },
   { MOD_CANDNUM,    "CANDIDATE#" },
   { MOD_DROOP,      "DROOP" },              //.THRESHOLD DROOP
   { MOD_DUP,        "DUPLICATES" },             //.VALID-BALLOTS DUPLICATES
   { MOD_EVERY_NTH_CAMBRIDGE,  "EVERY-NTH-CAMBRIDGE" },  //.SURPLUS EVERY-NTH
   { MOD_FRACT,      "FRACTIONAL" },
   { MOD_HARE,       "HARE" },               //.THRESHOLD HARE & ".ELIMINATE HARE"
   { MOD_HUMAN,      "BY-HUMAN" },           //.TIES BY-HUMAN
   { MOD_IGNORE_FIELD,  "IGNORE-FIELD" },          //.BALLOT-FORMAT-FIELDS IGNORE-FIELD ...
// { MOD_LASTBATCH,  "LAST-BATCH" },             //.SURPLUS FRACTIONAL LAST-BATCH
   { MOD_LESSTHAN,      "LESS-THAN" },          //.INITIAL-DROP LESS-THAN 50
   { MOD_NEW_YORK_CITY,"NEW-YORK-CITY" },       //.COMPLY-WITH NEW-YORK-CITY
   { MOD_NODUP,      "NO-DUPLICATES" },          //.VALID-BALLOTS NO-DUPLICATES
   { MOD_OFF,        "OFF" },                    //.DBL_ENTRY_VER
   { MOD_ON,         "ON"  },             //.DBL_ENTRY_VER
   { MOD_PREC_NUM_A, "PRECINCT-NUM-ALPHA" },     //.BALLOT-FORMAT-FIELDS PRECINCT-NUM...
   { MOD_PREVRND,    "PREVIOUS-ROUND" },        //.TIES PREVIOUS-ROUND
   { MOD_RAND,       "RANDOM" },
   { MOD_RANDOM_COMP,   "BY-LOT-COMPUTER" },       //.TIES BY-LOT-COMPUTER
   { MOD_SIMULXFERWINNERS, "SIMUL-XFER-WINNERS" }, //.SURPLUS FRACTIONAL SIMUL-XFER-WINNERS
   { MOD_SIMULXFERWINNERSNOT, "DON'T-SIMUL-XFER-WINNERS" }, //.SURPLUS FRACTIONAL SIMUL-XFER-WINNERS
   { MOD_SORT_FIELD, "SORT-ORDER" },            //.BALLOT-FORMAT-FIELDS... [NYC]
   { MOD_STOP_THRESH,   "STOP-ON-THRESHOLD" },     //.SURPLUS ...
*/
   //==========================================================================
   /**
    *    Set ballot format fields.
    * BALLOT-VALUE
    * BALLOT-ID-ALPHA
    * IGNORE-FIELD
    * PRECINCT-NUM-ALPHA
    * BALLOT-ID-NUMERIC
    * BALLOT-TOP-ALPHA
    * CAND-IDS-NUM-RANKED
    * RANKINGS-ALPHA
    */
   private void setBallotFormatFields()
   {
      ballotFormatFields = new Vector();
      
      int modifier = getTranslatedToken();
      while (modifier != StringIds.NULL_TOKEN)
      {
         ballotFormatFields.addElement(new Integer(modifier));
         modifier = getTranslatedToken();
      }
   }

   //==========================================================================
   // Set ballot format field separators.
   // E.g.: ".BALLOT-FORMAT-SEPS ,)"
   private void setBallotFormatSeparators()
   {
      extraSeparators = null;
      extraSeparators = getNextToken();
   }

   //==========================================================================
   // A new write-in candidate has been added.
   // E.g.: ' .WRITE-IN W01,"Joe Smith" '
   private void addWriteIn()
   {
      addCandidate(true);
   }

   //==========================================================================
   // A new candidate has been added.
   // E.g.: " .CANDIDATE 1,"AAA" "
   private void addCandidate(boolean isWriteIn)
   {
      String candAbbreviation = getNextToken();
      String candName = getNextToken();
      contest.addCandidate(candAbbreviation, candName, isWriteIn);
   }

   //==========================================================================
   // Set compliance with a certain set of rules.
   // E.g.: ".COMPLY-WITH CAMBRIDGE"
// When the input is set to ".COMPLY-WITH CAMBRIDGE" this function is called, to
// specify the Cambridge rules.
   private void setComplianceRules()
   {
      int modifier = getTranslatedToken();
      if (modifier == StringIds.MOD_CAMBRIDGE)
         rules.useCambridgeRules();
      else if (modifier == StringIds.MOD_IRELAND)
         rules.useIrelandRules();
      else if (modifier == StringIds.MOD_BURLINGTON_IRV)
         rules.useBurlingtonIRVRules();
      else if (modifier == StringIds.MOD_NEW_YORK_CITY)
         rules.useNYCRules();
   }

   //==========================================================================
   // Set the number of candidates to elect
   // E.g.: ".ELECT 1"
   private void setNumberToElect()
   {
      rules.numberToElect = getNextTokenAsInt();
      if (rules.numberToElect == 1 && rules.system == Rules.STV)
         rules.system = Rules.IRV;
   }

   //==========================================================================
   // Exclude a candidate.
   // E.g.: ".EXCLUDE-CANDIDATE JRL"
   private void excludeCandidate()
   {
      String candAbbr = getNextToken();
      Candidate cand = contest.getCandidate(candAbbr, candidateIdsAreAlpha);
      cand.setStatus(Candidate.EXCLUDED, 0);
   }

   //==========================================================================
   // Make a candidate undefeatable.
   // E.g.: ".UNDEFEATABLE JRL"
   // SCW 7/11/00 - added this method
   private void undefeatableCandidate()
   {
      String candAbbr = getNextToken();
      Candidate cand = contest.getCandidate(candAbbr, candidateIdsAreAlpha);
      cand.setStatus(Candidate.UNDEFEATABLE, 0);
   }

   //==========================================================================
   // Include a file, on the spot.
   // E.g.: ".INCLUDE prec10403.bal"
   private void includeFile()
      throws FileNotFoundException
   {
      String newFileName = getNextToken();
      inputFileStack.push(currInputFile);
      try {
         // In case the file is not in the default directory,
         // prepend the last path to the file name.
         File newfile = new File(newFileName);
         if (!newfile.isAbsolute()) {
            File currfile = new File(currInputFile.getFileName());
            String path = currfile.getParent();
            if (path != null)
               newFileName = path + File.separatorChar + newFileName;
         }

         // Now open the new file
         reporter.setOutputDirectory(Library.getFullDirectory(newFileName));
         currInputFile = new JSBufferedReader(newFileName);
      }
      catch (FileNotFoundException e) {
         String msg = null;
         msg = JSError.buildMsg( JSError.FATAL, StringIds.FILE_NOT_FOUND,
                           newFileName, null );
         e.fillInStackTrace();
         throw new FileNotFoundException(msg);
      }
   }

   //==========================================================================
   // Set the rules for the initial, early drop of candidates.
   // E.g.: ".INITIAL-DROP LESS-THAN 50" -- complies with Cambridge law.
   // Default: NY, mathmatical, rule
   private void setInitialDropRules()
   {
      System.out.println("DUMMY called: setInitialDropRules()");
   }

   //==========================================================================
   // Ignore the line, the command is not a CPPro command.
   private void ignoreLine()
   {
      // For now, anyway, intentionally do nothing.
   }

   //==========================================================================
   // A command that works with double-entry verification; tells
   // the double entry verifier to begin verifying at this point.
   private void startVerifyingHere()
   {
      System.out.println("DUMMY called: startVerifyingHere()");
   }

   //==========================================================================
   // Set the rules for surplus transferring.
   // E.g.: ".SURPLUS RANDOM EVERY-NTH-CAMBRIDGE STOP-ON-THRESHOLD
   //    AVOID-EXHAUSTED-BALLOTS" is the command to comply with Cambridge law.
   // E.g.: ".SURPLUS FRACTIONAL ALL-BALLOTS" is the CPPro default, and complies with the rules of the UC Berkeley Student Senate.
   // E.g.: ".SURPLUS FRACTIONAL LAST-BATCH" complies with the rules of Tasmania, Australia.
   private void setSurplusRules()
   {
      System.out.println("DUMMY called: setSurplusRules()");
   }

   //==========================================================================
   // Add an automated test to the list of tests to run.
   // E.g. ".AUTOTEST foo.pim"
   private void addAutotest()
   {
      autotestFiles.addElement( getNextToken() );
   }

   //==========================================================================
   // Add a line to the list of title lines.
   private void addTitleLine()
   {
      contest.addTitleLine( getRestOfLine() );
   }

   //==========================================================================
   // Set the rules for what a valid ballot is.
   private void setValidBallotRules() {
	   int type = getTranslatedToken();
	   switch (type) {
	   		case StringIds.MOD_NODUP: rules.duplicateRankings = Rules.NO_DUPLICATES; break;
	   		default:
	   			System.out.println("setValidBallotRules() called with type " + type);
	   }
   }

   //==========================================================================
   // Return the next token in the currentLine -- as an int
   private int getNextTokenAsInt()
   {
      String token = getNextToken();
      int returnInt = 0;

      try {
         returnInt = Integer.parseInt(token);
      }
      catch (NumberFormatException e) {
         returnInt = 0;
      }

      return returnInt;
   }

   //==========================================================================
   // Return the next token in the currentLine -- as a BCD
   private BCD getNextTokenAsBCD()
   {
      String token = getNextToken();
      BCD bcd = new BCD(token);
      return bcd;
   }

   //==========================================================================
   // Return the next token in the currentLine -- as a BCD
/*  not used
    private boolean getNextTokenAsBoolean()
      throws DataFormatException
   {
      boolean b = true;

      int type = getTranslatedToken();
      switch (type) {
         case StringIds.MOD_ON:  b = true;  break;
         case StringIds.MOD_OFF: b = false; break;
         default:
            throw new DataFormatException(currLine);
      }
      
      return b;
   }
*/
   
   
   //==========================================================================
   // Return the next token in the currentLine
   private String getNextToken()
   {
      char c = ' ';
      StringBuffer token = new StringBuffer();
      boolean quoting = false;
      boolean done = false;

      int i = currTokenIndex;
      if (i >= currLine.length())
         done = true;
      else {
         c = currLine.charAt(i);
         // skip any whitespace
         while (Character.isWhitespace(c) && i < currLine.length())
            c = currLine.charAt(++i);
      }

      // add chars to the token until a separator is hit
      while (!done) {
         if (isSeparator(c) && !quoting)
            done = true;
         else {
            if (c == '\"')
               // Quotation code.  Not terribly robust, but will
               // do, I think.
               if (!quoting)
                  quoting = true;
               else done = true;
            else token.append(c);
         }
         if (i < currLine.length() - 1)
            c = currLine.charAt(++i);
         else {
            done = true; // hit the end of the line
            i++;
         }
      }

      // now we have the token
      currTokenIndex = i;
      currToken = token.toString();
//      System.out.println(currToken);
      return currToken;
   }

   //==========================================================================
   // Return true if the char is a CPPro separator
   private boolean isSeparator(char c)
   {
      return (Character.isWhitespace(c) ||
               (extraSeparators != null && extraSeparators.indexOf(c) != -1));
   }

   //==========================================================================
   // Parse a CPPro ballot.
   // Cambridge '97: .BALLOT-FORMAT-FIELDS BALLOT-ID-ALPHA BALLOT-TOP-ALPHA IGNORE-FIELD IGNORE-FIELD RANKINGS-ALPHA
   private void parseBallot()
      throws DataFormatException
   {
      ///DMSG("parseBallot(): currLine == \"" + currLine + "\"");
      boolean hasRankings = true;
      Ballot ballot = new Ballot();
      String ballotId = null;

      try {
         for (int fieldNdx = 0; fieldNdx < ballotFormatFields.size(); fieldNdx++) {
            int commandNumber = ((Integer)ballotFormatFields.elementAt(fieldNdx)).intValue();
            switch (commandNumber) {
               case StringIds.MOD_BAL_ID_ALPHA:
                  ballotId = getNextToken();
                  ballot.setBallotId(ballotId);
                  break;
               case StringIds.MOD_BAL_ID_N: // ballot id numeric
                  ballotId = getNextToken();
                  ballot.setBallotIdNumeric(ballotId);
                  break;
               case StringIds.MOD_CANDIDS_NR:// ranked numeric candidate ids
                  candidateIdsAreAlpha = false;
                  hasRankings = setBallotRankings(ballot);
                  break;
               case StringIds.MOD_RANKINGS_ALPHA:
                  candidateIdsAreAlpha = true;
                  hasRankings = setBallotRankings(ballot);
                  break;
               case StringIds.MOD_BAL_VALUE:
                  ballot.setOrigValue(getNextTokenAsBCD());
                  break;
               case StringIds.MOD_IGNORE_FIELD:
               case StringIds.MOD_PREC_NUM_A:
               case StringIds.MOD_BAL_TOP_A:
               case StringIds.MOD_SORT_FIELD:
                  getNextToken(); // just eat it, and thus ignore it
                  break; // ignore the field
               default:
                  ///if (Mode.developing())
                  throw new FatalDeveloperError("Have not implemented command #" + commandNumber);
                  ///break;
            } //end switch
         } //end for

         // TODO: move this logic into Engine
         // Deciding whether or not a ballot is valid
         boolean valid = true;
         if (hasRankings) {
            if (cambridgeRecountState != NOT_RECOUNT)
               valid = true;  // If we are in a Cambridge recount, the ballot
                              // is always valid.
            else {            // regular election
               if (rules.duplicateRankings == Rules.SKIPPED) {
                  Vector dest = ballot.getCandsToXferTo(rules, 0);
                  if (rules.system == Rules.SIMPLE_TALLY ||
                      rules.system == Rules.MEASURE)
                  {
                     if (dest.size() == 0 || dest.size() > rules.maxVotesAllowed)
                        valid = false;
                  } else if (dest.size() == 0)
                     valid = false;
               } else if (rules.duplicateRankings == Rules.NO_DUPLICATES) {
                   valid = ballot.hasNoDuplicates();
               } else {  //rules.duplicateRankings == Rules.FULLY_LEGAL
                  Vector rankings = ballot.getRankings();
                  valid = rankings.size() > 0;
               }
            }
         } else 
        	 valid = false;
         if (valid)
            contest.addValidBallot(ballot);
         else {
            contest.addInvalidBallot(ballot);
            if (DEBUG) System.out.println("INVALID: " + ballot);
         }
      } catch (DataFormatException e) {
         //error.1007=Could not process ballot "{0}" (line #{1}, file "{2}").
         if (Mode.developing())
            e.printStackTrace();
         Engine.gui.msgOK(
               JSError.buildMsg(JSError.FATAL, 1007,
                  new Object[] { ballot.getIdAlpha(),
                              new Integer(currInputFile.getLineCount()),
                              currInputFile.getFileName() }) );
      }

   } // end parseBallot()

   // states getting information
   private static int STARTING = 0;
   private static int GETTING_ABBR = 1;
   private static int GOT_ABBR = 2;
   private static int GETTING_RANK = 3;
   private static int GOT_RANK = 4;
   private static int GOT_ALL_INFO = 5;
//   private static int DONE = 6;

   /** Sets the ballots rankings, expecting sorted rankings
    *  with alphanumeric candidate codes.  E.g.:
    *    "...AAA, BBB = CCC"
    *    or "... JRL [1], MBK [2] = PST [2]" where the actual voter's ranking is in brackets
    *  Note that an '=' sign means the vote is a duplicate ranking.
    *  @parameter alphaRanking -- True if alphanumeric, false if numeric only
    *  @return 'true' if the ballot has rankings, i.e., is not empty
    */
   private boolean setBallotRankings(Ballot ballot)
      throws DataFormatException
   {
      //boolean inBrackets = false;
      StringBuffer candAbbr = new StringBuffer();
      StringBuffer candRank = new StringBuffer();
      boolean isDup = false;
      boolean tokenEnds = true;
      boolean hasRankings = false;

      int state = STARTING;
      for (int i = currTokenIndex; i < currLine.length(); i++) {
         isDup = false;
         char c = currLine.charAt(i);
         if (Character.isWhitespace(c)) {
            tokenEnds = true;
            if (c == '\n')
               state = GOT_ALL_INFO;
         } else if (Character.isLetterOrDigit(c)) {
            if (state == STARTING || state == GETTING_ABBR) {
               candAbbr.append(c);
               state = GETTING_ABBR;
            } else if (state == GETTING_RANK)
               candRank.append(c);
            else 
            	throw new DataFormatException();
            tokenEnds = false;
            hasRankings = true;
         } else if (c == '=') {
            state = GOT_ALL_INFO;
            isDup = true;
         } else if (c == '[') {
            // should follow candidate abbr.
            if (state != GETTING_RANK) {
               state = GETTING_RANK;
               ///tokenEnds = true;
               if (state == STARTING)
                  state = GOT_ABBR;
            } else throw new DataFormatException();
         } else if (c == ']') {
            if (state == GETTING_RANK)
               state = GOT_RANK;
            else throw new DataFormatException();

         } else if (c == ',') {
            // we are done!
            state = GOT_ALL_INFO;
            tokenEnds = true;
         } else throw new DataFormatException();

         if (tokenEnds) {
            if (state == GETTING_ABBR)
               state = GOT_ABBR;
            else if (state == GETTING_RANK)
               state = GOT_RANK;
         }

         if (state == GOT_ALL_INFO && hasRankings) {
            gotAllInfo(ballot, candAbbr, candRank, isDup);
            state = STARTING;
         }
      } // end for() loop

      // At the end of the line we must have all of the info for the last cand, too
      if (hasRankings)
         gotAllInfo(ballot, candAbbr, candRank, isDup);

      return hasRankings;
   } // end setBallotRankingsAlpha()

   private void gotAllInfo(Ballot ballot, StringBuffer candAbbr, StringBuffer candRank, boolean isDup)
   {
      Candidate cand = contest.getCandidate(candAbbr.toString(), candidateIdsAreAlpha);
      int rank = 0;
      try {
         rank = Integer.parseInt(candRank.toString());
      }
      catch (NumberFormatException e) {
         // do nothing -- let the rank equal 0
      }
      ballot.addRanking(cand, isDup, rank);
      candAbbr.setLength(0);
      candRank.setLength(0);
   }

   //==========================================================================
   // Set the results for an automated test.
   // E.g. ".RESULTS AA,3,ELECTED,2; BB,2,DEFEATED,2; CC,0,DEFEATED,1"
   //    I.e. AA came in first, with 3 votes, was ELECTED in the 2nd round
   //         BB came in 2nd, with 2 votes, was DEFEATED in the 2nd round
   //         etc.
   private void setAutotestResults()
      throws DataFormatException
   {
      String abbr = null;
      String votes = "0.0";
      int status = 0, round = 0;
//      Vector candFinalResults = new Vector();
      boolean done = false;

      while (!done)
      {
         abbr = getNextToken();
         if (Library.isEmpty(abbr)) //reached the end of the line
            done = true;
         else {
            votes = getNextToken(); //string, will be used as BCD
            status = getTranslatedToken();
            round = getNextTokenAsInt();
            contest.addAutotestResult(abbr, votes, status, round);
         }
      }
   }

   //==========================================================================
   // A name has been supplied for the contest
   // E.g.: " .CONTEST "Mayor"
   private void setName()
   {
      String contestName = getNextToken();
      contest.setName(contestName);
   }

   // The file says ".SYSTEM STV", e.g.
   private void setSystem()
      throws DataFormatException
   {
      int sys = getTranslatedToken();
      switch (sys) {
         case StringIds.MOD_STV:
            rules.system = Rules.STV;
            break;
         case StringIds.MOD_IRV:
            rules.system = Rules.IRV;
            break;
         case StringIds.MOD_IR:
            rules.system = Rules.IR;
            break;
         case StringIds.MOD_BALLOT_MEASURE:
            rules.system = Rules.MEASURE;
            rules.maxVotesAllowed = 1;
            break;
         default:
            throw new DataFormatException(currLine);
      }

   }

   //==========================================================================
   // Set the rules for how to calculate the threshold.
   // E.g. ".THRESHOLD HARE"
   private void setThresholdRules()
      throws DataFormatException
   {
      int type = getTranslatedToken();
      switch (type) {
         case StringIds.MOD_HARE: rules.thresholdType = Rules.HARE; break;
         case StringIds.MOD_DROOP: rules.thresholdType = Rules.DROOP; break;
         default:
            throw new DataFormatException(currLine);
      }
   }

   //==========================================================================
   // Set special rules for handling simultaneous drops.
   // E.g. ".SIMULTANEOUS-DROPS NEW-YORK-CITY"
   private void setSimultaneousDrop()
      throws DataFormatException
   {
      int type = getTranslatedToken();
      switch (type) {
         case StringIds.MOD_OFF: rules.simultaneousDrop = Rules.NONE; break;
         case StringIds.MOD_NEW_YORK_CITY: rules.simultaneousDrop = Rules.NEW_YORK; break;
         case StringIds.MOD_CAMBRIDGE: rules.simultaneousDrop = Rules.CAMBRIDGE; break;
         default:
            throw new DataFormatException(currLine);
      }
   }

   //==========================================================================
   // Set special rules for handling transfers.
   // E.g. ".TRANSFER EXHAUST-ANY-DUPES"
   private void setTransferRules()
      throws DataFormatException
   {
      int type = getTranslatedToken();
      switch (type) {
         case StringIds.MOD_EXHAUST_CONTINUING_DUPES: rules.duplicateRankings = Rules.EXHAUST_CONTINUING; break;
         case StringIds.MOD_EXHAUST_ANY_DUPES: rules.duplicateRankings = Rules.EXHAUST_ANY; break;
         case StringIds.MOD_CONTINUE_TILL_2: rules.onMeetingThreshold = Rules.CONTINUE_TILL_2; break;
         default:
            throw new DataFormatException(currLine);
      }
   }

   //==========================================================================
   // Set the tiebreaking rules.
   // E.g. ".TIES ..."
   private void setTiebreakingRules()
      throws DataFormatException
   {
      int type = 0;
      rules.tieBreakingRules.removeAllElements();
      while ((type = getTranslatedToken()) != StringIds.NULL_TOKEN) {
//System.out.println("DataLoader.setTieBeakingRules got type " + type);
         switch (type) {
            case StringIds.MOD_PREVRND: 
               rules.tieBreakingRules.addElement(new Integer(Rules.PREVIOUS_ROUND)); 
               break;
            case StringIds.MOD_ONE_PREVRND: 
               rules.tieBreakingRules.addElement(new Integer(Rules.ONE_PREVIOUS_ROUND)); 
               break;
            case StringIds.MOD_HUMAN: 
               rules.tieBreakingRules.addElement(new Integer(Rules.BY_ELECTION_OFFICIAL)); 
               break;
            case StringIds.MOD_RAND: 
            case StringIds.MOD_RANDOM_COMP: 
               rules.tieBreakingRules.addElement(new Integer(Rules.RANDOM)); 
               break;
            default:
               throw new DataFormatException(currLine);
         }
      }
   }

   // Set the ignoreCommands vector.
   // Used by getCommand().
   private void setIgnoreCommands()
   {
      ignoreCommands = new Vector();

      String command = getNextToken();
      while (!Library.isEmpty(command))
      {
         ignoreCommands.addElement('.' + command);
         command = getNextToken();
      }
   }

   private String getRestOfLine()
   {
      return currLine.substring(currTokenIndex);
   }

   ///case StringIds.COM_CAMB_VAC_RECOUNT:    setupCambridgeVacancyRecount(); break;
   private void setupCambridgeVacancyRecount()
   {
      ///rules.cambridgeVacancyRecount = true;

      finalPileCandAbbr = getNextToken();
      cambridgeRecountState = WRONG_PILE;
      rules.cambridgeSimulDropLevel = 1;
      //Candidate cand = contest.getCandidate(candAbbr);
   }

   //==========================================================================
   // Notification that the ballots in a candidates pile follow.
   // E.g.: ".FINAL-PILE JRL"
   private void finalPileListStarting()
   {
      String candAbbr = getNextToken();
      if (candAbbr.equals(finalPileCandAbbr))
         cambridgeRecountState = RIGHT_PILE;
      else cambridgeRecountState = WRONG_PILE;
   }

} //end class DataLoader
