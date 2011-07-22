package edu.brown.cs.bubbles.bgta;

import javax.naming.OperationNotSupportedException;
import javax.swing.event.DocumentEvent;

import org.jivesoftware.smack.packet.Message;

public class BgtaResourceSwitchingBubble extends BgtaBubble
{
   BgtaChat chat;
   BgtaManager man;
   
   public BgtaResourceSwitchingBubble(BgtaManager m, String username)
   {
      this(m.startChat(username));
      man = m;
   }
   
   private BgtaResourceSwitchingBubble(BgtaChat c)
   {
      super(c);
      chat = c;
   }
   
   @Override public void insertUpdate(DocumentEvent e)
   {
      super.insertUpdate(e);
      Message msg = (Message)chat.getLastMessage();
      if(msg == null)
      {
         return;
      }
      System.out.println("Msg from : " + msg.getFrom());
      try {
         if(!msg.getFrom().equals(chat.getChat().getParticipant()))
         {
            System.out.println("BgtaChat switching from " + chat.getChat().getParticipant() + " to " + msg.getFrom());
            chat.setChat(man.startChat(msg.getFrom()).getChat());
         }
      } catch (OperationNotSupportedException e1) {
         // Do nothing
      }
   }

}
