import java.util.HashMap;
import java.util.Map;

/**
 * Class built for storing historical information
 *
 * 
 */
public class History {
    
    // Variables to store agent information
    int agent_id;
    
    // Hashmaps to store historical information
    HashMap<Integer, Integer> p = new HashMap<>();
    HashMap<Integer, Integer> q = new HashMap<>();
    
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
}