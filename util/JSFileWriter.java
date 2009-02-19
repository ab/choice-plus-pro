

package util;

import java.io.*;

public class JSFileWriter extends FileWriter
{
   String fileName = null;

   public JSFileWriter(String filename)
      throws IOException
   {
      super(filename);
      fileName = filename;
   }

   public void println(String s)
   {
      print(s);
      newline();
   }

   public void newlines(int num)
   {
      while (num-- > 0)
         print("\r\n");
   }

   public void newline()
   {
      newlines(1);
   }

   public void print(String s)
   {
      try {
         super.write(s);
      }
      catch (IOException e) {
         throw new FatalDeveloperError(e.getMessage());
      }
   }

   public void print(char c)
   {
      try {
         super.write(c);
      }
      catch (IOException e) {
         throw new FatalDeveloperError(e.getMessage());
      }
   }

   /** Change: -- This is an example.
           To: -- This is an
                  example.
       Where eolAt == 25, and tabAt == 4 (1 based)
    */
   public void printWrap(String str, int eolLoc, int tabLoc)
   {
      String remaining = str;
      while (remaining.length() > eolLoc) {
         String thisLine = remaining.substring(0,eolLoc);
         int lastSpace = thisLine.lastIndexOf(' ');
         thisLine = remaining.substring(0,lastSpace);
         remaining = Library.repeatChars(' ', tabLoc) +
                     remaining.substring(lastSpace).trim();
         println(thisLine);
      }
      println(remaining);
   }

   /** Overrides OutputStreamWriter, trapping the IOException
    */
   public void flush()
   {
      try {
         super.flush();
      }
      catch (IOException e) {
         System.out.println("JSFileWriter.flush() failed! -- file: " + fileName);
      }
   }
}
