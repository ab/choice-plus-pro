

package engine;

import java.io.*;
import java.util.Properties;
import util.*;

public class CPProProperties
{
   static Properties props = null;
   static String homeDirectory = null;

   public static void setHomeFileDir(String homeFileDir)
   {
      homeDirectory = homeFileDir;
   }
   public static String getHomeFileDir()
   {
   	return homeDirectory;
   }

	public static String getFileOpenStart()
   {
   	return props.getProperty("fileOpenStart");
   }

	public static String getFileOpenExtension()
   {
   	return props.getProperty("fileOpenExtension");
   }

   public static void loadProps(String propFileName)
   {
     try {
         props = new Properties();
			String fullName = homeDirectory + File.separatorChar +
         						"system" + File.separatorChar + propFileName;
		InputStream stream = new FileInputStream(fullName);
         props.load(stream);
      }
      catch (Exception e) {
         throw new FatalDeveloperError(e);
      }
   }

   public static String getDefaultTextPropertiesFileName()
   {
      return homeDirectory + File.separatorChar
      	+ "system" + File.separatorChar
         + props.getProperty("resource.file.default");
   }

   public static String getTextPropertiesFileName()
   {
      return homeDirectory + File.separatorChar
      	+ "system" + File.separatorChar
      	+ props.getProperty("resource.file");
   }

   public static String getOldStringDataFileName()
   {
      return homeDirectory + File.separatorChar
      	+ "system" + File.separatorChar
      	+ props.getProperty("old.strings.file");
   }

   public static String localeLanguage() { return props.getProperty("locale.language"); }
   public static String localeCountry() { return props.getProperty("locale.country"); }
   public static String localeVariant() { return props.getProperty("locale.variant"); }

   // UNDOCUMENTED PROPERTIES/OPTIONS
   ///public static boolean mimicV1bugs1() { return props.getProperty("mimicV1.duplicate.starting").equalsIgnoreCase("true"); }
   ///public static boolean mimicV1bugs2() { return props.getProperty("mimicV1.duplicate.internal").equalsIgnoreCase("true"); }
}


