import java.util.ArrayDeque;
import java.util.ArrayList;
import sim.engine.Steppable;

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

    // permanent attributes of the agent
    
    final int own_id;   
    final int par_id;
    
    /**
     * Pools for holding random numbers for this agent  
     *
     * Helps preserve testing repeatability when code using
     * the numbers is reordered.
     */
    final ArrayList<Double[]> rPool = new ArrayList<>();

    // random variable for DOS runs

    static final int IDOS = 0 ;
    
    double rBlock;

    // data bus this agent uses to communicate with its parent
    
    DBUS dbus;

    // this agent's incoming message queue
    
    final ArrayDeque<Msg> msgs = new ArrayDeque<>();

    /**
     * Children of the agent
     */
    final ArrayList<Agent> children = new ArrayList<>();

    // this agent's view of the price
    
    int aPrice;

    /**
     * Set the DBUS used by this agent to talk to its parent
     * 
     * @param dbus DBUS object
     */
    public void setDBUS(DBUS dbus) {
        this.dbus = dbus;
    }

    /**
     * Initialize this agent for a new population
     */
    public void popInit() {
        rBlock = runiform(IDOS)*100.0;
    }    

    /**
     * Initialize this agent for a new DOS run
     */
    public void runInit() {
        msgs.clear();
        aPrice = 0;
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
     * Send a demand to parent node
     * 
     * @param dem Demand curve
     */
    public void reportDemand(Demand dem) {
        Msg msg = new Msg(this,par_id);
        msg.setDemand(dem);
        assert dbus != null;
        dbus.send(msg);
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
     * @param up_id ID of agent's parent node
     * @param own_id Own ID
     */
    public Agent(int up_id, int own_id) {
        super();

        this.par_id = up_id;
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
}

