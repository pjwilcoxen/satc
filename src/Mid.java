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
        switch (Env.stageNow) {
            
            case MID_AGGREGATE:
                buildDemDn();
                sendDemUp();
                break;

            case MID_REPORT:
                priceUp = getPrice();
                priceDn = demDn.getP(priceUp,pc0,pc1,cost,cap);
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
