import java.util.ArrayList;
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
        ArrayList<Demand> dList;
        
        switch (Env.stageNow) {

            case ROOT_SOLVE:
                dList  = getDemands();
                demDn   = aggDemands(dList);
                priceAu = demDn.getEquPrice();
                priceDn = priceAu;
                break;

            case ROOT_REPORT:
                Env.printResult(this,priceDn,0);
                reportPrice(priceDn);
                log();
                break;
                
            default:
                break;
        }
    }

    /**
     * Aggregate net demands of middle nodes
     */
    void log() {
        int q = demDn.getQ(priceDn);
        if( priceAu == -1 )
            Env.log.println("No equilibrium at root node "+own_id+" for DOS run: "+Env.curDOS);
        Env.log.println(
            "node "+own_id+
            ", DOS "+Env.curDOS+
            ", p_self="+priceAu+
            ", p_down="+priceDn+
            ", q_down="+q                    
        );
    }
}
