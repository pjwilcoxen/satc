import java.util.ArrayList;

/** 
 * Markets aggregate demands and find equilibrium prices
 */
public abstract class Market extends Agent {

    // queue for incoming demands

    ArrayList<Demand> queueD = new ArrayList<>();
     
    // aggregate demand

    Demand aggD;

    /**
     * General market object
     * 
     * @param up_id Parent node's ID
     * @param own_id Own ID
     */
    public Market(int up_id, int own_id) {
        super(up_id,own_id) ;
    }
    
    /** 
     * Reset at the beginning of a DOS run
     */
    @Override
    public void runInit() {
        super.runInit();
        aggD = null;
        queueD.clear();
    }

    /**
     * Aggregate demands from child nodes
     */
    void aggDemands() {
        Demand thisD = null;

        for(Demand dem: queueD ) 
            if( thisD == null )
                thisD = dem;
            else
                thisD = thisD.aggregateDemand(dem);

        aggD = thisD;

        queueD.clear();
        Env.printLoad(this,Env.curDOS,thisD);
    }

    /**
     * Extract and save demands from list of messages
     */
    void getDemands() {
        for(Msg msg: getMsgs(Msg.Types.DEMAND)) 
            queueD.add(msg.getDemand());
    }

    /**
     * Broadcast a price to all children
     */
    void reportPrice(int price) {
        for (Agent child: children) {
            Msg msg = new Msg(this,child.own_id);
            msg.setPrice(price);
            child.dbus.send(msg);
            }
    }
}
