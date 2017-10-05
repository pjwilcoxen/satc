import java.util.ArrayDeque;
import java.util.ArrayList;
import sim.engine.Steppable;
import java.util.HashMap;

/**
 * General purpose Agent class
 */
public abstract class Agent implements Steppable {
    
    /**
     * Version of the Agent class
     */
    public static final String VER = "3.0";

    /**
     * Number of random values to generate per population
     */
    private static final int RCOUNT = 10;

    // permanent characteristics of this agent
    
    final int own_id;   

    /**
     * Pools for holding random numbers for this agent  
     *
     * Helps preserve testing repeatability when code using
     * the numbers is reordered.
     */
    final ArrayList<Double[]> rPool = new ArrayList<>();

    // random variables for DOS run and overall security

    static final int IDOS = 0;
    static final int ISEC = 4;

    double rBlock;
    double rSecure;
    
    // public and private key for encrypting/decrypting messages
    String publicKey;
    String privateKey;
    
    // hashmap for storing other agent's public keys that are known
    HashMap<Integer, String> knownKeys = new HashMap<>();
    
    // data channel this agent uses to communicate with its parent
    
    Channel channel;

    // this agent's incoming message queue
    
    final ArrayDeque<Msg> msgs = new ArrayDeque<>();

    /**
     * Set the channel used by this agent to talk to its parent
     * 
     * @param channel Channel object
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Initialize this agent for a new population
     */
    public void popInit() {
        rBlock  = runiform(IDOS)*100.0;
        rSecure = runiform(ISEC)*100.0;
    }    

    /**
     * Initialize this agent for a new DOS run
     */
    public void runInit() {
        msgs.clear();
    }

    /**
     * Return this agent's security level
     *
     * @return Agent's security level on 0 to 100.
     */
    public double getSecurity() {
        return rSecure;
    }

    /**
     * Determine vulnerability to an attack 
     *
     * @param strength Strength of attack on 0 to 100.
     * @return Is the agent vulnerable to the attack
     */
    public boolean isVulnerable(double strength) {
        return rSecure < strength;
    }

    /**
     * Add a message to this agent's input queue
     *
     * @param msg Message to add
     */
    public void deliver(Msg msg) {
        msgs.add(msg);
    }

    /**
     * Extract a set of messages from the input queue
     *
     * @param type Message type to extract
     * @return List of messages
     */
    ArrayList<Msg> getMsgs(Msg.Types type) {
        ArrayList<Msg> selected = new ArrayList<>();
        for(Msg msg: msgs) 
            if( msg.type == type ) 
                selected.add(msg);
        for(Msg msg: selected)
            msgs.remove(msg);
        return selected;
    }

    /**
     * Extract a price from list of messages
     * 
     * @return Price sent
     */
    int getPrice() {
        ArrayList<Msg> price_msgs;
        price_msgs = getMsgs(Msg.Types.PRICE);
        assert price_msgs.size() == 1;
        return price_msgs.get(0).getPrice();
    }

    /** 
     * Get a list of demands from the message queue
     *
     * @return ArrayList of Demand objects
     */
    public ArrayList<Demand> getDemands() {
        ArrayList<Demand> dList = new ArrayList<>();
        for(Msg msg: getMsgs(Msg.Types.DEMAND))
            dList.add(msg.getDemand());
        return dList;
    }
    
    /**
     * Get a random number from this agent's pool
     * 
     * @param which Which number to retrieve
     * @return Uniform random number
     */
    double runiform(int which) {
        Double[] pop_set = rPool.get(Env.pop-1);
        return pop_set[which];
    }

    /**
     * General agent instance
     * 
     * @param own_id ID of this agent
     */
    public Agent(int own_id) {
        super();
        this.own_id = own_id;

        // build a pool of values to be used for later
        // randomization. do it once when the agent is 
        // instantiated to help with repeatability when
        // code using the numbers is reordered.

        Double[] rArray;
        for(int i=0 ; i<Env.numPop ; i++) {
            rArray = new Double[RCOUNT];
            for(int j=0 ; j<RCOUNT ; j++)
                rArray[j] = Env.runiform();
            rPool.add(rArray);
        }
    }
    
    /**
     * Store a known agent's public key
     * 
     * @param agent_id is the other agent's id
     * @param key is the other agent's key
     */
     private void storeKey(int agent_id, String key) {
        if (!knownKeys.containsKey(agent_id)) {
            knownKeys.put(agent_id, key);
        }
        else {
            throw new RuntimeException("Redundant key storage. Agent "+own_id+" storing key "+key+" for agent "+agent_id);
        }
    }
    
    /**
     * Store a known agent's public key
     * 
     * @param agent_id is the other agent's id
     * @return Uniform random number
     */
    private String getKey(int agent_id) {
        return knownKeys.get(agent_id);
    }
}