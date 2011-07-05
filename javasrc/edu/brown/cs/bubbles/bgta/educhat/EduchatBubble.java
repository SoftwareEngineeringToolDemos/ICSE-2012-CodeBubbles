package edu.brown.cs.bubbles.bgta.educhat;

import javax.swing.JComponent;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPConnection;

import edu.brown.cs.bubbles.bgta.*;

public class EduchatBubble extends BgtaBubble{
   private EduchatBubble(XMPPConnection conn, Chat c)
   {
      super(BgtaUtil.bgtaChatForXMPPChat(conn, c));
   }
   
   public static class TABubble extends EduchatBubble {
      private TAXMPPClient client;
      private Chat chat;
      
      public TABubble(TAXMPPClient a_client, Chat a_chat)
      {
	 super(a_client.getConnection(), a_chat);
	 client = a_client;
	 chat = a_chat;
      }
      
      @Override public void setVisible(boolean vis)
      {
          super.setVisible(vis);
          client.endChatSession(chat);
      }
   }
}
