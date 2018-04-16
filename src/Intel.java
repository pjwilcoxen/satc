import java.util.HashMap;
import java.util.ArrayList;

/**
 * Class that stores a virtual agent's information about
 * another agent within the grid.  A set of intel objects
 * are contained within each virtual agent and used by
 * the virtual agent to make decisions.
 *
 * Contains functionality to store an agent's grid-level
 * (transmission cost, price, parent, children, tier)and 
 * historic (price, quantity, bid) information. Can also
 * retrieve max/min/avg information regarding p and q.
 */
   public class Intel{
    
    // Variables to store agent information
    int agent_id;
    int tier;
    int par_id;
    String channel;
    int cost;
    int cap;
    ArrayList<Integer> children = new ArrayList<>();
    History history;
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
    Intel(int id, Boolean learn) {
        agent_id = id;
        history = new History(id);
        this.learned = learn;
        captureAgent();
    }
    
    // Capture agent's information
    public void captureAgent(){
        
        // Retrieve agent
        Agent a = Env.getAgent(agent_id);
        
        
        // Store intel
        channel = a.channel.name;
        if(a instanceof Grid){
            
            //store grid level intel
            type = "G";
            channel = a.channel.name;
            tier = ((Grid) a).gridTier;
            par_id = ((Grid) a).par_id;
            cost = ((Grid) a).cost;
            cap = ((Grid) a).cap;
            
            for(Grid kid: ((Grid) a).children) {
                children.add(kid.own_id);
            }
        }
        else if(a instanceof Virtual){
            type = "V";
            channel = a.channel.name;
        }
    }
    
    // Store p, q and bid information in history object
    public void storeHistory(History h){
        history = h;
    }
}