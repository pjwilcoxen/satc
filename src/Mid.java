import java.util.ArrayList;
import sim.engine.SimState;

/** 
 * Middle tier market node
 */
public class Mid extends Market {

    /**
     * Midlevel market object
     * 
     * All markets other than root nodes are midlevel markets.
     * 
     * @param up_id ID of parent node
     * @param own_id Own ID
     */
    public Mid(int up_id, int own_id) {
        super(up_id,own_id);
    }

    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        ArrayList<Demand> dList;
        
        switch (Env.stageNow) {
            
            case MID_AGGREGATE:
                dList = getDemands();
                demDn = aggDemands(dList);
                demDn.log(this,"down");
                demUp = demDn.adjustTrans(this);
                demUp.log(this,"up");
                reportDemand(demUp);
                break;

            case MID_REPORT:
                priceUp = getPrice();
                priceAu = demDn.getEquPrice();
                priceDn = demDn.getP(priceUp,pc0,pc1,cost,cap);
                reportPrice(priceDn);
                writePQ();
                log();
                break;
                
            default:
                break;
        }
    }

    /** 
     * Initialize for a new population
     */
    @Override 
    public void popInit() {
        super.popInit();
    }

    /** 
     * Reset at the beginning of a DOS run
     */
    @Override
    public void runInit() {
        super.runInit();
        pc0 = 0;
        pc1 = 0;
        demUp = null;
    }

    /**
     * Write a log message
     */
    void log() {
        int q = demDn.getQ(priceDn);
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
