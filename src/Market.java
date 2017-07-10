import java.util.ArrayList;

/** 
 * Markets aggregate demands and find equilibrium prices
 */
public abstract class Market extends Agent {

    // set of queues of incoming bids from child nodes; there will be 
    // one queue for each DOS run
    ArrayList<ArrayList<Demand>> queueD;
     
    // array of aggregated demands; one for each DOS run
    Demand[] aggD;

    /**
     * General market object
     * 
     * @param up_id Parent node's ID
     * @param own_id Own ID
     */
    public Market(int up_id, int own_id) {
        super(up_id,own_id) ;

        aggD = new Demand[Env.nDOS];
        queueD = new ArrayList<>();
        for(int i=0 ; i<Env.nDOS ; i++)
            queueD.add(new ArrayList<>());
    }
    
    /** 
     * Reset at the beginning of a DOS run
     */
    @Override
    public void runInit() {
        aggD = new Demand[Env.nDOS];
        for(int i=0 ; i<Env.nDOS ; i++)
            queueD.get(i).clear();
    }

    /**
     * Aggregate demands from child nodes
     */
    Demand sumDemands(int dos_id) {
        Demand thisD = null;

        for(Msg msg: getMsgs(Msg.Types.DEMAND,dos_id)) 
            appendQueueD(msg.getDemand(),msg.dos_id);

        for(Demand dem: queueD.get(dos_id) ) 
            if( thisD == null )
                thisD = dem;
            else
                thisD = thisD.aggregateDemand(dem);

        return thisD;
    }

    /**
     * Broadcast a price to all children
     */
    void reportPrice(int price,int dos_id) {
        for (Agent child: children) {
            Msg msg = new Msg(this,child.own_id);
            msg.setPrice(price);
            msg.dos_id = dos_id;
            child.dbus.send(msg);
            }
    }

    void appendQueueD(Demand dem, int dos_id) {
        queueD.get(dos_id).add(dem);
    }

    //
    //  clearQueuesD
    //

    void clearQueuesD() {
        for (int i = 0; i < Env.nDOS ; i++) 
            queueD.get(i).clear();
    }

}
