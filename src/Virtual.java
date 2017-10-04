import java.util.ArrayList;

/**
 * General purpose class for communications-only agents
 *
 * Currently a stub with no functionality
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
                i.p.clear();
                i.q.clear();
            
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
        for(Intel i: intel ) {
            if(i.agent_id == id) {
                return i;
            }           
        }
        throw new RuntimeException("No intel for agent with id "+id);
    }
}

