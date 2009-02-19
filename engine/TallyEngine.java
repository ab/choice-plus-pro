package engine;

import engine.Contest;
import reporter.ICPProReporter;
import gui.ICPProGUI;

/** This is an abstract superclass.  Each system supported by ChoicePlus must
 *  create a subclass of this class, and implement the abstract methods.
 *
 *	 @see engine.TallySTV
 *  @see engine.TallyBottomsUp
 *  @see engine.Tally
 *  @author JLindsay 12/00
 */
public abstract class TallyEngine
{
   public abstract void tally(Contest contest, ICPProReporter reporter, ICPProGUI gui);
}
