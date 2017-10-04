import java.util.HashMap;

/**
 * Class built for storing gathered information
 *
 * 
 */
   public class Intel extends History {
    
    // Variables to store agent information
    Agent agent;
    int tier;
    int par_id;
    String channel;
    int cost;
    int cap;
    String publicKey;
    String privateKey;
    String type;
    Boolean compromised;
    Boolean interceptTo;
    Boolean interceptFrom;
    Boolean forge;
    Boolean send;
    Boolean learned;
    
    // Hashmaps to store historical information
    HashMap<Integer, Demand> bid = new HashMap<>();

    // Constructor
    Intel(Agent a, Boolean learn) {
        super(a.own_id);
        this.agent = a;
        this.learned = learn;
        captureAgent();
    }
    
    // Capture agent's information
    public void captureAgent(){
        channel = agent.channel.name;
        if(agent instanceof Grid){
            type = "G";
            channel = agent.channel.name;
            tier = ((Grid) agent).gridTier;
            par_id = ((Grid) agent).par_id;
            cost = ((Grid) agent).cost;
            cap = ((Grid) agent).cap;
        }
        else if(agent instanceof Virtual){
            type = "V";
            channel = agent.channel.name;
        }
    }
    
    // Stores demand curve in hashmap
    public void storeBid(int period, Demand demand) {
        bid.put(period, demand);
    }
    
    // Store p and q information in history object
    public void storeHistory(History history){
        
        // Merge price and quantity hashmaps
        this.p.putAll(history.p);
        this.q.putAll(history.q);
    }
}