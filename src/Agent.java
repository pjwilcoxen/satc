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

    // random variables for DOS run

    static final int IDOS = 0;

    double rBlock;
    
    // Types of security measures supported
    public static enum Security{
        
        // No security measures
        NONE,
        
        // Basic security checks: timestamp, content type, etc
        BASIC,
        
        // Hash and verify hash
        HASH,
        
        // Verify sender's digital signature
        VERIFY,
        
        // Add digital signature
        SIGN,
        
        // Encrypt message contents
        ENCRYPT,
        
        // Token exchange
        TOKEN
    }
    
    // Security measures for communication
    final ArrayList<Security> secMeasures = new ArrayList<>();
    
    // Security measures to enforce on message send and receive
    static class Enforce {
        ArrayList<Security> send;
        ArrayList<Security> receive;

        Enforce(ArrayList<Security> s, ArrayList<Security> r) {
            this.send = s;
            this.receive = r;
        }
    }
    
    // Security enforcement agreements with other agents
    HashMap<Integer, Enforce> config = new HashMap<>();
    
    // public and private key for encrypting/decrypting messages
    String publicKey;
    String privateKey;
    
    // Security level of the agent's system
    int security;
      
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
    public int getSecurity() {
        return security;
    }

    /**
     * Determine vulnerability to an attack 
     *
     * @param strength Strength of attack on 0 to 100.
     * @return Is the agent vulnerable to the attack
     */
    public boolean isVulnerable(int strength) {
        return security < strength;
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
     * Check if security measure is in place
     * 
     * @param which security measure to check
     * @return true if security measure is supported
     */
    private boolean securityEnabled(Security measure) {
        boolean supported = false;
        for(Security s: secMeasures){
            if(s == measure){
                supported = true;
            }
        }
        return supported;
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
     * Configure security measure used by agent
     * 
     * @param security measure to configure for agent
     */
    public void addSecurity(String measure) {
        switch(measure) {
            case "NONE":
                secMeasures.clear();
                secMeasures.add(Security.NONE);            
                break;
            case "BASIC":
                if (securityEnabled(Security.NONE)){
                    secMeasures.remove(Security.NONE);
                }
                secMeasures.add(Security.BASIC);            
                break;
            case "HASH":
                if (securityEnabled(Security.NONE)){
                    secMeasures.remove(Security.NONE);
                }
                secMeasures.add(Security.HASH);            
                break;
            case "VERIFY":
                if (securityEnabled(Security.NONE)){
                    secMeasures.remove(Security.NONE);
                }
                secMeasures.add(Security.VERIFY);            
                break;
            case "SIGN":
                if (securityEnabled(Security.NONE)){
                    secMeasures.remove(Security.NONE);
                }
                secMeasures.add(Security.SIGN);            
                break;
            case "ENCRYPT":
                if (securityEnabled(Security.NONE)){
                    secMeasures.remove(Security.NONE);
                }
                secMeasures.add(Security.ENCRYPT);            
                break;
            case "TOKEN":
                if (securityEnabled(Security.NONE)){
                    secMeasures.remove(Security.NONE);
                }
                secMeasures.add(Security.TOKEN);            
                break;
            default:
                throw new RuntimeException("Unexpected security measure: " + measure);                

        }
    }
}