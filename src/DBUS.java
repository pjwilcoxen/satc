import java.util.ArrayList;

/**
 * Basic communications bus
 */
public class DBUS {

    Env e;
    ArrayList<Bid[]> queueD;
    int [] bl;
    
    //
    //  Constructor
    //
    
    public DBUS(Env e){
        this.e = e;
        queueD = new ArrayList<>();
        queueD.add(new Bid[200]);
        queueD.add(new Bid[200]);
        queueD.add(new Bid[200]);
        queueD.add(new Bid[200]);
        bl = new int[4];
    }
    
    //  Check: 
    //     Add a message object and collapse these methods to one.
    //     That is, DBUS sends messages but leaves it up to the
    //     recipient to figure out what to do with them

    public void toQueue(Bid[] bids, int drop, int to, int sd_type) {
        queueD.set(drop, bids);
        e.listAgent.get(to - 1).appendQueueD(/*this.queueD.get(drop)*/bids, drop);
        
    }     
      
    public void toQueue(int bl, int drop, int to, int sd_type) {
        this.bl[drop] = bl;
        e.listAgent.get(to-1).setBl(this.bl[drop], drop);
    }
    
}
