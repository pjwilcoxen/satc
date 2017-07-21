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
        if( gridTier != Env.curTier )
            return;
        switch (Env.stageNow) {
            case AGGREGATE:
                if( gridTier != Env.curTier )return;
                buildDemDn();
                sendDemUp();
                break;

            case REPORT:
                priceUp = getPrice();
                priceDn = demDn.getPriceDn(priceUp,(Grid) this);
                sendPriceDn();
                break;
                
            default:
                break;
        }
    }

    /**
     * Send a demand curve up accounting for transmission
     */
    void sendDemUp() {
        demUp = demDn.adjustTrans((Grid) this);
        demUp.log(this,"up");
        reportDemand(demUp);
    }

}
