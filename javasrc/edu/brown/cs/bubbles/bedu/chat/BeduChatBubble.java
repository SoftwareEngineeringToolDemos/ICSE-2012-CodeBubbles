/********************************************************************************/
/*                         						 								*/
/*    		BeduChatBubble.java                 								*/
/*                            													*/
/* 	Bubbles for Education   													*/
/* 	A chat bubble that can be constructed from basic smack						*/
/* 	classes for use in BeduChat			      									*/
/* 				               													*/
/********************************************************************************/
/* 	Copyright 2011 Brown University -- Andrew Kovacs         					*/
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

package edu.brown.cs.bubbles.bedu.chat;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPConnection;

import edu.brown.cs.bubbles.bgta.*;


class BeduChatBubble extends BgtaBubble
{
private static final long serialVersionUID = 1L;



private BeduChatBubble(XMPPConnection conn, Chat c)
{
	super(BgtaUtil.bgtaChatForXMPPChat(conn, c));
}



static class TABubble extends BeduChatBubble
{
	private static final long serialVersionUID = 1L;

	private BeduTAXMPPClient client;
	private Chat chat;

	TABubble(BeduTAXMPPClient a_client, Chat a_chat)
	{
		super(a_client.getConnection(), a_chat);
		client = a_client;
		chat = a_chat;
	}
	
	
	
	@Override
	public void setVisible(boolean vis)
	{
		super.setVisible(vis);
		client.endChatSession(chat);
	}
}
}
