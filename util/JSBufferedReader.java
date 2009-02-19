package util;

import java.io.*;


// Does not extend BufferedReader, actually, because
// of Java's super() rules.
public class JSBufferedReader
{
   boolean DEBUG = false;

   String fileName = null;
   FileInputStream fis = null;
   BufferedReader file = null;
   int lineCount = 0;
   boolean isOpen = false;

   /**
    *    Gets a BufferedReader, given a filename.<br>
    *    @param fileName The filename to open the stream against.
    */
   public JSBufferedReader(String fileName)
      throws FileNotFoundException
   {
      this.fileName = fileName;
      // super() called implicitely
      fis = new FileInputStream(fileName);
      file = new BufferedReader(new InputStreamReader(fis));
      isOpen = true;
   }

   /** Get fileName */
   public String getFileName() { return fileName; } 
   public BufferedReader getBufferedReader() { return file; }
   public int getLineCount() { return lineCount; }

   /** wrappers around BufferedReader functionality */
   public String readLine() throws IOException
   {
      String ret = file.readLine();
      if (ret != null)
         lineCount++;
      return ret;
   }

   /** wrappers around BufferedReader functionality */
   public void finalize()
   {
   	// This test doesn't seem to work, as finalize() appears not to
      // be called (!?).
   	if (isOpen)
      	throw new FatalDeveloperError("File <" + getFileName() + "> left open!");
   }

	/** Close the file */
   public void close()
   {
   	try { file.close(); }
      catch (IOException e) {}
   }
}



