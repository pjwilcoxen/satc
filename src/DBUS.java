import java.util.HashMap;

/**
 * Basic communications bus
 */
public class DBUS {

    private static final HashMap<String, DBUS> busList = new HashMap<>();
    
    String name;

    /**
     * Find a bus by name or return null
     * 
     * @param name Name of bus
     * @return Bus instance
     */
    public static DBUS find(String name) {
        return busList.get(name);
    }

    /**
	 * Construct a new DBUS instance
	 *
	 * @param name Name of the bus
	 */
    public DBUS(String name){
        if( busList.containsKey(name) )
            throw new RuntimeException("Redundant dbus instantiation");
        this.name = name; 
        busList.put(name,this);
    }
    
    /**
     * Send a message
     *
     * @param msg Message to send
     */
    public void send(Msg msg) {
        if( ! Env.isBlocked(msg.dos_id,msg.from) )
            Env.getAgent(msg.to).deliver(msg);
    }
}
