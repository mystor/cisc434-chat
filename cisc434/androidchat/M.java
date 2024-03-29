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
    public static class RecepientFactory {
        public static Recepient parseChannel(String s) {
            if (!s.matches("#[a-zA-Z0-9]+")) {
                return null;
            }
            return new ChannelRecepient(s.substring(1));
        }

        public static Recepient parse(String s) {
            if (s.startsWith("#")) {
                return new ChannelRecepient(s.substring(1));
            } else {
                String[] parts = s.split(",");
                TreeSet<String> rs = new TreeSet<>();
                for (String part : parts) {
                    rs.add(part.trim());
                }
                return new DMRecepient(rs);
            }
        }
    }
    public interface Recepient extends Serializable {
    }
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
            if (!(o instanceof DMRecepient)) {
                return false;
            }
            DMRecepient or = (DMRecepient) o;
            return or.recepients.equals(recepients);
        }

        @Override
        public String toString() {
            Iterator<String> iter = recepients.iterator();
            String s = iter.next();
            while (iter.hasNext()) {
                String it = iter.next();
                s += ", ";
                s += it;
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
            if (!(o instanceof ChannelRecepient)) {
                return false;
            }
            ChannelRecepient or = (ChannelRecepient) o;
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
        public Recepient recepient;
    }

    /**
     * A listing of the users in a channel
     */
    public static class ListUsers implements Serializable {
        public Recepient recepient;
        public TreeSet<String> users;
    }

    public static class ListAllChannelsReq implements Serializable {}
    public static class ListAllChannels implements Serializable {
        public ArrayList<String> channels;
    }

    public static class DMUsersReq implements Serializable {}
    public static class DMUsers implements Serializable {
        public ArrayList<String> users;
    }

    public static class LeaveChannel implements Serializable {
        public Recepient recepient;
    }
    public static class LeftChannel implements Serializable {
        public Recepient recepient;
    }
}
