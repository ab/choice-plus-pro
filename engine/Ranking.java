

package engine;

/** A simple public struct, really.
 */
public class Ranking
{
   public String candAbbr = null;      // candidate abbreviation
   public boolean nextIsDup = false;   // true if the next ranking is a duplicate
   public int ranking = 0;             // ranking given to this candidate; 0 is none or undefined
}
