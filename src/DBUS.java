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

    //
    //  Constructor
    //
    
    public DBUS(String name){
        if( busList.containsKey(name) )
            throw new RuntimeException("Redundant dbus instantiation");
        this.name = name; 
        busList.put(name,this);
    }
    
    //  Check: 
    //     Add a message object and collapse these methods to one.
    //     That is, DBUS sends messages but leaves it up to the
    //     recipient to figure out what to do with them

    public void toQueue(Bidstep[] bids, int drop, int to) {
        Env.getAgent(to).appendQueueD(bids, drop);
        
    }     
      
    public void toQueue(int bl, int drop, int to) {
        Env.getAgent(to).setBl(bl, drop);
    }
    
}
