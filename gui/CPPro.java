

package gui;

import engine.CPProProperties;
import engine.Engine;    
import gui.CPProGUI;
import util.LiteralString;

public class CPPro
{
   public CPPro()
   {
   }

   public static void main(String[] args)
   {
      Engine.parseCommandLineParameters(args);
System.out.println("Loading " + CPProProperties.getTextPropertiesFileName());
      LiteralString.load(CPProProperties.getTextPropertiesFileName());
      Engine.gui = new CPProGUI();
      Engine.gui.start();
   }

}                    
