import java.util.ArrayList;
import sim.engine.SimState;

/**
 * Abstract class that extends Virtual. General purpose
 * functionality for all adversarial subclasses. 
 *
 * Contains functionality to probe agents in the grid
 * and attempt to compromise their capabilities.
 */

 public abstract class Adversary extends Virtual {
    
    // Properties controlling adversary's actions
    ArrayList<String> capability;
    ArrayList<Integer> target;
    double sophistication;
    double constraint;
    
    
    /**
     * Constructor
     *
     * 
     */
    public Adversary(int own_id) {
        super(own_id);
        this.capability = new ArrayList<String>();
        this.target = new ArrayList<Integer>();
    }
    
    /** 
     * Initialize for a new population
     */
    @Override 
    public void popInit() {
        super.popInit();
        probeVulnerabilities();
        
    }

    /** 
     * Reset at the beginning of a DOS run
     */
    @Override
    public void runInit() {
        super.runInit();
    }
    
    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
    }
    
    /** 
     * Probe vulnerabilities of known agents
     */
     private void probeVulnerabilities() {
        
        for (Intel i: intel) {
            
            // Retrieve agent from environment
            Agent agent = Env.getAgent(i.agent_id);
            
            // Attempt to compromise agent's systems
            i.compromised = canCompromise(agent);
            
            if (i.compromised) {
                
                // If compromised, all capabilities are available
                i.interceptTo = true;
                i.interceptFrom = true;
                i.forge = true;
                i.send = true;
            }
            else {
                
                // Else attempt to compromise other capabilities
                i.interceptTo = canInterceptTo(agent);
                i.interceptFrom = canInterceptFrom(agent);
                i.forge = canForge(agent);
                i.send = canSend(agent);
            }           
        }
     }
     
     /** 
     * Returns true if adversary can compromise agent's system
     *
     * Must meet four conditions
     *  1. Adversary must be able to control other systems
     *  2. Target's channel must be accessible by adversary
     *  3. Target itself must be accessible by adversary
     *  4. Target must have a lower security than the adversary's sophistication
     */
     private boolean canCompromise(Agent a) {
        
        // Ensures agent meets conditions to be compromised
        if(capability.contains("control") && channels.contains(a.channel) && agents.contains(a.own_id) && a.isVulnerable(sophistication)) {
            a.channel.divert_from(a.own_id, this.own_id);
            a.channel.divert_to(a.own_id, this.own_id);

            return true;
        }
        else {
            return false;
        }
     }
     
     /** 
     * Returns true if adversary can intercept messages to an agent
     *
     * Must meet three conditions
     *  1. Adversary must be able to intercept messages by recepient
     *  2. Target's channel must be accessible by adversary
     *  3. Target itself must be accessible by adversary
     */
     private boolean canInterceptTo(Agent a) {
        
        // Ensures agent meets conditions to intercept messages
        if(capability.contains("interceptTo") && channels.contains(a.channel) && agents.contains(a.own_id)) {
            a.channel.divert_to(a.own_id, this.own_id);
            return true;
        }
        else {
            return false;
        }
     }
     
     /** 
     * Returns true if adversary can intercept messages from an agent
     *
     * Must meet three conditions
     *  1. Adversary must be able to intercept messages by sender
     *  2. Target's channel must be accessible by adversary
     *  3. Target itself must be accessible by adversary
     */
     private boolean canInterceptFrom(Agent a) {
        
        // Ensures agent meets conditions to intercept messages
        if(capability.contains("interceptFrom") && channels.contains(a.channel) && agents.contains(a.own_id)) {
            a.channel.divert_from(a.own_id, this.own_id);
            return true;
        }
        else {
            return false;
        }
     }
     
     /** 
     * Returns true if adversary can forge messages
     *
     * Must meet two conditions
     *  1. Adversary can forge messages or sender does not use encryption
     *  2. Target's channel must be accessible by adversary
     */
     private boolean canForge(Agent a) {
        
        // Ensures agent meets conditions to intercept messages
        if((capability.contains("forge") || a.privateKey == null) && channels.contains(a.channel)) {
            return true;
        }
        else {
            return false;
        }
     }
     
     /** 
     * Returns true if adversary can send messages to an agent
     *
     * Must meet one conditions
     *  1. Target's channel must be accessible by adversary
     */
     private boolean canSend(Agent a) {
        
        // Ensures agent meets conditions to intercept messages
        if(channels.contains(a.channel)) {
            return true;
        }
        else {
            return false;
        }
     }
}

