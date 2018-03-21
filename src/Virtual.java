import java.util.ArrayList;
import java.util.HashMap;

/**
 * Abstract class for communications-only agents that
 * are not connected to the electic portion of the grid.
 *
 * Contains functionality to reset intel for new runs
 * and retrieve known intel for a given agent.
 */
public abstract class Virtual extends Agent {
    
    // Local counter for trading period
    int period;
    
    // Channels the virtual agent has access to
    ArrayList<Channel> channels;
    
    // Id of grid agents that virtual agent has access to
    ArrayList<Integer> agents;
    
    // Set of information that virtual agent has
    ArrayList<Intel> intel;
    
    // Configuration parameters for virtual agent's behavior
    HashMap<String, String> config = new HashMap<>();

    /**
     * Constructor
     *
     * Creates virtual agent and initializes period
     */
    Virtual(int own_id) {
        super(own_id);
        this.period = 1;
        this.channels = new ArrayList<Channel>();
        this.agents = new ArrayList<Integer>();
        this.intel = new ArrayList<Intel>();
    }
    
    /** 
     * Initialize for a new population
     */
    @Override 
    public void popInit() {
        super.popInit();
    }

    /** 
     * Reset at the beginning of a DOS run
     */
    @Override
    public void runInit() {
        super.runInit();
        period = 1;
        resetIntel(Env.getHistory(Env.getPop(), Integer.parseInt(Env.curDOS)));
    }
    
    /** 
     * Reset at the beginning of a DOS run
     */
    private void resetIntel(ArrayList<History> history){
        
        // Loop through intel
        for(Intel i: intel ) {
            
            // If learned during run, remove else reset
            if (i.learned) {
                intel.remove(i);
            }
            else {
            
                // Reset price, quantity and bids
                i.bid.clear();
                i.history.p.clear();
                i.history.q.clear();
            
                // Initialize p and q from global intel
                for(History h: history) {
                    if (h.agent_id == i.agent_id){
                        i.storeHistory(h);
                    }
                }
            }
        }
    }
    
    /** 
     * Get intel for given agent
     */
    private Intel getIntel(Integer id){
        
        // Loop through intel
        for(Intel i: intel) {
            if(i.agent_id == id) {
                return i;
            }           
        }
        throw new RuntimeException("No intel for agent with id " + id);
    }
    
    /** 
     * Get configuration parameter for virtual agent
     */
    private String getConfig(String key){
        
        // Attempt to get parameter
        value = config.get(key);
        
        //Check if parameter exists
        if (value == null){
            throw new RuntimeException("No parameter named " + key + " for agent " + own_id);
        }
        else {
            return value
        }
    }
}

