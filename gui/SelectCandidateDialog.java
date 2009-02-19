package  gui;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;


import util.*;
// import com.borland.jbcl.layout.*;

public class SelectCandidateDialog extends JDialog
{
  	JPanel mainPanel = new JPanel();
  	JPanel buttonPanel = new JPanel();
  	JButton okButton = new JButton();
  	JButton cancelButton = new JButton();
  	Border border1;
	BorderLayout borderLayout = new BorderLayout();
	FlowLayout flowLayout = new FlowLayout();
	JList candidateList = new JList();
	JTextArea instructions = new JTextArea();

   LiteralString res = new LiteralString();


   Vector start()
   {
   	setVisible(true); //starts up the dialog
      return new Vector( Library.makeVector(candidateList.getSelectedValues()) );
   }

   void init(String resFileID, Vector candidates,
      boolean cancelAllowed, boolean multiSelect)
   {
      Vector lines = new Vector();
//      res.appendStringSet("gui.breakTie.instructions.", lines);
      res.appendStringSet(resFileID, lines);
   	instructions.setEditable(false);
      for (int i=0; i<lines.size(); i++) {
         instructions.append("  ");
		   instructions.append((String)lines.elementAt(i));
         instructions.append("\n");
		}
      candidateList.setListData(candidates);
   }

  	public SelectCandidateDialog(Frame frame, String title, boolean modal,
      boolean cancelAllowed)
   {
    	super(frame, title, modal);
    	try {
      	jbInit(cancelAllowed);
    	}
    	catch (Exception e) {
      	e.printStackTrace();
    	}

    	pack();
  	}

  	private void jbInit(boolean cancelAllowed) throws Exception
  	{
  		border1 = BorderFactory.createRaisedBevelBorder();
    	mainPanel.setLayout(borderLayout);
    	buttonPanel.setLayout(flowLayout);
    	okButton.setText("OK");
    	okButton.addActionListener(new SelectCandidateDialog_okButton_actionAdapter(this));
		buttonPanel.add(okButton);
    	if (cancelAllowed) {
    		cancelButton.setText("Cancel");
    		cancelButton.addActionListener(new SelectCandidateDialog_cancelButton_actionAdapter(this));
    		buttonPanel.add(cancelButton);
    	}
    	this.addWindowListener(new SelectCandidateDialog_this_windowAdapter(this));
    	candidateList.setBorder(BorderFactory.createRaisedBevelBorder());
		mainPanel.setMinimumSize(new Dimension(300, 300));
		mainPanel.setPreferredSize(new Dimension(300, 300));
		instructions.setMargin(new Insets(50, 50, 50, 50));
		instructions.setBorder(BorderFactory.createLineBorder(Color.black));
		mainPanel.add(instructions, BorderLayout.NORTH);
		mainPanel.add(candidateList, BorderLayout.CENTER);
		mainPanel.add(buttonPanel,BorderLayout.SOUTH);
    	getContentPane().add(mainPanel);
	}

  	// OK
  	void okButton_actionPerformed(ActionEvent e)
   {
    	dispose();
  	}

  	// Cancel
  	void cancelButton_actionPerformed(ActionEvent e)
   {
    	dispose();
  	}

  	void this_windowClosing(WindowEvent e)
   {
    	dispose();
  	}

} // end class SelectCandidateDialog

//===============================================================
// Secondary class
//===============================================================
class SelectCandidateDialog_okButton_actionAdapter implements ActionListener
{
  	SelectCandidateDialog adaptee;

 	SelectCandidateDialog_okButton_actionAdapter(SelectCandidateDialog adaptee)
   {
    	this.adaptee = adaptee;
  	}

  	public void actionPerformed(ActionEvent e)
   {
    	adaptee.okButton_actionPerformed(e);
  	}
} // end class SelectCandidateDialog_okButton_actionAdapter

//===============================================================
// Another secondary class
//===============================================================
class SelectCandidateDialog_cancelButton_actionAdapter implements ActionListener
{
  	SelectCandidateDialog adaptee;

  	SelectCandidateDialog_cancelButton_actionAdapter(SelectCandidateDialog adaptee)
   {
    	this.adaptee = adaptee;
  	}

  	public void actionPerformed(ActionEvent e)
   {
    	adaptee.cancelButton_actionPerformed(e);
  	}
} // end class SelectCandidateDialog_cancelButton_actionAdapter

//===============================================================
// Another secondary class
//===============================================================
class SelectCandidateDialog_this_windowAdapter extends WindowAdapter
{
  	SelectCandidateDialog adaptee;

  	SelectCandidateDialog_this_windowAdapter(SelectCandidateDialog adaptee)
   {
    	this.adaptee = adaptee;
  	}

  	public void windowClosing(WindowEvent e)
   {
    	adaptee.this_windowClosing(e);
  	}
}

