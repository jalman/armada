package examplejurgzplayer.messaging;

import examplejurgzplayer.messaging.MessagingSystem.MessageType;

public class Message {
  public static final int MAX_SIZE = 10;

  public MessageType type;
  public int[] message = new int[MAX_SIZE];
}
