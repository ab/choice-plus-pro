

package util;

public class JSError
{
   public static final int FATAL = 1;
   public static final int WARNING = 2;

   public static final int CANT_FIND_STRING_FILE = -1;
   public static final int CANT_READ_STRING_FILE = -2;
   public static final int CANT_FIND_PROPERTIES_FILE = -3;
   public static final int CANT_READ_PROPERTIES_FILE = -4;

   public JSError()
   {
   }

   /** Builds and emits the message */
   public static String buildMsg(int type, int number)
   {
      return buildMsg(type, number, null, null);
   }
   public static String buildMsg(int type, String msg)
   {
      return buildMsg(type, 0, null, msg);
   }
   public static String buildMsg(int type, int number, String sub)
   {
      return buildMsg(type, number, sub, null);
   }
   /** Build the message */
   public static String buildMsg(int type, int number, String sub, String extraMsg)
   {
      StringBuffer finalMsg = null;

      Object[] params = new Object[1];
      params[0] = sub;

      finalMsg = new StringBuffer(buildMsg(type, number, params));
      if (extraMsg != null)
        finalMsg.append("\n\t" + extraMsg);

      return finalMsg.toString();
   }

   public static String buildMsg(int type, int number, Object[] params)
   {
      StringBuffer msg =  new StringBuffer();

      if (number == 0) {
         if (params != null) {
            msg.append(params);
            msg.append("; ");
         }
      }
      else if (number == CANT_FIND_STRING_FILE)
         msg.append("FATAL ERROR #-1: Cannot find or open string file.");
      else if (number == CANT_READ_STRING_FILE)
         msg.append("FATAL ERROR #-2: Cannot load string file.");
      else if (number == CANT_FIND_PROPERTIES_FILE)
         msg.append("FATAL ERROR #-3: Cannot find or open string file.");
      else if (number == CANT_READ_PROPERTIES_FILE)
         msg.append("FATAL ERROR #-4: Cannot load string file.");
      else {
         String prefixStr = (type == FATAL) ? "error." :"warning.";
         msg =  new StringBuffer(LiteralString.gets(prefixStr + "title"));
         msg.append(" #");
         msg.append(number);
         msg.append(": ");
         if (params != null)
            ///msg.append(LiteralString.get(number, sub));
            msg.append(LiteralString.gets(prefixStr + number, params));
         else
            ///msg.append(LiteralString.get(number));
            msg.append(LiteralString.gets(prefixStr + number));
      }

      return msg.toString();
   }

   /**
    *  Use for "impossible" errors.  Catch programming errors.
    */
   public static void impError(String msg)
   {
      System.out.println("\"IMPOSSIBLE\" ERROR: " + msg);
      Thread.dumpStack();
   }
}
