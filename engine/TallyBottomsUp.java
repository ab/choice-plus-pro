package engine;

import util.BCD;

public class TallyBottomsUp
	extends TallySTV
{
	public TallyBottomsUp()
   {
   	super();
	}

   // Compute the threshold
   // Overrides TallySTV
   protected BCD calcThreshold()
   {
      return(null);
   }

   /** Overrides TallySTV
    * Supposed to check if the current candidate is to be newly elected, and if so, do
    * everything necessary to properly set his/her status, etc.
    * But in bottoms-up, there is no threshold, no one is declared a winner except by
    * elimination
    */
   protected boolean checkIfElected(Candidate curr, BCD votes, BCD threshold)
   {
   	return false;
	}

}
