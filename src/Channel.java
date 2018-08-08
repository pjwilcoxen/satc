import java.util.HashMap;

/**
 * Basic communications channel
 */
public class Channel {

    private static final HashMap<String, Channel> channelList = new HashMap<>();
    private static final HashMap<Integer, Integer> divertTo = new HashMap<>();
    private static final HashMap<Integer, Integer> divertFrom = new HashMap<>();

    /**
     * Header for log files
     */
    public static final String LOGHEADER = "channel,"+Msg.LOGHEADER+",status";

    private static boolean initLog = true;

    String name;

    /**
     * Find a channel by name or return null
     *
     * @param name Name of channel
     * @return Channel instance
     */
    public static Channel find(String name) {
        return channelList.get(name);
    }

    /**
     * Construct a new Channel instance
     *
     * @param name Name of the channel
     */
    public Channel(String name){
        if( channelList.containsKey(name) )
            throw new RuntimeException("Redundant channel instantiation");
        this.name = name;
        channelList.put(name,this);
        if( initLog ) {
            Env.msg.print(LOGHEADER+"\n");
            initLog = false;
        }
    }

    /**
    * Reset diversion maps for new populations
    */
    public static void divert_clear() {
        divertTo.clear();
        divertFrom.clear();
        Util.debug("diversions cleared");
    }

    /**
     * Divert messages based on recipient
     *
     * @param to_id ID of original recipient
     * @param new_id ID of new node to receive it instead
     */
    public void divert_to(int to_id, int new_id) {
        if( divertTo.containsKey(to_id) )
            throw new RuntimeException("Redundant diversion for recipient "+to_id);
        divertTo.put(to_id,new_id);
        Util.debug("divert_to "+to_id+" -> "+new_id);
    }

    /**
     * Divert messages based on sender
     *
     * @param from_id ID of original sender
     * @param new_id ID of new node to receive it
     */
    public void divert_from(int from_id, int new_id) {
        if( divertFrom.containsKey(from_id) )
            throw new RuntimeException("Redundant diversion for recipient "+from_id);
        divertFrom.put(from_id,new_id);
        Util.debug("divert_from "+from_id+" -> "+new_id);
    }

    /**
     * Send a message
     *
     * @param msg Message to send
     */
    public void send(Msg msg) {
        String who;
        int new_id;

        who = name+","+msg.logString()+",";

        if( Env.isBlocked(msg.from) ) {
            Env.msg.print(who+"blocked\n");
            return;
        }

        if( divertFrom.containsKey(msg.from) ) {
            new_id = divertFrom.get(msg.from);
            Env.msg.print(who+"diverted (from) to "+new_id+"\n");
            Env.getAgent(new_id).deliver(msg);
            return;
        }

        if( divertTo.containsKey(msg.to) ) {
            new_id = divertTo.get(msg.to);
            Env.msg.print(who+"diverted (to) to "+new_id+"\n");
            Env.getAgent(new_id).deliver(msg);
            return;
        }

        Env.getAgent(msg.to).deliver(msg);
        Env.msg.print(who+"delivered\n");
    }

    /**
     * Inject a message downstream from diversions
     *
     * @param msg Message to deliver
     */
    public void inject(Msg msg) {
        String who = name+","+msg.logString()+",";
        if( Env.isBlocked(msg.from) ) {
            Env.msg.print(who+"blocked\n");
            Util.debug("injected message from "+msg.from+" blocked");
        }
        else {
            Env.getAgent(msg.to).deliver(msg);
            Env.msg.print(who+"injected\n");
            Util.debug("injected message from "+msg.from+" delivered");
        }
    }

}
