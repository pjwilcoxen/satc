import java.util.HashMap;

/**
 * Basic communications channel
 */
public class Channel {

    private static final HashMap<String, Channel> channelList = new HashMap<>();
    private static final HashMap<Integer, Integer> divertList = new HashMap<>();
    
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
     * Configure a diversion of messages in transit
     * 
     * @param old_id ID of original recipient
     * @param new_id ID of new node to receive it instead
     */
    public void divert(int old_id, int new_id) {
        if( divertList.containsKey(old_id) )
            throw new RuntimeException("Redundant diversion for recipient "+old_id);
        divertList.put(old_id,new_id);
    }
    
    /**
     * Send a message
     *
     * @param msg Message to send
     */
    public void send(Msg msg) {
        String who = name+","+msg.logString()+",";
        
        if( Env.isBlocked(msg.from) ) {
            Env.msg.println(who+"blocked");
            return;
        }
        
        if( divertList.containsKey(msg.to) ) {
            int target = divertList.get(msg.to);
            Env.msg.println(who+"diverted to "+target);
            Env.getAgent(target).deliver(msg);
            return;
        }
        
        Env.getAgent(msg.to).deliver(msg);
        Env.msg.println(who+"delivered");
    }
}
