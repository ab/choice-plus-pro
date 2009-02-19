// LiteralString.java -- Copyright (c) Jerel Software 1998-99 -- All Rights Reserved

package util;

import java.io.StreamTokenizer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

import engine.CPProProperties; //TODO -- get rid of this...

/**
 *
 */
public class LiteralString
{
   private static final int NOT = -1;
   private static String USAGE_INPUT = "INPUT"; //deprecated
//   private static String USAGE_OUTPUT = "OUTPUT"; //deprecated

   /** Case transformations */
/* public static final int NONE = 0;      // This is a sample.
   public static final int UPPER = 1;     // THIS IS A SAMPLE.
   public static final int LOWER = 2;     // this is a sample.
   public static final int VARIABLE = 3;  // ThisIsASample.
   public static final int SENTENCE = 4;  // This is a sample.
*/
   // Key is the string id, contents is the string
   static Hashtable stringsById = new Hashtable();

   // Key is the string id, contents is the string
   static Hashtable stringsByString = new Hashtable();

   // Properties file -- to eventually replace the other stuff
   static PropertyResourceBundle text = null;
   static PropertyResourceBundle textDefault = null;

   /**
    *    Get a string, with a substitution.
    *    Substitutes for "%1"
    */
   public static String get(int stringId, Object param1)
   {
      String rawString = (String)stringsById.get( new Integer(stringId) );
      StringBuffer buf = new StringBuffer();

      int location = rawString.indexOf("%1");
      if (location != NOT)
      {
         buf.append(rawString.substring(0, location));
         buf.append(param1);
         buf.append(rawString.substring(location+2));
      }
      return buf.toString();
   }

   /**
    *  The simplest "get()"; just return the string given the id.
    */
   public static String get(int stringId)
   {
      return (String)stringsById.get( new Integer(stringId) );
   }

   /**
    *    Return the string-id given the string.
    *    Returns -1 (NOT) for failure.
    */
   public static int get(String string)
   {
      int id = NOT;

      Integer stringId = (Integer)stringsByString.get(string);
      if (stringId != null)
         id = stringId.intValue();

      return id;
   }

   /**
    *    Loads strings from the file "strings.dat" into the Hashtable.
    */
   public static void load(String textPropertiesFileName)
   {
      loadStringsDat(); // TODO: remove this!

      // Now do the string properties file(s)
      try {
         // Using PropertyResourceBundle -- manually...
         FileInputStream file = new FileInputStream(textPropertiesFileName);///_en_US.properties");
         text = new PropertyResourceBundle(file);
///System.out.println("LiteralString.load loaded '" + textPropertiesFileName + "' as the properties file.");
         file = new FileInputStream(CPProProperties.getDefaultTextPropertiesFileName());
         textDefault = new PropertyResourceBundle(file);
///System.out.println("LiteralString.load loaded '" + CPProProperties.getDefaultTextPropertiesFileName() + "' as the default properties file.");
/*
         // Try using resource bundles
         try {
            text = ResourceBundle.getBundle(textPropertiesFileName, Locale.getDefault());//FIXIT! this needs to work, I think
         }
         catch (MissingResourceException mre){
            System.out.println(mre.getMessage());
            System.out.println(mre.getClassName());
            System.out.println(mre.getKey());
            mre.printStackTrace();
         }
         ///text = ResourceBundle.getBundle("text");
*/
         // Output test.
         ///System.out.println("Resource Test String = <" + text.getString("test.string") + ">");
         ///System.out.println("Resource Test String 2 = <" + text.getString("candidate") + ">");
      }
      catch (Exception e) {
         System.out.println(e);
         e.printStackTrace();
         System.exit(-1);
      }
   }

   // TO BE REMOVED -- loads strings.dat file
   private static void loadStringsDat()
   {
//      int tokenType = StreamTokenizer.TT_WORD;
      int tokenNumber = 0;
      Integer stringId = null;
      String usage = null;
      String contents = null;
      StreamTokenizer tokens = null;

      try {
//System.out.println("LiteralString.loadStringsDat() loading " + CPProProperties.getOldStringDataFileName());
         tokens = Library.getStreamTokenizer(CPProProperties.getOldStringDataFileName());
         tokenNumber = 0;
         while (tokens.ttype != StreamTokenizer.TT_EOF) {
            tokens.nextToken();
//System.out.println(tokenNumber + ":" + stringId + ":" + usage + ":" + contents + ":" + tokens);
            switch (tokens.ttype) {
               case StreamTokenizer.TT_EOF:
                  // Intentionally dropping thru;
                  // I suppose I might get an EOF instead of an EOL...
               case StreamTokenizer.TT_EOL:
                  if (contents != null) {
                     if (usage.equals(USAGE_INPUT))
                        stringsByString.put(contents, stringId);
                     else stringsById.put(stringId, contents);
                  } 
                  //else System.out.println("Hit end of line with no content");
                  stringId = null;
                  usage = null;
                  contents = null;
                  tokenNumber = 0;
                  break;
               case StreamTokenizer.TT_NUMBER:
                  Double value = new Double(tokens.nval);
                  stringId = new Integer(value.intValue());
                  tokenNumber++;
                  break;
               case StreamTokenizer.TT_WORD:
               case 34:   //KLUDGE!  For some reason StreamTokenizer is returning this!
            	          // SCW StreamTokenizer returns this if the token is a single "double quote" character (ascii 34)
                  if (tokenNumber == 1)
                     usage = tokens.sval;
                  else if (tokenNumber == 2)
                     contents = tokens.sval;
                  else JSError.impError("Token number error in LiteralString.load(): tokenNumber = " + tokenNumber);
                  tokenNumber++;
                  break;
               default:
                  JSError.impError("Token type error in LiteralString.load()!  'Tokens' == " + tokens);
                  break;
            } // end switch
         }  // end while
      }  // end try
      catch (FileNotFoundException e2) {
         System.out.println(
           JSError.buildMsg(JSError.FATAL, JSError.CANT_FIND_STRING_FILE) );
           //TODO -- don't emit from here...
      }
      catch (IOException e) {
         System.out.println(
            JSError.buildMsg(JSError.FATAL, JSError.CANT_READ_STRING_FILE) );
           //TODO -- don't emit from here...
      }
   }

   /** Get string from resource bundle -- w/no params
       E.g. String foo = litStr.gets("autotest.failed");
    */
   public static String gets(String key)
   {
      return gets(key, (Object[])null);
/*
      String newString = null;

      try {
         newString = text.getString(key);
      }
      catch (MissingResourceException e) {
         newString = "[?? <" + key + "> ??]";
      }

      return (newString);
*/
   }

   /** Get string from resource bundle -- with one parameter
       E.g. String foo = litStr.gets("autotest.failed", "whatever");
    */
   public static String gets(String key, Object param)
   {
      return gets(key, new Object[]{param});
   }

   /** Get string from resource bundle, with parameters
       E.g. String foo = litStr.gets("a", new Object[]{"b"});
       Will never return null.  If you need a null, use getsNull().
    */
   public static String gets(String key, Object[] params)
   {
   	String newString = getsNull(key, params);
      if (newString == null)
      	newString = "[?? " + key + " ??]";

      return newString;
	}

   // GetsNull() will return the string if found, and null if
   // not found.
   private static String getsNull(String key, Object[] params)
	{
      String rawString = null;
      String newString = null;
      try {
         rawString = text.getString(key);
///System.out.println("LiteralString.getsNull got '" + rawString + "', for key '" + key + "', from the properties file.");
      } catch (MissingResourceException e) {
         try {
            rawString = textDefault.getString(key);
///System.out.println("LiteralString.getsNull got '" + rawString + "' for key '" + key + "', from the default properties file.");
         }
         catch (MissingResourceException e2) {
///System.out.println("LiteralString.getsNull failed to get a String for key '" + key + "', from the either properties file.");
            ; // do nothing -- just return null
         }
      }

      if (params != null && rawString != null)
         newString = MessageFormat.format(rawString, params);
      else newString = rawString;

      return (newString);
   }

   /** Get string from resource bundle -- with one int parameter
       E.g. String foo = litStr.gets("rules.elect.n", 5);
    */
   public static String gets(String key, int param)
   {
      return gets(key, new Integer(param));
   }

   /** Return the ordinal for a given number
    *  deprecated -- not internationalized
    */
   public static String getOrdinal(int number)
   {
      String suffix = "th";
      StringBuffer buf = new StringBuffer();
      int numberAbs = Math.abs(number);
      if (numberAbs > 10 && numberAbs < 20)
         suffix = "th";
      else {
         int lastDigit = numberAbs % 10;
         switch (lastDigit) {
            case 1:  suffix = "st"; break;
            case 2:  suffix = "nd"; break;
            case 3:  suffix = "rd"; break;
            default: suffix = "th"; break;
         }
      }
      buf.append(String.valueOf(number));
      buf.append(suffix);

      return buf.toString();
   }

   /** Just like gets(), but needs the number to be
       zero, one, or 'n'.  The text.properties file
       must contain 3 lines: keyPrefix + '0', '1', and 'n'.
    */
   public String gets01n(String keyPrefix, int number)
   {
   	return gets01n(keyPrefix, new BCD(number));
   }

   /** Just like gets(), but needs the number to be
       zero, one, or 'n'.  The text.properties file
       must contain 3 lines: keyPrefix + '0', '1', and 'n'.
    */
   public String gets01n(String keyPrefix, BCD number)
   {
      if (number.isZero())
         return gets(keyPrefix + ".0");
      else if (number.equals(BCD.ONE))
         return gets(keyPrefix + ".1");
      else
         return gets(keyPrefix + ".n", number);
   }

   /** Append a set of strings to a given vector.
    * E.g., appendStringSet("gui.about.a", lines) will
    * look for "gui.about.a1", then "gui.about.a2", etc.,
    * until the lookup fails, adding each found line to the
    * 'lines' vector.
    */
   public void appendStringSet(String key, Vector lines)
   {
      String curr = null;
      boolean done = false;

      for (int i = 1; !done; i++) {
      	curr = getsNull(key + i, null);
         if (curr != null)
         	lines.addElement(curr);
         else done = true;
		}
   }
}

