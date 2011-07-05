package edu.brown.cs.bubbles.bgta.educhat;

import javax.swing.JComponent;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPConnection;

import edu.brown.cs.bubbles.bgta.*;

class EduchatBubble extends BgtaBubble{
   EduchatBubble(XMPPConnection conn, Chat c)
   {
      super(BgtaUtil.bgtaChatForXMPPChat(conn, c));
   }
   
   @Override public void setVisible(boolean vis)
   {
      ((JComponent)this).setVisible(vis);
   }
}
