import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class for communications-only agents that
 * are not connected to the electric portion of the grid.
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
    HashMap<Integer, Intel> intel;

    // Configuration parameters for virtual agent's behavior
    HashMap<String, String> config;

    /** 
     * Find and create a virtual agent of a specific type
     *
     * @param type Type of agent to construct
     * @param id Id of the agent
     * @return New agent
     */
    public static Agent makeAgent(String type,int id) {
       switch(type) {
          case "ADV_ADAM":
             return new Adv_Adam(id);
          case "ADV_BETH":
             return new Adv_Beth(id);
          case "ADV_DARTH":
             return new Adv_Darth(id);
          case "ADV_ELVIRA":
             return new Adv_Elvira(id);
          default:
             throw new RuntimeException("Unexpected agent type "+type);
       }
    }

    /**
     * Constructor
     *
     * Creates virtual agent and initializes period
     */
    Virtual(int own_id) {
        super(own_id);
        this.period   = 1;
        this.channels = new ArrayList<>();
        this.agents   = new ArrayList<>();
        this.intel    = new HashMap<>();
        this.config   = new HashMap<>();
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
    private void resetIntel(HashMap<Integer, History> gHistory){

        // Loop through intel
        for(Map.Entry<Integer,Intel> entry: intel.entrySet()) {

            // Get intel object and key
            Intel i = entry.getValue();
            Integer key = entry.getKey();

            // If learned during run, remove else reset
            if (i.learned) {
                intel.remove(key);
            }
            else {
                // Reset p, q, and bids
                i.history.clear();

                // Initialize p, q and bids from global intel
                i.storeHistory(gHistory.get(i.agent_id));
            }
        }
    }

    /**
     * Get intel for given agent
     */
    protected Intel getIntel(Integer id){

        // Check if intel exists for given agent
        if (intel.containsKey(id))
            return intel.get(id);

        throw new RuntimeException("No intel for agent with id " + id);
    }

    /**
     * Get configuration parameter for virtual agent
     */
    protected String getConfig(String key){

        // Attempt to get parameter
        String value = config.get(key);

        //Check if parameter exists
        if (value == null)
            throw new RuntimeException("No parameter named " + key + " for agent " + own_id);

        return value;
    }
}

