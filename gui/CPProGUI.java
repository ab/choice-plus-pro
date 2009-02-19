

package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import java.io.*;
import java.util.Vector;

import engine.Engine;
import engine.CPProProperties;
import engine.RoundResult;
import reporter.CPProReporter;
import util.Library;
import util.LiteralString;

/** <pre>
   This is CPPro's standard GUI.
   @see ICPProGUI, CPProNoGui
 */

 public class CPProGUI extends JFrame implements ICPProGUI
{
	private static final int FRAME_WIDTH = 640;
	private static final int FRAME_HEIGHT = 280;

	Container frameContentPane = null;
	JLabel statusLine = null;
	File selectedFile = null;
   JProgressBar progressBar = null;

   /** Emits an error msg */
   public void msgOK(String str)
   {
      JOptionPane.showMessageDialog(null, str, "Info", JOptionPane.INFORMATION_MESSAGE);
   }

   /** Emit a dialog, in which the user is to pick
    *  the loser in a tie or the loser or winner in a distributed
    *  election -- the next candidate or candidates
    *  to defeat.
    *  Implements method in ICPProGUI
    */
   public Vector selectCandidate(String resFileTitle, String resFileInst, Vector candidates,
      boolean cancelAllowed, boolean multiSelect)
   {
//  	SelectCandidateDialog dlg = new SelectCandidateDialog(this, LiteralString.gets("gui.breakTie.title"),true);
   	SelectCandidateDialog dlg = new SelectCandidateDialog(this, LiteralString.gets(resFileTitle),true,false);
      dlg.init(resFileInst, candidates, cancelAllowed, multiSelect);
      Vector chosen = dlg.start();
      repaint(); // JAVABUG? -- clean up the screen
      return chosen;
   }

   /**
    *  When a round is complete, this method is called.
    *  This gives the UI a chance to show round-by-round results,
    *  and, if desired, to hold up the election until ready to
    *  continue.
    *  The program will only continue when this method is done
    *  and returns 'true'.  A return of 'false' means to abort the
    *  election.
    */
   public boolean onRoundComplete(RoundResult results)
   {
   	System.out.println("Round " + results.getRound() + " complete.");

      ///statusLine.setText(LiteralString.gets("gui.status.roundComplete", results.getRound()));
      ///statusLine.repaint();//FIXIT!  Why doesn't this progress bar work???
      ///progressBar.setString(LiteralString.gets("gui.status.roundComplete", results.getRound()));
      return true;
   }

   /**
    *  This method is called when the election is complete.
    */
   public void onContestComplete()
   {
   	System.out.println("Program Complete");//FIXIT -- progress bar not working...
   	try {
			String msg = LiteralString.gets("msg.contestComplete", Engine.reporter.getOutputDirectory());
      	statusLine.setText(msg);
   		msgOK(msg);
      }
		catch (Exception e) {}
   }

   /**
    * REQUIRED METHOD.
    * This method is called each time a ballot is loaded, with
    * the total number of ballots that have been loaded as the
    * parameter.
    */
   public void ballotLoaded(int totalCount, boolean done)
   {
   	// TEMPORARY CODE
      final int SHOW_COUNT = 100;
      StringBuffer buf = new StringBuffer();

      if (done && totalCount > 0) {
         System.out.println();
         buf.append(totalCount + " ballots loaded.  Load complete.");
         System.out.println(buf);
      }
      else if (totalCount > 0 && totalCount % SHOW_COUNT == 0) {
         if (totalCount == SHOW_COUNT)
            buf.append("Loaded " + totalCount + " ballots...");
         else buf.append(totalCount + "...");
         System.out.println(buf);
      }
/*		FIXIT -- progress bar not working!
      if (done && totalCount > 0)
         ;///statusLine.setText(LiteralString.gets("gui.status.ballotsDone",totalCount));
		else ;///statusLine.setText(LiteralString.gets("gui.status.ballotCount",totalCount));

      else if (totalCount > 0 && totalCount % SHOW_COUNT == 0) {
         if (totalCount == SHOW_COUNT)
            buf.append("Loaded " + totalCount + " ballots...");
         else buf.append(totalCount + "...");
*/
   }

   /** Handle window closes other than File+Exit */
  	private void setupWindowClose()
  	{
    	this.addWindowListener( new WindowAdapter() {
      	public void windowClosing(WindowEvent e) {System.exit(0);}
      });
  	}

   /** Start off the gui */
   public void start()
   {
      Engine.reporter = new CPProReporter();
      this.setTitle(LiteralString.gets("version.name"));
      this.setSize(FRAME_WIDTH, FRAME_HEIGHT);
      frameContentPane = getContentPane();

      setupWindowClose();

      setupMenu();

    	statusLine = new JLabel();
    	frameContentPane.add(statusLine, BorderLayout.SOUTH);

      // Now show the frame and menu
    	this.setVisible(true);
   }

   // Set up the menu
   private void setupMenu()
   {
    	// Create Menu Bar
    	JMenuBar menuBar = new JMenuBar();
    	setJMenuBar(menuBar);

    	// Create File, Election, and About menus
    	JMenu fileMenu = new JMenu(LiteralString.gets("gui.menu.file"));
    	fileMenu.setMnemonic(KeyEvent.VK_F);
      JMenu contestMenu = new JMenu((LiteralString.gets("gui.menu.contest")));
    	contestMenu.setMnemonic(KeyEvent.VK_C);
      JMenu helpMenu = new JMenu((LiteralString.gets("gui.menu.help")));
    	helpMenu.setMnemonic(KeyEvent.VK_H);

    	JMenuItem item;
    	// Create a menu item File + Exit, accelerator E
    	// Have it call onExitCommand() when selected
    	fileMenu.add (item = new JMenuItem (LiteralString.gets("gui.menuItem.exit")));
    	item.setMnemonic (KeyEvent.VK_E);
    	item.addActionListener (
    		new ActionListener() {
    			public void actionPerformed (ActionEvent e) {
    				onExitCommand();
    			}
    		}
    	);
      menuBar.add(fileMenu);

    	// Create a menu item Contest + Load & Tally, accelerator L
    	// Have it call onLoadTallyCommand() when selected
    	contestMenu.add (item = new JMenuItem (LiteralString.gets("gui.menuItem.loadTally")));
    	item.setMnemonic (KeyEvent.VK_L);
    	item.addActionListener (
      	new ActionListener() {
      		public void actionPerformed (ActionEvent e) {
      			onLoadTallyCommand();
	      	}
    		}
      );
      menuBar.add(contestMenu);

    	// Create a menu item Help + About, accelerator A
    	// Have it call onAboutCommand() when selected
    	helpMenu.add (item = new JMenuItem (LiteralString.gets("gui.menuItem.about")));
    	item.setMnemonic (KeyEvent.VK_A);
    	item.addActionListener (
      	new ActionListener() {
      		public void actionPerformed (ActionEvent e) {
      			onAboutCommand();
	      	}
    		}
      );
      menuBar.add(helpMenu);

      // Note: the menu is now set up, and will show up when the frame
      // it is on is set visible.
   }

  	void onExitCommand ()
   {
    	System.exit(0);
  	}

   /** <pre>
    *  For the FileChooser dialog:
    *		1. Use the last selected directory, if there is one, else
    *    2. Use the FileOpenStart property: *cwd*, *home*, or a directory name.
    *		3. Use the home file directory.
    *  </pre>
    */
  	void onLoadTallyCommand()
   {
   	JFileChooser filechooser = null;
      File startingDir = Engine.reporter.getOutputDirectory();
      String startingDirName = null;

      if (startingDir == null) {
      	startingDirName = CPProProperties.getFileOpenStart();
         if (startingDirName == null || startingDirName.equals("*home*"))
         	startingDir = Library.getFullDirectory(CPProProperties.getHomeFileDir());
         else {
         	if (startingDirName.equals("*cwd*"))
         		startingDir = Library.getFullDirectory("foo"); // A cheap trick to get the cwd
            else {
            	startingDir = new File(startingDirName);
               if (startingDir == null)
	         		startingDir = Library.getFullDirectory(CPProProperties.getHomeFileDir());
            }
      	}
      }

 		filechooser = new JFileChooser(startingDir);

      String fileOpenExts = (CPProProperties.getFileOpenExtension()).trim();
      String [] ExtArray = new String [5];
      int i = -1;
      int j = 0;
      while (j<4 && (i = fileOpenExts.indexOf(' ')) > 0) {
         ExtArray[j++] = fileOpenExts.substring(0,i);
         fileOpenExts = fileOpenExts.substring(++i);
      }
      ExtArray[j] = fileOpenExts;
      filechooser.addChoosableFileFilter(new ExtensionFileFilter(ExtArray, "ChoicePlus Input"));

    	if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
         selectedFile = filechooser.getSelectedFile();
/*
	      progressBar = new JProgressBar();
   	   progressBar.setStringPainted(true);
      	progressBar.setBorderPainted(true);
         frameContentPane.add(progressBar);
         progressBar.setVisible(true);
*/
	      Engine engine = new Engine();

         try {
		      engine.start(selectedFile.getCanonicalPath());
         }
         catch (IOException e) {
         	System.out.println("ERROR in getting filename");//FIXIT!  Do something better here
         }
      }
      repaint();  // JAVABUG? -- otherwise the menu appears to stay open!
  	}

	void onAboutCommand()
   {
   	JDialog dlg = new AboutDialog(this,LiteralString.gets("gui.about.title"),true);
      dlg.setVisible(true);
      repaint(); // JAVABUG? -- otherwise, the menu appears to stay open
   }


   /**
    *	Shows results for the current round.
    * Shows candidate name, change this round, and total.
	 *
    * If it is a distributed election, then show "Select Winner" and "Select Loser"
    * buttons.  Otherwise show "Continue" button (a feature Cambridge wanted).
    *
    * IMPLEMENTS ICPProGUI.
    */
   public int showRoundResults(int currentRound, Vector sortedCands, boolean isDistributed)
   {
    	ShowRoundResultsDialog dlg = new ShowRoundResultsDialog( this,
      		LiteralString.gets("gui.showRoundResults.title", currentRound), isDistributed );
      dlg.init(sortedCands, "gui.showRoundResults.instructions.");
      int buttonChosen = dlg.start();
      repaint(); // JAVABUG? -- clean up the screen
      return buttonChosen;
   }

}
