

package engine;

public class DataFormatException extends Exception
{

   public DataFormatException()
   {
      super();
      ///Thread.dumpStack();
   }

   // A DataFormatException that provides everything you need to know...
   public DataFormatException(String msg)
   {
      super(msg);
      System.out.println(msg);
      ///Thread.dumpStack();
   }
}