import sim.engine.SimState;

/**
 * Root nodes are markets that have no parent nodes
 */
public class Root extends Market {

    /**
     * Root node
     * 
     * @param par_id ID of parent; normally 0
     * @param own_id ID number of the node
     */
    public Root(int par_id, int own_id) {
        super(par_id,own_id);
    }
            
    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        switch (Env.stageNow) {

            case ROOT_SOLVE:
                buildDemDn();
                break;

            case ROOT_REPORT:
                priceDn = priceAu;
                sendPriceDn();
                break;
                
            default:
                break;
        }
    }
}
