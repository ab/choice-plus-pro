package reporter;

import java.io.IOException;
import java.util.Vector;

import engine.Ballot;
import engine.BallotRanking;
import engine.Candidate;
import engine.Contest;
import util.JSFileWriter;
import util.LiteralString;

public class Statistician
{
	private Contest contest = null;
	private CPProReporter reportClass = null;
   private JSFileWriter reportFile = null;
//   private final LiteralString LS = new LiteralString(); //really for shorthand!

	public Statistician(Contest contest, CPProReporter rep)
   {
   	this.contest = contest;
      this.reportClass = rep;
	}

   public void run()
   	throws IOException
   {
   	System.out.println("Beginning Statistical Report");

      String title = LiteralString.gets("report.title.statisticsReport");
  	   reportFile = new JSFileWriter(reportClass.getReportPrefix() + title + ".txt");
     	reportFile.println(title);
      reportFile.newlines(2);

      reportClass.printTitleLines(reportFile);
      reportFile.newlines(2);

   	doVotingDepthReport();  reportFile.newlines(3);
  		doSuccessReport();

      reportFile.close();
   	System.out.println("Ending Statistical Report");
   }

   private void doVotingDepthReport()
   {
      Ballot ballot = null;
      Vector ballots = contest.getBallots();
      Vector cands = contest.getCandidates();
      ///Rules rules = currentContest.getRules();
      double depthArray[] = new double[cands.size()+1];
      double total = 0.0D;

      for (int ndx = 0; ndx < ballots.size(); ndx++)
      {
         ballot = (Ballot)ballots.elementAt(ndx);
         Vector rankings = ballot.getRankings();
         double value = ballot.getOrigValue().doubleValue();
         depthArray[rankings.size()] += value;
         total += value;
      }

      // The data is now there, just need to emit it now...
      reportFile.println(LiteralString.gets("report.votingDepth.title"));
      reportFile.newline();
      reportFile.println(LiteralString.gets("report.votingDepth.explanation"));
      reportFile.newline();
      for (int i = 0; i < depthArray.length; i++)
      {
      	double depth = depthArray[i];
         double pct = depth / total * 100;
      	reportFile.println(depth + " voters ranked " + i + " candidates -- " + pct + "%.");
      }
   }

   // How many voters elected a candidate?
   // How many voters had their 1st, 2nd, etc., ranking elect someone?
   private void doSuccessReport()
   {
      Ballot ballot = null;
      BallotRanking ranking = null;
//      Candidate cand = null;
      double totalVote = 0.0D;
      Vector ballots = contest.getBallots();
      double successArray[] = new double[contest.getCandidates().size()+1];

      for (int ndx = 0; ndx < ballots.size(); ndx++) {
         ballot = (Ballot)ballots.elementAt(ndx);
         Vector rankings = ballot.getRankings();
         int actualRank = 1;         // With duplicate rankings, it could be 2nd in the list, bur ranked #1
         double voteValue = ballot.getOrigValue().doubleValue(); // With weighted votes, the vote value could be != 1
         totalVote += voteValue;
         boolean oneElected = false; // Turned on when a voter successfully elected a candidate
         for (int rankNdx = 0; !oneElected && rankNdx < rankings.size(); rankNdx++) {
            ranking = (BallotRanking)rankings.elementAt(rankNdx);
// 5/4/01 - next line added by SCW to stop NullPointerException on ASSU president data
            if (ranking == null || ranking.cand == null)
               continue;
            int status = ranking.cand.getStatus();
  			    if (status == Candidate.ELECTED || status == Candidate.UNDEFEATABLE) {
               successArray[actualRank] += voteValue;
               oneElected = true;
            }
            if (!ranking.nextIsDup)
               actualRank++;
         }
         if (oneElected == false)
            successArray[0] += voteValue;
      }

      // The data is now there, just need to emit it now...
      reportFile.println(LiteralString.gets("report.votingSuccess.title"));
      reportFile.newline();
      reportFile.println(LiteralString.gets("report.votingSuccess.explanation.1"));
      reportFile.println(LiteralString.gets("report.votingSuccess.explanation.2"));
      reportFile.newline();
      reportFile.println(totalVote + " votes.");
      for (int i = 1; i < successArray.length; i++)
      {
      	double val = successArray[i];
         double pct = val / totalVote * 100;
      	reportFile.println(val + " voters elected their #" + i + " choice -- " + pct + "%.");
      }
      double pct = successArray[0] / totalVote * 100;
      reportFile.println(successArray[0] + " voters did not elect any candidates -- " + pct + "%.");

   }
}