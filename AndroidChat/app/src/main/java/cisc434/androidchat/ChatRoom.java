package cisc434.androidchat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mlayzell on 2015-11-28.
 */
public class ChatRoom {
    public M.Recepient recepient;
    private ArrayList<M.RcvMessage> messages;

    public synchronized void addMessage(M.RcvMessage message) {
        messages.add(message);
    }

    public synchronized List<M.RcvMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    public ChatRoom(M.Recepient recepient) {
        this.recepient = recepient;
        this.messages = new ArrayList<>();
    }
}
