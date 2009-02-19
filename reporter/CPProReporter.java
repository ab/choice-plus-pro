package reporter;

import java.io.IOException;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import engine.*;
import util.*;

public class CPProReporter implements ICPProReporter
{
   private int precision = Rules.DEFAULT_PRECISION; ///5; // 5 is the default

   private static final int ruleRtJustify = 21;//kludge -- is calculated empirically, for English...
   private static final int ruleTab = 24;//kludge -- is calculated empirically, for English...
   private static final int eolAt = 80;      // kludge

   private static final String DEL = "|";
   private final LiteralString LS = new LiteralString(); 

   // For the delimited detailed report, the first column will be one
   // of these codes
   private static final String TITLE_CODE       = "10"; // 10, "......"
   private static final String CAND_CODE        = "20"; // 20, name
   private static final String RULE_CODE        = "30"; // 30, label, info
   private static final String CONTEST_HDR_CODE = "40"; // 40, #valid ballots, #to elect, threshold, #invalid ballots
   private static final String ROUND_HDR_CODE   = "50"; // 50, round number, reason
   private static final String CAND_LINE_CODE   = "60"; // 60, cand, diff, total, status, round status determined
   private static final String ROUND_FTR_CODE   = "70"; // 70, cand, new status, reason
   private static final String CONTEST_FTR_CODE = "80"; // 80, footer line

   // possible actions on looping over candidates
   private static final int PRINT_FINPILES_CANDLINE   = 1;
   private static final int PRINT_FINPILES_EXCLUSIONS = 2;
   private static final int PRINT_FINPILES_BALLOTS    = 3;

   // Suffix -Std means the standard, human readable report.
   // Suffix -Del means comma delimited report;
   // Suffix -Htm means HTML report;
   // Suffix -Tmp means a temporary (round result) file
   JSFileWriter detailedReportStd = null;
   JSFileWriter detailedReportDel = null;
   JSFileWriter roundReportTmp = null;
   JSFileWriter chartReportDel = null;
   JSFileWriter transferReportStd = null;
   JSFileWriter transferReportDel = null;
   JSFileWriter finalPilesReportStd = null;
   JSFileWriter simpleTallyReportStd = null;
   JSFileWriter ballotAllocationReportStd = null;
   JSFileWriter ballotAllocationReportDel = null;

   // other
   Contest contest = null;
   Rules rules = null;
   String reportPrefix = null;
   int transfersCount = 0;
   BCD ballotsTransferredValueTotal = new BCD();
   int currentRound = 0;
   File outputDirectory = null;
   Vector navTab = null; // navigation table for HTML reports
   Vector titleLines = null;

   /** Default constructor */
   public CPProReporter()
   {
      //super() called implicitely
   }

   /** Set the directory where the output is to go */
   public void setOutputDirectory(File dir)
   {
      outputDirectory = dir;
   }

   public File getOutputDirectory()
   {
      return outputDirectory;
   }

   /** Call when a contest tally starts.
       Implements ICPProReporter.
    */
   public void onContestStarted(Contest contest)
      throws IOException
   {
      rules = contest.getRules();
      precision = rules.reportPrecision;

      // Figure out report prefix.  Includes the path
      // to place the file in the correct directory.
      reportPrefix = outputDirectory.getPath() + File.separatorChar;

      if (contest.getName() != null)
         reportPrefix = reportPrefix + contest.getName() + " ";

      this.contest = contest;

      if (rules.system == Rules.STV || rules.system == Rules.IRV || rules.system == Rules.IR)
      {
         startDetailReport();
         startTransferReport();
      }
   }

   // start the transfer report
   private void startTransferReport()
      throws IOException
   {
      System.out.println("Beginning Transfer Report");

      String title = reportPrefix + LiteralString.gets("report.title.transferReportStd");
      transferReportStd = new JSFileWriter(title + ".txt");
      transferReportStd.println(title);

      title = reportPrefix + LiteralString.gets("report.title.transferReportDel");
      transferReportDel = new JSFileWriter(title + ".txt");
      transferReportDel.println(title);
      transferReportStd.flush();
      transferReportDel.flush();
   }

   // start the detail reports
   private void startDetailReport()
      throws IOException
   {
      System.out.println("Beginning Detail Report");
      System.out.println("Beginning HTML Reports");

      // Detail Report
      titleLines = contest.getTitleLines();
      String title = reportPrefix + LiteralString.gets("report.title.detailedReportStd");
      detailedReportStd = new JSFileWriter(title + ".txt");
      detailedReportStd.println(title);

      title = reportPrefix + LiteralString.gets("report.title.detailedReportDel");
      detailedReportDel = new JSFileWriter(title + ".txt");
      detailedReportDel.println(title);

      title = reportPrefix + LiteralString.gets("report.title.roundReportHtm");
      roundReportTmp = new JSFileWriter(title + ".tmp");
      roundReportTmp.println("<p><table border=1 cellspacing=0 cellpadding=3>");

      // Print top part of detailed reports
      detailedReportStd.newline();
      detailedReportStd.newline();
      for (int i = 0; i < titleLines.size(); i++) {
         String line = (String)titleLines.elementAt(i);
         detailedReportStd.println(line);
         detailedReportDel.println(TITLE_CODE + DEL + line);
      }
      detailedReportStd.newline();

      // Print candidates on detail reports
      Vector cands = contest.getCandidates();
      detailedReportStd.println(LiteralString.gets("report.label.CANDIDATES"));
      for (int i = 0; i < cands.size(); i++) {
         Candidate cand = (Candidate)cands.elementAt(i);
         detailedReportStd.println("\t" + cand.getName());
         detailedReportDel.println(CAND_CODE + DEL + cand.getAbbr() + DEL + "\"" + cand.getName() + "\"");
      }
      detailedReportStd.newline();

      // output the rules to the detail reports
      outputRules();
      detailedReportStd.newline();

      // output the rest of the contest hdr (# of ballots, etc.)
      outputContestHdr();
      detailedReportStd.newline();
      detailedReportStd.flush();
      detailedReportDel.flush();

      // Print candidates on HTML report
      cands = contest.getCandidates();
      roundReportTmp.println("<tr><td colspan=2 align=center><b><u>CANDIDATES</u></b>");
      for (int i = 0; i < cands.size(); i++) {
         Candidate cand = (Candidate)cands.elementAt(i);
         roundReportTmp.println("<br>" + cand.getName());
      }
      roundReportTmp.println("</td></tr>");
      roundReportTmp.println("</table>");
      roundReportTmp.close();
   }

   // output the rules
   private void outputRules()
   {
      Vector strings = new Vector();
      String buf = null;

      detailedReportStd.println(LiteralString.gets("report.label.ELECTION.RULES"));
      roundReportTmp.println("<tr><th colspan=2>ELECTION RULES</th></tr>");

      outputRule( "system",
                  LiteralString.gets("report.system." + String.valueOf(rules.system)));

      if (rules.distributedCount)
         outputRule("distributedCount", "Yes");

      if (rules.system != Rules.IR)
      {
         outputRule( "threshold",
                  LiteralString.gets("report.threshold."+ String.valueOf(rules.thresholdType)));

         // Next, do the Surplus Transfer lines
         strings.addElement( LiteralString.gets("report.surplus." +
                           String.valueOf(rules.surplusTransfer) ));
         if (rules.avoidExhaustedBallots)
            strings.addElement( LiteralString.gets("report.AvoidExhaustedBallots") );

         strings.addElement( LiteralString.gets("report.onMeetingThreshold." +
                             rules.onMeetingThreshold) );
         if (rules.system == Rules.STV) {
            outputRuleStd("surplusTransfers", strings);
            outputRuleHtm("surplusTransfers", strings);
         }
      }

      // Next, show the early simul drop rule
      switch (rules.simultaneousDrop) {
         case Rules.NONE:
         default:
            buf = LiteralString.gets("None");
            break;
         case Rules.NEW_YORK:
            buf = LiteralString.gets("report.simulDrop.1");
            break;
         case Rules.CAMBRIDGE:
            buf = LiteralString.gets("report.simulDrop.2", rules.cambridgeSimulDropLevel);
            break;
      }
      outputRuleStd("simulDrop", buf);
      outputRuleHtm("simulDrop", buf);

      // Next, do the Tie breaking rules
      strings.removeAllElements();
      for (int i=0; i < rules.tieBreakingRules.size(); i++) {
         strings.addElement( String.valueOf(i+1) + ") " +
            LiteralString.gets("report.ties." +
            String.valueOf( (Integer)rules.tieBreakingRules.elementAt(i) )));
      }
      outputRuleStd("ties", strings);
      outputRuleHtm("ties", strings);
   }

   // output a rule, given the area and a string value
   private void outputRule(String area, String value)
   {
      outputRuleStd(area,value);
      detailedReportDel.println(RULE_CODE + DEL + area + DEL + value);
      outputRuleHtm(area,value);
      roundReportTmp.println("</td>");
      roundReportTmp.println("</tr>");
   }

   // output a rule just to the standard file, given the area and a string value
   private void outputRuleStd(String area, String value)
   {
      String label = LiteralString.gets("report.label." + area);
      int spaces = ruleRtJustify - label.length();
      detailedReportStd.print(Library.repeatChars(' ', spaces));
      detailedReportStd.print(label + ":  ");
      detailedReportStd.println(value);
   }

   // output a rule just to the HTML file, given the area and a string value
   private void outputRuleHtm(String area, String value)
   {
      String label = LiteralString.gets("report.label." + area);
      roundReportTmp.println("<tr>");
      roundReportTmp.println("<td align=right>" + label + ":</td>");
      roundReportTmp.println("<td align=left>" + value);
   }

   // output a rule, with a set of strings to the text reports
   private void outputRuleStd(String area, Vector values)
   {
      outputRuleStd(area, (String)values.elementAt(0));
      detailedReportDel.print(RULE_CODE + DEL + area + DEL + values.elementAt(0));
      for (int i = 1; i < values.size(); i++) {
         String value = (String)values.elementAt(i);
         detailedReportStd.println( Library.repeatChars(' ',ruleTab) + value);
         detailedReportDel.print(DEL + value);
      }
      detailedReportDel.newline();
   }

   // output a rule, with a set of strings to the HTML report
   private void outputRuleHtm(String area, Vector values)
   {
      outputRuleHtm(area, (String)values.elementAt(0));
      for (int i = 1; i < values.size(); i++) {
         String value = (String)values.elementAt(i);
         roundReportTmp.println("<br>" + value);
      }
      roundReportTmp.println("</td>");
      roundReportTmp.println("</tr>");
   }

   // [0] = valid ballots
   // [1] = total vote
   // [2] = number electing
   // [3] = threshold
   // [4] = number of invalid ballots
   private Vector setupContestHdrStrings()
   {
      Vector hdrStrings = new Vector();
      String temp;

      int numValidBallots = contest.numberOfBallots();
      if (numValidBallots == 1)
         temp = LiteralString.gets("report.hdr.validBallots.1");
      else
         temp = LiteralString.gets("report.hdr.validBallots.n", numValidBallots);
      hdrStrings.addElement(new String(temp));

      BCD numVotes = contest.getVoteTotal();
      BCD numBallotsBCD = new BCD(numValidBallots);
      if (!numVotes.equals(numBallotsBCD)) {
         temp = LS.gets01n("report.hdr.votes", numVotes);
         hdrStrings.addElement(temp);
      }

      if (rules.system == Rules.STV || rules.system == Rules.IRV) {
         if (rules.numberToElect == 1)
            temp = LiteralString.gets("report.hdr.elect.1");
         else
            temp = LiteralString.gets("report.hdr.elect.n", rules.numberToElect);
         hdrStrings.addElement(new String(temp));

         BCD threshold = contest.getThreshold();
         if (threshold.equals(BCD.ONE))
            temp = LiteralString.gets("report.hdr.threshold.1");
         else
            temp = LiteralString.gets("report.hdr.threshold.n", threshold.toString());
         hdrStrings.addElement(new String(temp));
      }

      int numInvalidBallots = contest.numberOfInvalidBallots();
      temp = LS.gets01n("report.hdr.invalidBallots", numInvalidBallots);
      hdrStrings.addElement(new String(temp));

      temp = LS.gets01n("report.hdr.totalBallots", numValidBallots + numInvalidBallots);
      hdrStrings.addElement(new String(temp));

      return hdrStrings;
   }

   // Std Report:
   //    -----------------------------------
   //    1688 valid ballots.
   //    7003 votes. [If #votes different than # of ballots]
   //    Electing 1 candidate.
   //    Threshold to win is 845 votes.
   //    There were no invalid ballots.
   //    -----------------------------------
   // Del Report:
   //    40, 1688, 7003, 1, 845, 0
   private void outputContestHdr()
   {
      String separatorLine = Library.repeatChars('-', 50);
      Vector contestHdrStrings = setupContestHdrStrings();

      detailedReportStd.println(separatorLine);
      roundReportTmp.println("<tr>");
      roundReportTmp.print("<th colspan=2>");
      for (int line = 0; line < contestHdrStrings.size(); line++) {
         if (line != 0)
            roundReportTmp.print("<br>");
         roundReportTmp.println((String)contestHdrStrings.elementAt(line));
         detailedReportStd.println((String)contestHdrStrings.elementAt(line));
      }
      detailedReportStd.println(separatorLine);

      detailedReportDel.print(CONTEST_HDR_CODE);
      detailedReportDel.print(DEL + contest.numberOfBallots());
      detailedReportDel.print(DEL + contest.getVoteTotal());
      detailedReportDel.print(DEL + rules.numberToElect);
      detailedReportDel.print(DEL + contest.getThreshold());
      detailedReportDel.print(DEL + contest.numberOfInvalidBallots());
      detailedReportDel.newline();
   }

   /** Call when a transfer occurred.
       Implements ICPProReporter.
    */
   public void onTransferOccurred(Candidate from, Candidate to, Ballot ballot, BCD amountTransferred)
                                    ///BCD xferValue)
   {
      transfersCount++;

      ///BCD amountTransferred = ballot.getAmountMoved();

      if (to == null) // exhausted ballot?
         to = contest.getExhaustedPile();

      // This 'from' == null, then it is the first round, the initial sort,
      // and doesn't need reporting.
      if (from != null)
      {
         ballotsTransferredValueTotal = ballotsTransferredValueTotal.add(amountTransferred);

         if (to == null || from == null || ballot == null)
            throw new FatalDeveloperError();
         transferReportStd.print("   " + ballot.getIdAlpha() + ":  " +
                  from.getName() + " --> " + to.getName());
         if (rules.surplusTransfer == Rules.FRACTIONAL)
            transferReportStd.print( " (at a value of " + amountTransferred.toString(precision) + ")" );
         transferReportStd.newline();

         transferReportDel.println(
                          currentRound
                  + DEL + ballot.getIdAlpha()
                  + DEL + from.getAbbr()
                  + DEL + to.getAbbr()
                  + DEL + amountTransferred);
      }
   }

   private void pageBreak(JSFileWriter file)
   {
      String buf = Library.repeatChars('*', 80);
      file.newline();
      file.newline();
      file.println(buf);
      file.newline();
      file.newline();
   }

   /** Print a report
       Implements ICPProReporter.
    */
   public void print(int report)
   {
      System.out.println("DUMMY CPProReporter.print()");
   }

   /** Call when ballots randomized
       Implements ICPProReporter.
    */
   public void onBallotsRandomized(Vector newOrder)
   {
      System.out.println("DUMMY CPProReporter.onBallotsRandomized()");
   }

   /** Call at the end of each round.
       Implements ICPProReporter.
    */
   public void onRoundComplete(RoundResult result)
      throws IOException
   {
      BCD voteDiffTotal = new BCD();
      BCD voteAllTotal = new BCD();

      pageBreak(detailedReportStd);
      String temp = null;
      JSFileWriter stdRep = detailedReportStd; //shorter!
      JSFileWriter delRep = detailedReportDel; //shorter!

      String title = reportPrefix + LiteralString.gets("report.title.roundReportHtm");
      roundReportTmp = new JSFileWriter(title + result.getRound() + ".tmp");
      JSFileWriter tmpRep = roundReportTmp; //shorter!

      temp = LiteralString.gets("round");
      temp = temp.toUpperCase() + ' ' + result.getRound() + " -- ";
      stdRep.printWrap(temp + result.getTransferExpl(),
                        eolAt, temp.length());
      delRep.println(ROUND_HDR_CODE + DEL + result.getRound() + DEL +
                     result.getTransferExpl() );
      tmpRep.println("<h3>" + temp + result.getTransferExpl() + "</h3>");
      tmpRep.newline();
      tmpRep.println("<p><table border=1 cellspacing=0>");
      tmpRep.newline();
      tmpRep.println("<tr><th>CANDIDATE</th>");
      tmpRep.println("<th>THIS ROUND</th>");
      tmpRep.println("<th>TOTAL</th>");
      tmpRep.println("<th>STATUS</th>");
      tmpRep.println("</tr>");
      tmpRep.newline();

      stdRep.newline();
      stdRep.println(LiteralString.gets("report.roundTableHeader.1"));
      stdRep.println(LiteralString.gets("report.roundTableHeader.2"));

      // Next print the candidate by candidate results
      Vector cands = result.getByStanding();
      for (int i = 0; i < cands.size(); i++) {
         CandRoundResult crr = (CandRoundResult)cands.elementAt(i);
         reportCandRoundResult(crr);
         voteDiffTotal = voteDiffTotal.add(crr.voteDiff);
         voteAllTotal = voteAllTotal.add(crr.voteTotal);
      }

      // Next print the exhausted ballots and the total
      stdRep.newline();
      CandRoundResult crr = result.getExhausted();
      reportCandRoundResult(crr);
      voteDiffTotal = voteDiffTotal.add(crr.voteDiff);
      voteAllTotal = voteAllTotal.add(crr.voteTotal);
      printCandRoundResult(null, LiteralString.gets("report.totals"),voteDiffTotal.toStringSigned(precision),voteAllTotal.toString(precision),"");

      // Finally, print the elimination/election determination from this round.
      stdRep.newlines(3);
      Vector determination = result.getStatusChangeExpl();
      if (determination.size()>0) {
         tmpRep.println("<tr>");
         tmpRep.println("<td colspan=4>");
         for (int i = 0; i < determination.size(); i++) {
            String reason = (String)determination.elementAt(i);
            stdRep.printWrap("-- " + reason, eolAt, 3);
            if (i != 0)
               tmpRep.print("<br>");
            tmpRep.println(reason);
            delRep.println(ROUND_FTR_CODE + DEL + reason);
         }
         tmpRep.println("</td>");
         tmpRep.println("</tr>");
         tmpRep.newline();
      }
      tmpRep.println("<tr>");
      tmpRep.close();

      // Print to the transfer report
      if (result.getRound() > 1) {
         transferReportStd.newline();
         transferReportStd.println( "   " + LS.gets01n("report.transfer.ballots", transfersCount) );
         if (!ballotsTransferredValueTotal.equals(new BCD(transfersCount)))
            transferReportStd.println( "   " + LiteralString.gets("report.transfer.value", ballotsTransferredValueTotal.toString(precision)) );
      }

      transfersCount = 0;
      ballotsTransferredValueTotal = new BCD();
      stdRep.flush();
      delRep.flush();
      transferReportStd.flush();
      transferReportDel.flush();
   }

   // Report one CandRoundResult to the detailed reports
   private void reportCandRoundResult(CandRoundResult crr)
   {

      if (crr != null) {
         String voteDiff = crr.voteDiff.toStringSigned(precision);
         String voteTotal = crr.voteTotal.toString(precision);
         String fullStatusString = getFullStatusString(crr.status, crr.cand.getRoundDetermined());
         String name = crr.cand.getName();
         if (crr.cand.getType() == Candidate.EXHAUSTED_PILE)
            name += ":";
         printCandRoundResult(crr.cand, name, voteDiff,
                              voteTotal.toString(), fullStatusString);
      }
   }

   // Formats and prints one CandRoundResult to the detailed reports.
   private void printCandRoundResult(Candidate cand, String name, String voteDiff,
                                     String voteTotal, String status)
   {
      final int[] columnSizes = {23, 13, 13, 22};
      final int spaceBtwCols = 2;
      JSFileWriter stdRep = detailedReportStd; //shorter!
      JSFileWriter delRep = detailedReportDel; //shorter!
      JSFileWriter tmpRep = roundReportTmp; //shorter!

      if (name.length() > columnSizes[0])
         name = name.substring(0,columnSizes[0]-1) + '.';
      stdRep.print(name);
      int spaces = columnSizes[0] - name.length() + spaceBtwCols +
                   + columnSizes[1] - voteDiff.length();
      stdRep.print(Library.repeatChars(' ',spaces));
      stdRep.print(voteDiff);
      stdRep.print(Library.repeatChars(' ',spaceBtwCols + columnSizes[2] - voteTotal.length()));
      stdRep.print(voteTotal);
      stdRep.print(Library.repeatChars(' ',spaceBtwCols));
      stdRep.print(status);
      stdRep.newline();

      tmpRep.println("<tr>");
      tmpRep.println("<th>" + name + "</th>");
      tmpRep.println("<td align=right>" + Library.stripZeros(voteDiff) + "&nbsp;</td>");
      tmpRep.println("<td align=right>" + Library.stripZeros(voteTotal) + "&nbsp;</td>");
      tmpRep.println("<td>&nbsp;" + status + "</td>");
      tmpRep.println("</tr>");
      tmpRep.newline();

      if (cand != null)
         delRep.println(CAND_LINE_CODE + DEL + cand.getAbbr() +
                        DEL + voteDiff + DEL + voteTotal + DEL + status);
   }

   /** Called at the beginning of each round.
       Implements ICPProReporter.
    */
   public void onRoundStarted(Contest contest, int currentRound)
   {
      this.currentRound = currentRound;
      if (currentRound > 1) {
         transferReportStd.newlines(2);
         transferReportStd.println("*** " + LiteralString.gets("ROUND") + ' ' + currentRound + " ***");
         transferReportStd.newline();
      }
   }

   /** Called when the contest is over.
       Only gets called for STV and IRV, though.
       Implements ICPProReporter.
    */
   public void onContestComplete(boolean tallySuccessful)
      throws IOException
   {
      String finalStatus = null;
      if (tallySuccessful)
         finalStatus = LiteralString.gets("report.tally.successful");
      else finalStatus = LiteralString.gets("report.tally.unsuccessful");
      onContestCompleteDetailRpt(finalStatus);
      onContestCompleteRoundRpt(finalStatus);
      if (tallySuccessful) {
         produceChartRpt();
         produceFinalPilesRpt();
         produceBallotAllocationRpt();

         if (contest.areStatisticsOn())
         {
            Statistician stats = new Statistician(contest, this);
            stats.run();
         }
      }
   }

   /** Called when the contest is over.
    *  Completes the text Detail reports.
    */
   private void onContestCompleteDetailRpt(String finalStatus)
      throws IOException
   {
      pageBreak(detailedReportStd);
      detailedReportStd.println(finalStatus);
      detailedReportStd.newlines(5);
      detailedReportDel.println(CONTEST_FTR_CODE + DEL + finalStatus);

      printFooter(detailedReportStd, detailedReportDel, false);

      // And close the detail and transfer files
      detailedReportStd.close();
      detailedReportDel.close();
      transferReportStd.close();
      transferReportDel.close();

      System.out.println("Ending Detail Report");
      System.out.println("Ending Transfer Report");
   }

   /** Called when the contest is over.
    *  Complete the HTML Round report files
    */
   private void onContestCompleteRoundRpt(String finalStatus)
      throws IOException
   {
      String name = contest.getName();
      if (name == null)
         name = LiteralString.gets("report.title.roundReportHtm");
      else
         name = name + " " + LiteralString.gets("report.title.roundReportHtm");

      createNavTab(name);

      // Now create the starting (Round.htm) file
      String title = reportPrefix + LiteralString.gets("report.title.roundReportHtm");
      convertTmpToHtm(name, title, 0, finalStatus);
      File tf = new File(title + ".tmp");
      tf.delete();

      // now loop through the rounds, creating the individual round HTML files
      for (int round=1; round<=currentRound; round++) {
         convertTmpToHtm(name, title, round, null);
         tf = new File(title + round + ".tmp");
         tf.delete();
      }
      System.out.println("Ending HTML Reports");
   }


   /**
   * Creates a browser friendly version of the contest name
   */
   String nameToHTM(String name)
   {
      int i = name.indexOf(' ');
      while (i > -1)
      {
         name = name.substring(0,i) + "%20" + name.substring(i+1);
         i = name.indexOf(' ');
      }
      return name;
   }

   /**
   *  Creates a navigation table once we know how many rounds there are
   */
   private void createNavTab(String name)
   {
      String HTMname = nameToHTM(name);
      navTab = new Vector();
      // create a Vector of lines for the navigation table
      navTab.addElement(new String("<table border=0 cellpadding=2><tr>"));
      navTab.addElement(new String("<td><a href='" + HTMname + ".htm'>Start</a></td>"));
      for (int i=1; i<currentRound; i++)
         navTab.addElement(new String("<td><a href='" + HTMname + i + ".htm'>" + i + "</a></td>"));
      navTab.addElement(new String("<td><a href='" + HTMname + currentRound + ".htm'>Final</a></td>"));
      navTab.addElement(new String("</tr>"));
      navTab.addElement(new String("<tr>"));
   }

   /**
    *  Converts a temporary round file into an HTML file
    */
   private void convertTmpToHtm(String name, String title, int round, String finalStatus)
      throws IOException
   {
      int i;
      String line = null;
      String HTMname = nameToHTM(name);

      if (round != 0)
         title = title + round;
      JSFileWriter htmRep = new JSFileWriter(title + ".htm");
      JSBufferedReader tmpRep = new JSBufferedReader(title + ".tmp");
      htmRep.println(LiteralString.gets("report.htm.hdr.1"));
      if (round == 0)
         htmRep.println(LiteralString.gets("report.htm.hdr.2", name + " Detail"));
      else
         htmRep.println(LiteralString.gets("report.htm.hdr.2", name + " " + round));
      for (i=3; i<12; i++)
         htmRep.println(LiteralString.gets("report.htm.hdr." + i));

      // Now print contest title as a headline
      for (i=0; i<titleLines.size(); i++) {
         if (i>0)
            htmRep.println("<br>");
         htmRep.println((String)titleLines.elementAt(i));
      }
      htmRep.println("</h2>");

      if (finalStatus != null)
         htmRep.println("<p>" + finalStatus);

      insertNavTab(htmRep, HTMname, round);      // insert the navigation table

      // insert the body of the starting report
      line = tmpRep.readLine();
      while (line != null) {
         htmRep.println(line);
         line = tmpRep.readLine();
      }
      tmpRep.close();

      // if this is the Start file, insert navTab again at the bottom
      // otherwise insert appropriate round links into last row of table
      if (round == 0)
         insertNavTab(htmRep, HTMname, round);
      else if (round == currentRound) { // i.e., this is the final page
         htmRep.println("<td colspan=4 align=left>");
         if (round == 1) { // round 1 is the final round,  so there is no previous round
            htmRep.println("<a href='" + HTMname + ".htm'>&lt;&lt;&nbsp;");
            htmRep.println("Start</a></td>");
         } else {
            htmRep.println("<a href='" + HTMname + (round - 1) + ".htm'>&lt;&lt;&nbsp;");
            htmRep.println("Previous " + LiteralString.gets("Round") + "</a></td>");
         }
         htmRep.println("</tr>");
         htmRep.println("</table>");
      }
      else { // this is neither the start page nor the final page
         htmRep.println("<td colspan=2 align=left>");
         if (round == 1) { // we go back to start instead of a round
            htmRep.println("<a href='" + HTMname + ".htm'>");
            htmRep.println("&lt;&lt;&nbsp;Start</a></td>");
         }
         else {
            htmRep.println("<a href='" + HTMname + (round - 1) + ".htm'>&lt;&lt;&nbsp;");
            htmRep.println("Previous " + LiteralString.gets("Round") + "</a></td>");
         }
         htmRep.println("<td colspan=2 align=right>");
         htmRep.println("<a href='" + HTMname + (round + 1) + ".htm'>");
         htmRep.println("Next " + LiteralString.gets("Round") + "&nbsp;&gt;&gt;</a></td>");
         htmRep.println("</tr>");
         htmRep.println("</table>");
      }

      htmRep.newline();
      htmRep.println("<br><font size=-2><br>");
      htmRep.println(getDateAndTime() + "<br>");
      htmRep.println(LiteralString.gets("version.name") + " -- " + LiteralString.gets("version.number") + "<br>");
      htmRep.println(LiteralString.gets("copyright") + "<br>");
      htmRep.println(LiteralString.gets("report.htm.href"));
      htmRep.println("</font></center></body></html>");
      htmRep.close();
   }


   /**
    *  Inserts the navigation table into htmRep
    */
   private void insertNavTab(JSFileWriter htmRep, String HTMname, int round)
   {
      for (int i=0; i<navTab.size(); i++)
         htmRep.println((String)navTab.elementAt(i));
      if (round == 0) { // we are on the start page
         htmRep.println("<td colspan=" + (currentRound + 1) + " align=center>");
         htmRep.println("<a href='" + HTMname + "1.htm'>First " + LiteralString.gets("Round") +
            "&nbsp;&gt;&gt;</a></td>");
      } else if (round == currentRound) { // we are on the final page
         htmRep.println("<td colspan=" + (currentRound + 1) + " align=center>");
         if (round == 1) // round 1 is the final round
            htmRep.println("<a href='" + HTMname + ".htm'>&lt;&lt;&nbsp;Start</a></td>");
         else
            htmRep.println("<a href='" + HTMname + (round - 1) + ".htm'>&lt;&lt;&nbsp;Previous " + LiteralString.gets("Round") + "</a></td>");
      } else if (round == 1) { // round 1 but not final
         htmRep.println("<td colspan=" + ((currentRound+1)/2) + " align=left>");
         htmRep.println("<a href='" + HTMname + ".htm'>&lt;&lt;&nbsp;Start</a></td>");
         htmRep.println("<td colspan=" + ((currentRound + 1) - ((currentRound + 1)/2))
                + " align=right>");
         htmRep.println("<a href='" + HTMname + "2.htm'>Next " + LiteralString.gets("Round") +
            "&nbsp;&gt;&gt;</a></td>");
      } else { // some non-final round after 1
         htmRep.println("<td colspan=" + ((currentRound+1)/2) + " align=left>");
         htmRep.println("<a href='" + HTMname + (round - 1) + ".htm'>&lt;&lt;&nbsp;" +
            "Previous " + LiteralString.gets("Round") + "</a></td>");
         htmRep.println("<td colspan=" + ((currentRound + 1) - ((currentRound + 1)/2))
                + " align=right>");
         htmRep.println("<a href='" + HTMname + (round + 1) + ".htm'>Next " +
            LiteralString.gets("Round") + "&nbsp;&gt;&gt;</a></td>");
      }
      htmRep.println("</tr>");
      htmRep.println("</table>");
   }


   // Print the date and time
   private void printFooter(JSFileWriter std, JSFileWriter del, boolean quote)
   {
      String dateAndTime = getDateAndTime();

      if (std != null) {
         std.println(dateAndTime);
         std.println(LiteralString.gets("version.name") + " -- " + LiteralString.gets("version.number"));
         std.println(LiteralString.gets("copyright"));
      }
      if (del != null) {
         del.println(CONTEST_FTR_CODE + DEL + dateAndTime);
         del.println(CONTEST_FTR_CODE + DEL + LiteralString.gets("version.name") + " -- " + LiteralString.gets("version.number"));
         del.println(CONTEST_FTR_CODE + DEL + LiteralString.gets("copyright"));
      }
   }

   // Does the chart report.
   private void produceChartRpt()
      throws IOException
   {
      System.out.println("Beginning Chart Report");

      String title = reportPrefix + LiteralString.gets("report.title.chartReportDel");
      chartReportDel = new JSFileWriter(title + ".txt");
      chartReportDel.println(title);

      // Print title lines
      Vector titleLines = contest.getTitleLines();
      for (int i = 0; i < titleLines.size(); i++) {
         String line = (String)titleLines.elementAt(i);
         chartReportDel.println(line);
      }
      chartReportDel.newline();

      // Print threshold, number of valid ballots, number to elect
      Vector contestHdrStrings = setupContestHdrStrings();
      for (int line = 0; line < contestHdrStrings.size(); line++)
         chartReportDel.println((String)contestHdrStrings.elementAt(line));
      chartReportDel.newline();

      // print round numbers
      String roundStr = LiteralString.gets("Round");
      chartReportDel.print(DEL); // no candidate in the column headings
      for (int round = 1; round <= contest.getResults().getNumberOfRounds(); round++) {
         chartReportDel.print(roundStr + ' ' + round);
         chartReportDel.print(DEL);
      }
      chartReportDel.newline();

      // for each candidate, for each round, print total vote rcvd,
      // comma delmited, followed by final status and round status determined.
      Results res = contest.getResults();
      Vector cands = contest.getCandidates();
      for (int candNdx = 0; candNdx < cands.size(); candNdx++) {
         Candidate cand = (Candidate)cands.elementAt(candNdx);
         chartReportDel.print(cand.getName());
         chartReportDel.print(DEL);
         for (int round = 1; round <= res.getNumberOfRounds(); round++) {
            chartReportDel.print( res.getVotes(cand,round).toString() );
            chartReportDel.print(DEL);
         }
         chartReportDel.print(getFullStatusString(cand.getStatus(),
                                          cand.getRoundDetermined()));
         chartReportDel.newline();
      }

      chartReportDel.newlines(3);
      printFooter(chartReportDel, null, true);
      chartReportDel.close();

      System.out.println("Ending Chart Report");
   }

   // Example:  String fullStatusString = getFullStatusString(crr.status, crr.cand.getRoundDetermined());
   private String getFullStatusString(int status, int round)
   {
      StringBuffer fullStatusString = new StringBuffer();

      if (status != Candidate.SPECIAL) {
         fullStatusString.append(Candidate.statusToString(status));
         if (status == Candidate.DEFEATED || status == Candidate.ELECTED) {
            String rndDet = LiteralString.getOrdinal(round);
            fullStatusString.append(" -- ");
            fullStatusString.append(LiteralString.gets("report.roundStatusDetermined", rndDet));
         }
      }

      return fullStatusString.toString();
   }

   private String getDateAndTime()
   {
      DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);
      return df.format(new Date());
   }

   // Does the final piles report.
   private void produceFinalPilesRpt()
      throws IOException
   {
      System.out.println("Beginning Final Piles Report");

      String title = reportPrefix + LiteralString.gets("report.title.finalPilesReportStd");
      finalPilesReportStd = new JSFileWriter(title + ".txt");

      finalPilesReportStd.println(LiteralString.gets("report.finpiles.titleLine"));
      finalPilesReportStd.println(LiteralString.gets("report.finpiles.date", getDateAndTime()));
      finalPilesReportStd.newlines(2);

      for (int i=1; i <= 17; i++)
         finalPilesReportStd.println(LiteralString.gets("report.finpiles." + i));

      loopOverCandidates(PRINT_FINPILES_CANDLINE);

      for (int i = 1; i <= 4; i++)
         finalPilesReportStd.println(LiteralString.gets("report.finpiles.exclude." + i));

      loopOverCandidates(PRINT_FINPILES_EXCLUSIONS);

      for (int i = 0; i <= 7; i++)
         finalPilesReportStd.println(LiteralString.gets("report.finpiles.misc." + i));
      finalPilesReportStd.newline();

      for (int i = 1; i <= 2; i++)
         finalPilesReportStd.println(LiteralString.gets("report.finpiles.format." + i));

      // Now loop over each candidate, printing their ballots, if
      // there are any.  Then print the Exhausted pile.
      loopOverCandidates(PRINT_FINPILES_BALLOTS);
      printFinalPile(contest.getExhaustedPile());

      Vector invalids = contest.getInvalidBallots();
      finalPilesReportStd.newline();
      finalPilesReportStd.println(LiteralString.gets("report.finpiles.invalids"));
      finalPilesReportStd.println("# " +
               LS.gets01n("report.hdr.invalidBallots", invalids.size()));
      printFinalPileBallots(invalids, null);

      // Close the files
      finalPilesReportStd.close();
      System.out.println("Ending File Piles Report");
   }

   // Loop over all the candidates, executing the specified command
   // for each candidate.
   private void loopOverCandidates(int command)
   {
      Vector candidates = contest.getCandidates();
      for (int i = 0; i < candidates.size(); i++) {
         Candidate cand = (Candidate)candidates.elementAt(i);
         switch (command) {
            case PRINT_FINPILES_CANDLINE:   printFinalPilesCandLine(cand); break;
            case PRINT_FINPILES_EXCLUSIONS: printFinalPileExcludes(cand); break;
            case PRINT_FINPILES_BALLOTS:    printFinalPile(cand); break;
            default:
               if (Mode.developing())
                  throw new FatalDeveloperError();
               break;
         }
      }
   }

   // print, e.g.:
   //    .CANDIDATE C01, "Born"
   private void printFinalPilesCandLine(Candidate cand)
   {
      finalPilesReportStd.print(
            LiteralString.gets("input.command." + StringIds.COM_CANDIDATE) );
      finalPilesReportStd.print(' ');
      finalPilesReportStd.print(cand.getAbbr());
      finalPilesReportStd.print(", \"");
      finalPilesReportStd.print(cand.getName());
      finalPilesReportStd.print('\"');
      finalPilesReportStd.newline();
   }

   // print, e.g.:
   //    .EXCLUDE-CANDIDATE C01
   private void printFinalPileExcludes(Candidate cand)
   {
      if (cand.getStatus() == Candidate.ELECTED
          || cand.getStatus() == Candidate.EXCLUDED)
      {
         finalPilesReportStd.print(
                 LiteralString.gets("input.command." + StringIds.COM_EXCLUDE_CAND) );
         finalPilesReportStd.print(' ');
         finalPilesReportStd.print(cand.getAbbr());
         finalPilesReportStd.newline();
      }
   }

   // Print all of the currently owned ballots for a candidate.  E.g.:
   //    # Ballots for candidate CO7 ("Smith")
   //    .FINAL-PILE C07
   //    000503-00-2142) C13, C03, C07, C14
   //    ...
   private void printFinalPile(Candidate cand)
   {
      JSFileWriter std = finalPilesReportStd; //shorter!
      std.newline();
      std.println(
            LiteralString.gets( "report.finpiles.pile",
                     new Object [] {cand.getAbbr(), cand.getName()} ));

      // E.g. "# Candidate has 4 ballots in his/her pile"
      Vector ballots = cand.getBallots();
      if (ballots.size() == 0)
         std.println(LiteralString.gets("report.finpiles.ballots.0"));
      else if (ballots.size() == 1)
         std.println(LiteralString.gets("report.finpiles.ballots.1"));
      else std.println(LiteralString.gets("report.finpiles.ballots.n", ballots.size()));

      // E.g. "# Candidate's vote total is: 43.75"
      if (rules.surplusTransfer == Rules.FRACTIONAL)
         std.println( LiteralString.gets("report.finpiles.voteTotal", cand.getVotes().toString(precision)) );

      // E.g. ".FINAL-PILE AAA"
      // But don't print the line if there are no ballots for this candidate
      if (ballots.size() > 0) {
         std.print( LiteralString.gets("input.command." + StringIds.COM_FINAL_PILE) );
         std.print(' ');
         std.println(cand.getAbbr());
      }

      printFinalPileBallots(ballots,cand);
   }

   // Print the ballots in a final pile
   private void printFinalPileBallots(Vector ballots, Candidate cand)
   {
      JSFileWriter std = finalPilesReportStd; //shorter!

      // Print the ballots
      for (int ballotNdx = 0; ballotNdx < ballots.size(); ballotNdx++) {
         Ballot ball = (Ballot)ballots.elementAt(ballotNdx);
         BCD value = null;
         if (cand != null)
            value = ball.getAllocation(cand);

         std.print(ball.getIdAlpha()); // Ballot Id

         std.print(", ");
         if (value == null)
            std.print("0");
         else
            std.print(value.toString(precision)); // gets the ballot value

         std.print(") ");
         Vector rankings = ball.getRankings();
         for (int rankNdx = 0; rankNdx < rankings.size(); rankNdx++) {
            BallotRanking ranking = (BallotRanking)rankings.elementAt(rankNdx);
            if (ranking != null && ranking.cand != null) {
               std.print(ranking.cand.getAbbr());
               if (ranking.nextIsDup)
                  std.print("=");
               else if (rankNdx + 1 < rankings.size())
                  std.print(",");
               else ; // end of the line
            }
         }
         std.newline();
      }

   }

   /** Call when a simple-tally election is complete */
   public void produceSimpleTallyReport(Contest contest)
      throws IOException
   {
      System.out.println("Beginning Simple Tally Report");

      Vector titleLines = contest.getTitleLines();
      JSFileWriter rpt = null;

      String title = reportPrefix + LiteralString.gets("report.title.simpleTallyReportStd");
      rpt = simpleTallyReportStd = new JSFileWriter(title + ".txt");
      rpt.println(title);

      // Print top part of detailed report
      rpt.newlines(2);
      for (int i = 0; i < titleLines.size(); i++) {
         String line = (String)titleLines.elementAt(i);
         rpt.println(line);
      }
      rpt.newline();

      // Print candidates on detail report
      if (rules.system != Rules.MEASURE)
      {
         Vector cands = contest.getCandidates();
         rpt.println(LiteralString.gets("report.label.CANDIDATES"));
         for (int i = 0; i < cands.size(); i++) {
            Candidate cand = (Candidate)cands.elementAt(i);
            rpt.println("\t" + cand.getName());
         }
         rpt.newline();
      }

      // Output the contest header
      String separatorLine = Library.repeatChars('-', 50);
      Vector contestHdrStrings = setupContestHdrStrings();
      rpt.println(separatorLine);
      for (int line = 0; line < contestHdrStrings.size(); line++)
         rpt.println((String)contestHdrStrings.elementAt(line));
      rpt.println(separatorLine);

      // Output the results
      // TODO: Need to sort; if not a ballot measure
      rpt.newlines(2);
      for (int i = 1; i <= 2; i++)
         rpt.println( LiteralString.gets("report.ballotMeasureHeader." + i) );

      Vector candidates = contest.getCandidates();
      for (int i = 0; i < candidates.size(); i++) {
         Candidate cand = (Candidate)candidates.elementAt(i);
         BCD percent = cand.getVotes().divide(contest.getVoteTotal());
         percent = percent.multiply(new BCD(100));
         printSimpleTallyCandResult(
               cand.getName(), cand.getVotes().toString(0),
               percent.toString(2) + "%");
      }

      rpt.newlines(5);
      printFooter(rpt,null,false);
      rpt.close();
      System.out.println("Ending Simple Tally Report");
   }

   // Formats and prints one CandRoundResult to the detailed reports.
   private void printSimpleTallyCandResult(String name,
                                     String voteTotal, String percent)
   {
      int[] columnSizes = {7, 6, 7};
      if (rules.system != Rules.MEASURE)
         columnSizes[0] = 23;//make into constant; is good size for a candidate name
                              // even better: calculate dyamically
      final int spaceBtwCols = 2;
      JSFileWriter stdRep = simpleTallyReportStd; //shorter!

      if (name.length() > columnSizes[0])
         name = name.substring(0,columnSizes[0]-1) + '.';
      stdRep.print(name);
      int spaces = columnSizes[0] - name.length() + spaceBtwCols +
                   + columnSizes[1] - voteTotal.length();
      stdRep.print(Library.repeatChars(' ',spaces));
      stdRep.print(voteTotal);
      stdRep.print(Library.repeatChars(' ',spaceBtwCols + columnSizes[2] - percent.length()));
      stdRep.print(percent);
      stdRep.newline();
   }

   /** Show the allocation of each ballot */
   public void produceBallotAllocationRpt()
      throws IOException
   {
      System.out.println("Beginning Ballot Allocation Report");

      JSFileWriter stdRep = null;   // the standard ascii report
      JSFileWriter delRep = null;   // the comma delimited report

      String title = reportPrefix + LiteralString.gets("report.title.ballotAllocationReportStd");
      stdRep = ballotAllocationReportStd = new JSFileWriter(title + ".txt");
      stdRep.println(title);
      title = reportPrefix + LiteralString.gets("report.title.ballotAllocationReportDel");
      delRep = ballotAllocationReportDel = new JSFileWriter(title + ".txt");
      stdRep.newlines(2);
      delRep.newlines(2);

      Vector ballots = contest.getBallots();
      // Append the invalid ballots
      Vector invalids = contest.getInvalidBallots();
      for (int i = 0; i < invalids.size(); i++)
         ballots.addElement(invalids.elementAt(i));

      Sorter.sort(ballots.elements(), Sorter.ASCENDING);
      for (int b = 0; b < ballots.size(); b++) {
         Ballot ballot = (Ballot)ballots.elementAt(b);
         Enumeration keys = ballot.getAllocationKeys();
         stdRep.print(ballot.getIdAlpha());
         stdRep.print(": ");
         delRep.print(ballot.getIdAlpha());
         delRep.print(DEL);
         if (keys.hasMoreElements()) {
            // Print each candidate
            while (keys.hasMoreElements()) {
               Candidate cand = (Candidate)keys.nextElement();
               BCD alloc = ballot.getAllocation(cand);
               stdRep.print(cand.getName());
               delRep.print("\"" + cand.getName() + "\"");
               if ( !alloc.equals(BCD.ONE) || !alloc.equals(ballot.getOrigValue()) )
               {
                  stdRep.print(" (");
                  stdRep.print(alloc.toString(precision));
                  stdRep.print(")");
               }
               delRep.print(DEL);
               delRep.print(alloc.toString(precision));
               if (keys.hasMoreElements()) {
                  stdRep.print(", ");
                  delRep.print(DEL);
               }
            }
         }
         else {
            // invalid ballot
            stdRep.print(LiteralString.gets("INVALID"));
            delRep.print(LiteralString.gets("INVALID"));
         }
         stdRep.newline();
         delRep.newline();
      } // end 'for'

      // end of the method, close files, etc.
      stdRep.close();
      delRep.close();
      System.out.println("Ballot Allocation Report Completed");
   }

   public String getReportPrefix() { return reportPrefix; }

   public void printTitleLines(JSFileWriter target)
   {
      titleLines = contest.getTitleLines();
      for (int i = 0; i < titleLines.size(); i++)
      {
         String line = (String)titleLines.elementAt(i);
         target.println(line);
      }
      target.newline();
   }
}
