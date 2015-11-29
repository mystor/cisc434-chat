package cisc434.androidchat;

import java.io.*;
import java.net.*;
import java.util.*;

public class M {
    public static class Login implements Serializable {
        public String username;
        public String password;
    }

    public static class LoggedIn implements Serializable {}

    /**
     * An interface representing the recepient of a given message
     * Either a DMRecepient or a ChannelRecepient. Other Recepients
     * are not supported
     */
    public interface Recepient extends Serializable {}
    public static class DMRecepient implements Recepient {
        public final TreeSet<String> recepients;

        public DMRecepient(TreeSet<String> r) {
            recepients = r;
        }

        @Override
        public int hashCode() {
            return recepients.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            DMRecepient or = (DMRecepient) o;
            if (or == null) {
                return false;
            }

            return or.recepients.equals(recepients);
        }

        @Override
        public String toString() {
            Iterator<String> iter = recepients.iterator();
            String s = iter.next();
            String it = iter.next();
            while (it != null) {
                s += ",";
                s += it;
                it = iter.next();
            }
            return s;
        }
    }
    public static class ChannelRecepient implements Recepient {
        public final String channel;

        public ChannelRecepient(String c) {
            channel = c;
        }

        @Override
        public int hashCode() {
            return channel.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            ChannelRecepient or = (ChannelRecepient) o;
            if (or == null) {
                return false;
            }

            return or.channel.equals(channel);
        }

        @Override
        public String toString() {
            return "#" + channel;
        }
    }

    /**
     * A request to join a channel
     */
    public static class JoinChannel implements Serializable {
        public Recepient recepient;
    }

    // XXX: Leave channel message?

    /**
     * A request to send a message
     */
    public static class SendMessage implements Serializable {
        public Recepient recepient;
        public String body;
    }

    /**
     * A message which has been sent
     */
    public static class RcvMessage implements Serializable {
        public Recepient recepient;
        public String username;
        public Date when;
        public String body;
    }

    /**
     * A request to list the users in a channel
     */
    public static class ListUsersReq implements Serializable {
        public String channel;
    }

    /**
     * A listing of the users in a channel
     */
    public static class ListUsers implements Serializable {
        public String channel;
        public TreeSet<String> users;
    }
}
