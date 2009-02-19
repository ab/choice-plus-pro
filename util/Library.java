

package util;

import java.io.*;
import java.util.Vector;

public class Library 
{
   private static boolean IN_DEVELOPMENT_MODE = true;
//   private static boolean DEBUG = false;
   /**
    *    <pre>
    *    Gets a StreamTokenizer, given a filename.<br>
    *    Sets '#' as the comment char, accepts Java style comments,
    *    adds commas to the list of whitespace chars, and declares that
    *    EOLs are important.
    *    Note: In PRMaster, used for the strings.dat file, but not the
    *    main import file, because it only works for simple parsing.
    *    @param fileName The filename to open the stream against.
    *    @param errorMsg The error message to emit if the attempt should fail.
    */
   public static StreamTokenizer getStreamTokenizer(String fileName)
      throws FileNotFoundException
   {
      StreamTokenizer stream = null;
      JSBufferedReader file = new JSBufferedReader(fileName);

      stream = new StreamTokenizer(file.getBufferedReader());
      stream.commentChar('#');
      stream.slashSlashComments(true);
      stream.slashStarComments(true);
      stream.whitespaceChars(',', ',');
      stream.eolIsSignificant(true);

      return stream;
   }

   /**
    *    Is this object empty?
    */
   public static boolean isEmpty(Object obj)
   {
      boolean isEmpty = false;

      if (obj == null)
         isEmpty = true;
      else if (obj instanceof String)
         isEmpty = ( ((String)obj).trim().length() == 0 );
      else if (obj instanceof Vector)
         isEmpty = ( ((Vector)obj).size() == 0);
      else
         throw new FatalDeveloperError();

      return isEmpty;
   }

   /**
    *  Create a string of 'count' characters.
    *  E.g. Library.repeatChars('*', 5) == "*****";
    */
   public static String repeatChars(char c, int count)
   {
      StringBuffer buf = new StringBuffer();
      while (count-- > 0)
         buf.append(c);
      return buf.toString();
   }


   /**
    *  Strip trailing zeros from a string number with a fractional part.
    *  E.g. Library.stripZeros("15.000") == "15";
    */
   public static String stripZeros(String source)
   {
      int dot = source.indexOf('.');
      if (-1 == dot)
         return source;
      StringBuffer buf = new StringBuffer(source);
      int count = buf.length() - 1;
      while (count > 0 &&  ('0' == buf.charAt(count)))
         count--;
      if (dot == count)
         count--;
      buf.setLength(++count);
      return buf.toString();
   }

   /** In Development Mode? */
   public static boolean developing()
   {
      return (IN_DEVELOPMENT_MODE);
   }

   /** (Could be in a JSFile class, but not now).
    *  Will return the directory for the given file name, not in 8.3, but
    *  rather with long-names allowed.  Relative file names will use
    *  the current working directory.
    */
   public static File getFullDirectory(String fileName)
   {
      File file = new File(fileName);
   	return getFullDirectory(file);
   }

   public static File getFullDirectory(File file)
   {
      try {
         String canPath = file.getCanonicalPath();

         // We now have a guaranteed full path...
         File fileFullPath = new File(canPath);
         ///return new File(fileFullPath.getPath());
         return new File(fileFullPath.getParent());
      }
      catch (Exception e)
      {
         return null;// TODO
      }
   }

   public static Vector makeVector(Object[] array)
   {
   	Vector vec = new Vector(array.length);

      for (int i=0; i < array.length; i++)
      	vec.addElement(array[i]);

      return vec;
   }
}

