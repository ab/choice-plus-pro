//Title:        ChoicePlus Pro
//Version:
//Copyright:    Copyright (c) 2007
//Author:       Jim Lindsay and Steve Willett
//Company:      Voting Solutions
//Description:

package gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;

import util.Library;
import util.LiteralString;

public class AboutDialog extends JDialog
{
	private static final int ABOUT_WIDTH = 30;
   private static final int ABOUT_DASHES = 75;

	JPanel panel = new JPanel();
	BorderLayout borderLayout = new BorderLayout();
   LiteralString res = new LiteralString();

	public AboutDialog(Frame frame, String title, boolean modal) {
		super(frame, title, modal);
		try  {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public AboutDialog() {
		this(null, "", false);
	}

	void jbInit() throws Exception {
		panel.setLayout(borderLayout);
		getContentPane().add(panel, BorderLayout.CENTER);
      setText();
	}


  	void setText()
   {
      Vector lines = new Vector();
      lines.addElement("");
      lines.addElement(LiteralString.gets("version.name"));
      lines.addElement(LiteralString.gets("version.number"));
      lines.addElement(Library.repeatChars('-', ABOUT_DASHES));

      res.appendStringSet("gui.about.a", lines);
      lines.addElement(Library.repeatChars('-', ABOUT_DASHES));

      res.appendStringSet("gui.about.b", lines);

      // We now have all the strings in the Vector 'lines'.
      // The trick is to get the damn things to show up...
		JTextArea area = new JTextArea(lines.size(), ABOUT_WIDTH);
      area.setEditable(false);
      for (int i = 0; i < lines.size(); i++) {
      	String str = (String)lines.elementAt(i);
         area.append("   ");
      	area.append(str);
         area.append("\n");

	    	///label = new JLabel();
         ///label.setText(str);
         ///label.setLocation(i+20,i+15);
   	   ///panel.add(label);
      }
      panel.add(area);
      ///frameContentPane.add(area, BorderLayout.CENTER);

  		///System.out.println(area.getText());
      //area.setVisible(true);
      //area.repaint();
      ///area.setTitle(LiteralString.gets("gui.about.title"));
  	}


}
