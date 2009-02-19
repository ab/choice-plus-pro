

package util;

public class FatalDeveloperError extends RuntimeException
{
   public FatalDeveloperError()
   {
      super();
   }

   public FatalDeveloperError(String errMsg)
   {
      super(errMsg);
   }

   public FatalDeveloperError(Exception e)
   {
      super(e.getMessage());
      System.out.println(e.getMessage());
      e.printStackTrace();
   }

}