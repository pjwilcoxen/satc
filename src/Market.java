import java.util.ArrayList;

/** 
 * Markets aggregate demands and find equilibrium prices
 */
public abstract class Market extends Agent {

    // aggregate demand

    Demand aggD;

    /**
     * General market object
     * 
     * @param up_id Parent node's ID
     * @param own_id Own ID
     */
    public Market(int up_id, int own_id) {
        super(up_id,own_id);
        aggD = null;
    }
    
    /** 
     * Reset at the beginning of a DOS run
     */
    @Override
    public void runInit() {
        super.runInit();
        aggD = null;
    }

    /**
     * Aggregate demands from child nodes
     * 
     * @param demands List of demand curves
     * @return Aggregate demand
     */
    Demand aggDemands(ArrayList<Demand> demands) {
        Demand newD = null;

        for(Demand dem: demands ) 
            if( newD == null )
                newD = dem;
            else
                newD = newD.aggregateDemand(dem);

        newD.log(this,"sum");
        return newD;
    }

    /**
     * Extract and save demands from list of messages
     * 
     * @return List of demands
     */
    ArrayList<Demand> getDemands() {
        ArrayList<Demand> queue = new ArrayList<>();
        for(Msg msg: getMsgs(Msg.Types.DEMAND)) 
            queue.add(msg.getDemand());
        return queue;
    }

    /**
     * Broadcast a price to all children
     * 
     * @param price Price to send
     */
    void reportPrice(int price) {
        for (Agent child: children) {
            Msg msg = new Msg(this,child.own_id);
            msg.setPrice(price);
            child.channel.send(msg);
            }
    }
}
