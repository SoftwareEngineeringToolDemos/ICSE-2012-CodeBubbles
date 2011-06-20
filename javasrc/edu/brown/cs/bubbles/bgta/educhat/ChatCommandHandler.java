package edu.brown.cs.bubbles.bgta.educhat;

import org.jivesoftware.smack.packet.Message;

public interface ChatCommandHandler {
   public void handleCommand(Message msg);
}
