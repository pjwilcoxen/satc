import java.util.HashMap;
import java.util.Map;

/**
 * Class for storing historic price and quantity data for
 * a specific agent. Intended to be used as a single set of
 * information stored at the global level and populated
 * during initialization.  Used by virtual agents to 
 * initialize their intel about known agents.
 */
public class History {
    
    // Variables to store agent information
    int agent_id;
    
    // Hashmaps to store historical information
    HashMap<Integer, Integer> p    = new HashMap<>();
    HashMap<Integer, Integer> q    = new HashMap<>();
	HashMap<Integer, Demand> upD   = new HashMap<>();
	HashMap<Integer, Demand> downD = new HashMap<>();
    
    // Constructor
    History(int agent_id) {
        this.agent_id = agent_id;
    }
    
        // Stores price information in hashmap
    public void storePrice(int period, int price) {
        this.p.put(period, price);
    }
    
    // Stores quantity information in hashmap
    public void storeQuantity(int period, int quantity) {
        this.q.put(period, quantity);
    }
	
	// Stores up demand information in hashmap
    public void storeUpDemand(int period, Demand d) {
        this.upD.put(period, d);
    }
	
	// Stores down demand information in hashmap
    public void storeDownDemand(int period, Demand d) {
        this.downD.put(period, d);
    }
    
    // Retrieves max quantity
    public Integer getMaxQ() {
        Integer max = null;
        
        for (Map.Entry<Integer, Integer> entry : q.entrySet()) {
            
            if(max == null || entry.getValue() > max) {
                max = entry.getValue();
            }
        }
        return max;
    }
    
    // Retrieves min quantity
    public Integer getMinQ() {
        Integer min = null;
        
        for (Map.Entry<Integer, Integer> entry : q.entrySet()) {
            
            if(min == null || entry.getValue() < min) {
                min = entry.getValue();
            }
        }
        return min;
    }
    
    // Retrieves average quantity
    public double getAvgQ() {
        Integer sum = null;
        Integer i = 0;
        
        for (Map.Entry<Integer, Integer> entry : q.entrySet()) {
            
            sum += entry.getValue();
            i++;
        }
        return sum/i;
    }
    
    // Retrieves quantity for a specific period
    public Integer getQuantity(Integer period) {
            return q.get(period);
    }
    
    // Retrieves price for a specific period
    public Integer getPrice(Integer period) {
            return p.get(period);
    }
	
	// Retrieves an up demand for a specific period
    public Demand getUpDemand(Integer period) {
            return upD.get(period);
    }
	
	// Retrieves a down demand for a specific period
    public Demand getDownDemand(Integer period) {
            return downD.get(period);
    }
	
	// Clear out history data
    public void clear() {
            p.clear();
			q.clear();
			upD.clear();
			downD.clear();
    }
}