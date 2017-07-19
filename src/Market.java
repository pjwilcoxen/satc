import java.util.ArrayList;

/** 
 * Markets aggregate demands and find equilibrium prices
 */
public abstract class Market extends Grid {

    /**
     * General market object
     * 
     * @param up_id Parent node's ID
     * @param own_id Own ID
     */
    public Market(int up_id, int own_id) {
        super(up_id,own_id);
        demDn = null;
        demUp = null;
    }
    
    /**
     * Retrieve demands from children and aggregate them
     */
    void buildDemDn() {
        ArrayList<Demand> dList = new ArrayList<>();

        for(Msg msg: getMsgs(Msg.Types.DEMAND))
            dList.add(msg.getDemand());

        demDn = Demand.agg(dList);  
        demDn.log(this,"down");

        priceAu = demDn.getEquPrice();
    }

    /**
     * Broadcast the downstream price to all children
     */
    void sendPriceDn() {

        // send to kids

        for (Agent child: children) {
            Msg msg = new Msg(this,child.own_id);
            msg.setPrice(priceDn);
            child.channel.send(msg);
            }

         // write and log results

         writePQ();
         log();
    }

    /**
     * Write a log message
     */
    void log() {
        String pUp;
        int q;
        
        q = demDn.getQ(priceDn);
        if( (this instanceof Root) && priceAu == -1 )
            Env.log.println("No equilibrium at root node "+own_id+" for DOS run: "+Env.curDOS);

        Env.log.println(
            "node "+own_id+
            ", DOS "+Env.curDOS+
            ", p_self="+priceAu+
            ", p_up="+priceUp+
            ", p_down="+priceDn+
            ", q_down="+q
        );
    }        
}
