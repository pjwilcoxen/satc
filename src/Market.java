import java.util.ArrayList;
import sim.engine.SimState;

/** 
 * Markets aggregate demands and find equilibrium prices
 */
public class Market extends Grid {

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
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        if( gridTier != Env.curTier )
            return;
        switch (Env.stageNow) {
            case AGGREGATE:
                buildDemDn();
                if( par_id != 0 )
                    sendDemUp();
                break;

            case REPORT:
                if( par_id == 0 ) 
                    priceDn = priceAu;
                else {
                    priceUp = getPrice();
                    priceDn = demDn.getPriceDn(priceUp,(Grid) this);
                }
                sendPriceDn();
                break;
                
            default:
                break;
        }
    }

    /**
     * Retrieve demands from children and aggregate them
     */
    void buildDemDn() {
        demDn = Demand.agg(getDemands());  
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
     * Send a demand curve up accounting for transmission
     */
    void sendDemUp() {
        demUp = demDn.adjustTrans((Grid) this);
        demUp.log(this,"up");
        reportDemand(demUp);
    }


    /**
     * Write a log message
     */
    void log() {
        String pUp;
        int q;
        
        q = demDn.getQ(priceDn);
        if( par_id == 0 && priceAu == -1 )
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
