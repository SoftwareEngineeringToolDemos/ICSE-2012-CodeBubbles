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


import edu.brown.cs.bubbles.bgta.*;


class BeduTAChatBubble extends BgtaBubble
{
private static final long serialVersionUID = 1L;

private BeduTAXMPPClient client;
private BgtaChat chat;

BeduTAChatBubble(BeduTAXMPPClient a_client, BgtaChat a_chat)
{
   super(a_chat);
   client = a_client;
   chat = a_chat;
}


@Override
public void setVisible(boolean vis)
{
   //when this chat window is closed make sure to cut 
   //off the student 
   super.setVisible(vis);
   client.endChatSession(chat);
}
}

