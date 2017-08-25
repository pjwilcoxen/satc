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
            Env.msg.println(LOGHEADER);
            initLog = false;
        }
    }
    
    /**
     * Reset diversion maps for new populations
     */
    public static void divert_clear() {
        divertTo.clear();
        divertFrom.clear();
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
            Env.msg.println(who+"blocked");
            return;
        }
        
        if( divertFrom.containsKey(msg.from) ) {
            new_id = divertFrom.get(msg.to);
            Env.msg.println(who+"diverted (from) to "+new_id);
            Env.getAgent(new_id).deliver(msg);
            return;
        }

        if( divertTo.containsKey(msg.to) ) {
            new_id = divertTo.get(msg.to);
            Env.msg.println(who+"diverted (to) to "+new_id);
            Env.getAgent(new_id).deliver(msg);
            return;
        }
        
        Env.getAgent(msg.to).deliver(msg);
        Env.msg.println(who+"delivered");
    }
    
    /**
     * Inject a message downstream from diversions
     * 
     * @param msg Message to deliver 
     */
    public void inject(Msg msg) {
        String who = name+","+msg.logString()+",";
        Env.getAgent(msg.to).deliver(msg);
        Env.msg.println(who+"injected");
    }
    
}
