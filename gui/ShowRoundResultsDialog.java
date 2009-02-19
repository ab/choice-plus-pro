package gui;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;

import engine.*;
import util.*;
// import com.borland.jbcl.layout.*;

public class ShowRoundResultsDialog extends JDialog
{
  	JPanel mainPanel = new JPanel();
  	JPanel buttonPanel = new JPanel();
  	JButton winnerButton = new JButton();
  	JButton loserButton = new JButton();
  	JButton continueButton = new JButton();

  	Border border1;
	BorderLayout borderLayout = new BorderLayout();
	FlowLayout flowLayout = new FlowLayout();

	JTextArea instructions = new JTextArea();
	JTextArea table = new JTextArea();
	JScrollPane scrollPane = new JScrollPane(table);

	LiteralString res = new LiteralString();

	int buttonSelected = -1;

  	public ShowRoundResultsDialog(Frame frame, String title, boolean isDistributed)
   {
   	super(frame, title, true);
    	try {
      	jbInit(isDistributed);
    	}
    	catch (Exception e) {
      	e.printStackTrace();
    	}

    	pack();
  	}

   void init(Vector sortedCands, String resFileID)
   {
      final int[] columnSizes = {30, 6, 20};
      Vector lines = new Vector();

      // Sets up instructions text area
      res.appendStringSet(resFileID, lines);
      instructions.setEditable(false);
      for (int i=0; i<lines.size(); i++) {
    	  instructions.append("   ");
		  instructions.append((String)lines.elementAt(i));
		  instructions.append("\n");
      }

		// Sets up round results
		///Vector candRoundResults = results.getByStanding();
      table.setEditable(false);
      table.setLineWrap(true);
      table.append("     name                           total   status\n");
      for (int candNdx = 0; candNdx < sortedCands.size(); candNdx++) {
    	  Candidate cand = (Candidate)sortedCands.elementAt(candNdx);
    	  String data = cand.getName();
    	  if (data.length() > columnSizes[0])
    		  data = data.substring(0,columnSizes[0]-1) + '.';
    	  else if (data.length() < columnSizes[0])
    		  data = data + Library.repeatChars(' ', columnSizes[0] - data.length());
    	  table.append("  " + data + "  ");
          data = cand.getVotes().toString();
          data = Library.stripZeros(data);
          data = Library.repeatChars(' ', columnSizes[1] - data.length()) + data;
          table.append(data + "  ");
          table.append(Candidate.statusToString(cand.getStatus()));
          table.append("\n");
      }
   }

   int start()
   {
   	setVisible(true); //starts up the dialog
      return buttonSelected;
   }

  	private void jbInit(boolean isDistributed) throws Exception
  	{
    	border1 = BorderFactory.createRaisedBevelBorder();
    	buttonPanel.setLayout(flowLayout);
    	if (isDistributed) {
    		winnerButton.setText(LiteralString.gets("gui.showRoundResults.winnerButton"));
    		winnerButton.addActionListener(new ShowRoundResultsDialog_winnerButton_actionAdapter(this));
    		loserButton.setText(LiteralString.gets("gui.showRoundResults.loserButton"));
    		loserButton.addActionListener(new ShowRoundResultsDialog_loserButton_actionAdapter(this));
    	} else {
	    	continueButton.setText(LiteralString.gets("gui.showRoundResults.continueButton"));
	    	continueButton.addActionListener(new ShowRoundResultsDialog_continueButton_actionAdapter(this));
    	}

    	this.addWindowListener(new ShowRoundResultsDialog_this_windowAdapter(this));
    	mainPanel.setLayout(borderLayout);
    	table.setBorder(BorderFactory.createRaisedBevelBorder());

    	Font font = new Font("Monospaced", Font.PLAIN, 12);
    	table.setFont(font);

		mainPanel.setMinimumSize(new Dimension(400, 400));
		mainPanel.setPreferredSize(new Dimension(400, 400));
		instructions.setMargin(new Insets(50, 50, 50, 50));
		instructions.setBorder(BorderFactory.createLineBorder(Color.black));

		mainPanel.add(instructions, BorderLayout.NORTH);
    	if (isDistributed) {
    		buttonPanel.add(loserButton);
    		buttonPanel.add(winnerButton);
    	} else 
    		mainPanel.add(continueButton);

		mainPanel.add(scrollPane, BorderLayout.EAST);
    	getContentPane().add(mainPanel);
	}

  	void winnerButton_actionPerformed(ActionEvent e)
   {
   	buttonSelected = ICPProGUI.SELECT_WINNER_BUTTON;
    	dispose();
  	}

  	void loserButton_actionPerformed(ActionEvent e)
   {
   	buttonSelected = ICPProGUI.SELECT_LOSER_BUTTON;
    	dispose();
  	}

  	void continueButton_actionPerformed(ActionEvent e)
   {
   	buttonSelected = ICPProGUI.CONTINUE_BUTTON;
    	dispose();
  	}

  	void this_windowClosing(WindowEvent e)
   {
    	dispose();
  	}

} // end class ShowRoundResultsDialog

//===============================================================
// Secondary class
//===============================================================
class ShowRoundResultsDialog_winnerButton_actionAdapter implements ActionListener
{
  	ShowRoundResultsDialog adaptee;

 	ShowRoundResultsDialog_winnerButton_actionAdapter(ShowRoundResultsDialog adaptee)
   {
    	this.adaptee = adaptee;
  	}

  	public void actionPerformed(ActionEvent e)
   {
    	adaptee.winnerButton_actionPerformed(e);
  	}
} // end class ShowRoundResultsDialog_okButton_actionAdapter

//===============================================================
// Another secondary class
//===============================================================
class ShowRoundResultsDialog_loserButton_actionAdapter implements ActionListener
{
  	ShowRoundResultsDialog adaptee;

  	ShowRoundResultsDialog_loserButton_actionAdapter(ShowRoundResultsDialog adaptee)
   {
    	this.adaptee = adaptee;
  	}

  	public void actionPerformed(ActionEvent e)
   {
    	adaptee.loserButton_actionPerformed(e);
  	}
} // end class ShowRoundResultsDialog_loserButton_actionAdapter

//===============================================================
// Another secondary class
//===============================================================
class ShowRoundResultsDialog_continueButton_actionAdapter implements ActionListener
{
  	ShowRoundResultsDialog adaptee;

  	ShowRoundResultsDialog_continueButton_actionAdapter(ShowRoundResultsDialog adaptee)
   {
    	this.adaptee = adaptee;
  	}

  	public void actionPerformed(ActionEvent e)
   {
    	adaptee.continueButton_actionPerformed(e);
  	}
} // end class ShowRoundResultsDialog_loserButton_actionAdapter

//===============================================================
// Another secondary class
//===============================================================
class ShowRoundResultsDialog_this_windowAdapter extends WindowAdapter
{
  	ShowRoundResultsDialog adaptee;

  	ShowRoundResultsDialog_this_windowAdapter(ShowRoundResultsDialog adaptee)
   {
    	this.adaptee = adaptee;
  	}

  	public void windowClosing(WindowEvent e)
   {
    	adaptee.this_windowClosing(e);
  	}
}

