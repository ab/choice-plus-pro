

package engine;

import util.Library;

/**
 *    Get the maximum number of votes and maximum number of
 *    candidates allowed under the current model.
 *    @author JRL 10/3/98
 */
public class Mode
{
   public static final int UNLIMITED = -1;

   // What model is being used?
//   private static final int DEMO = 0;
//   private static final int NGO_SMALL = 1;
//   private static final int NGO_LARGE = 2;
   private static final int GOV = 3;
   private static final int model = GOV;

   // autotesting
   public static boolean autotesting = false;


   //======================================================================
   private class LicensingOption
   {
      LicensingOption(int v, int c) { maxVotes = v; maxCands = c; }
      int maxVotes;
      int maxCands;
   } private LicensingOption opts[] =
   {
                       //   Max    Max
                       //   Votes, Cands
         new LicensingOption(  100,   10),  //DEMO 
         new LicensingOption( 1000,   50),  //NGO SMALL
         new LicensingOption(10000,  500),  //NGO LARGE
         new LicensingOption(   -1,   -1)   //GOVERNMENT   (-1 == UNLIMITED)
   };

   //======================================================================
   /**
    *    Get the maximum number of votes allowed under the current
    *    model.
    */

   public int getMaxVotes()
   {
      ///int model = Integer.parseInt(LiteralString.gets("m"));
      return opts[model].maxVotes;
   }

   //======================================================================
   /**
    *    Get the maximum number of votes allowed under the current
    *    model.
    */

   public int getMaxCands()
   {
      ///int model = Integer.parseInt(LiteralString.gets("m"));
      return opts[model].maxCands;
   }

   //======================================================================
   /** return 'true' if we are in development mode */
   public static boolean developing()
   {
      return Library.developing();
   }
}
