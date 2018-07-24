import java.util.HashMap;
import java.util.Map;
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
    
    /** Pairs all parent nodes with their children give a list of intel
     * 
     * @param intel: set of intel to analyze
     * @return an ArrayList, where it's elements are ArrayLists, where the first element is the parent node, followed by its children
     *         an ArrayList of one element represents a node without any children (possibly remove feature?) <- fixed
     */
    static ArrayList<ArrayList<Intel>> getFamily(HashMap<Integer, Intel> intel){
    	ArrayList<ArrayList<Intel>> list = new ArrayList<>();
    	for(Map.Entry<Integer,Intel> parent : intel.entrySet()) {
    		ArrayList<Intel> family = new ArrayList<>();
    		Intel p = parent.getValue();
    		family.add(p);
            for(Map.Entry<Integer,Intel> child: intel.entrySet()) {
            	Intel c = child.getValue();
                if (c.par_id == p.agent_id) {
                    family.add(c);
                }
            }
            list.add(family);
        }
    	for(int i = 0 ; i < list.size() ; i++) {
    		if(list.get(i).size() == 1)
    			list.remove(i);
    	}
    	return list;
    }
    
    /** Functions the same as getFamily, but only returns a parent along with the first child getFamily2 finds
     * 
     * @param intel: set of intel to analyze
     * @return an ArrayList where its elements are arrays, where the first element is the parent, and the second is the child
     */
    static ArrayList<Intel[]> getFamily2(HashMap<Integer,Intel> intel){
    	ArrayList<Intel[]> list = new ArrayList<>();
    	for(Map.Entry<Integer,Intel> parent : intel.entrySet()) {
    		Intel p = parent.getValue();
            for(Map.Entry<Integer,Intel> child: intel.entrySet()) {
            	Intel c = child.getValue();
                if (c.par_id == p.agent_id) {
                	Intel[] family = {p,c};
                    list.add(family);
                    break;
                }
            }
        }
    	return list;
    }
}
