
//Title:        CPPro
//Version:
//Copyright:    Copyright (c) 1998
//Author:       JRL
//Company:      Jerel
//Description:  CPPro


package util;

/**
 *    Get the maximum number of votes and maximum number of
 *    candidates allowed under the current model.
 *    @author JRL 10/3/98
 */
public class Model
{
//   private static final int DEMO = 0;
//   private static final int NGO_SMALL = 1;
//   private static final int NGO_LARGE = 2;
   private static final int GOV = 3;

   private static final int CURRENT_MODEL = GOV;

   public static final int UNLIMITED = -1;

   /**
    *    Get the maximum number of votes allowed under the current
    *    model.
    */
   public int getMaxVotes()
   {
      return opts[CURRENT_MODEL].maxVotes;
   }

   /**
    *    Get the maximum number of votes allowed under the current
    *    model.
    */
   public int getMaxCands()
   {
      return opts[CURRENT_MODEL].maxCands;
   }

   LicensingOption opts[] =
   {
                       //   Max    Max
                       //   Votes, Cands
         new LicensingOption(  100,   10),  //DEMO 
         new LicensingOption( 1000,   50),  //NGO SMALL
         new LicensingOption(10000,  500),  //NGO LARGE
         new LicensingOption(   -1,   -1)   //GOVERNMENT   (-1 == UNLIMITED)
   };

   class LicensingOption
   {
      LicensingOption(int v, int c) { maxVotes = v; maxCands = c; }
      int maxVotes;
      int maxCands;
   }
}
