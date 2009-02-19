

package engine;

import java.io.*;
import java.util.*;

import gui.ICPProGUI;
import gui.CPProGUI;
import gui.CPProNoGUI;
import reporter.ICPProReporter;
import reporter.CPProReporter;
import util.*;

/**
 * The entry point for the CPPro tallying engine.
 *
 * @author Jim Lindsay 9/98
 */
public class Engine
{
   public static ICPProGUI gui = null;
   public static ICPProReporter reporter = null;
   private static String initialDataFile = null;

   private boolean invokedStandalone = false;
//   private static final boolean DEBUG = false;

//   private LiteralString LS = new LiteralString();

   private Contest currentContest = null;
   private Rules rules = null;
//   private Autotest autotest = null;

   private TallyEngine tallyEngine = null;

   /**
    * Constructor called from main(); will load a contest via an import file.
    * Also called from CPProGUI after having obtained a file to
    * load and run; engine.start(filename) is then called.
    */
   public Engine()
   {
      init();
   }

   public static String getInitialDataFile() { return initialDataFile; }
//   private static void setInitialDataFile(String filename) { initialDataFile = filename; }

   /** initialize */
   public void init()
   {
      ///CPProProperties props = new CPProProperties();
/* TODO WHEN ResourceBundle behaves...
      Locale locale = new Locale(props.localeLanguage(),
                                 props.localeCountry(),
                                 props.localeVariant());
      Locale.setDefault(locale);
*/

      LiteralString.load(CPProProperties.getTextPropertiesFileName());
      BCD.classInit();

      if (reporter == null)
         reporter = new CPProReporter();
      if (gui == null)
         gui = new CPProNoGUI();

   }

   /**
    *  Start the engine.
    *  Expected input: ...Engine -p c:\prmaster\system myinput.txt
    */
   public void start(String _initialDataFile)
   {
   	  Engine.initialDataFile = _initialDataFile;
      loadAndTally(_initialDataFile);
   }

   /**
    *  Given a filename, load it and tally it.  Can be called
    *  recursively, when it hits an autotest command.
    *  The fileName passed in should always be a complete path
    */
   private void loadAndTally(String fileName)
   {
      try {
         reporter.setOutputDirectory(Library.getFullDirectory(fileName));
         DataLoader loader = new DataLoader(fileName, reporter);
         Object loadedObject = loader.load();
         if (loadedObject instanceof Vector) {
            Mode.autotesting = true;
            Vector autotestFiles = (Vector)loadedObject;
            for (int i = 0; i < autotestFiles.size(); i++) {
               fileName = (String)autotestFiles.elementAt(i);
               File newFile = new File(fileName);
               if (!newFile.isAbsolute()) {
						File originalDir = Library.getFullDirectory(initialDataFile);
         			fileName = originalDir + File.separator + fileName;
               }
               loadAndTally(fileName); // recursive call
               if (currentContest != null)
                  currentContest.compareAutotestResults(fileName);
            }
         }
         else tally((Contest)loadedObject);
      }
      catch (FileNotFoundException e) {
         gui.msgOK( JSError.buildMsg(JSError.FATAL, StringIds.FILE_NOT_FOUND, fileName) );
      }
   }

   /**
    * Command line entry point.
    */
   public static void main(String[] args)
   {
      parseCommandLineParameters(args);
      Engine engine = new Engine();
      engine.invokedStandalone = true;

      engine.start(initialDataFile);
      if (Mode.developing())
         System.out.println("Program Complete");
      System.exit(0);
   }


   /**
    * Handle command line parameters.
    */
   public static void parseCommandLineParameters(String[] args)
   {
      String homeFileDir = null;

      for (int i=0; i < args.length; i++)
      {
         String param = (String)args[i];
         if (param.charAt(0) == '-')
         {
            if (param.charAt(1) == 'h') {
					homeFileDir = (String)args[++i];
System.out.println("homeFileDir = " + homeFileDir);
               CPProProperties.setHomeFileDir(homeFileDir);
               CPProProperties.loadProps("ChoicePlus.properties");
            }
            //else JSError.msg(...); -- TODO
         }
         else initialDataFile = param;
      }
   }

   /**
    *  Given a Contest, tally it.
    */
   public void tally(Contest contest)
   {
      boolean dataOK = false;

      currentContest = contest;
      rules = currentContest.getRules();

      if (contest.numberOfCandidates() < 1)
         gui.msgOK(JSError.buildMsg(JSError.FATAL, 1004));
      else if (contest.numberOfBallots() < 1)
         gui.msgOK(JSError.buildMsg(JSError.FATAL, 1005));
      else if (!rules.areValid())
         gui.msgOK(JSError.buildMsg(JSError.FATAL, 1008));
      else dataOK = true;

      if (dataOK) {
         if (invokedStandalone && rules.distributedCount)
            gui = new CPProGUI();
         switch (rules.system) {
            case Rules.IRV:
            case Rules.STV:
            	tallyEngine = new TallySTV();
               break;
            case Rules.MEASURE:
            case Rules.SIMPLE_TALLY:
            	tallyEngine = new TallySimpleCount();
               break;
            case Rules.IR:
            	tallyEngine = new TallyBottomsUp();
               break;
            default:
               throw new FatalDeveloperError();
         }
         tallyEngine.tally(contest, reporter, gui);
      }
   }

} // end class Engine
