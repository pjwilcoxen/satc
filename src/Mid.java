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
                demUp = adjustTrans(demDn);
                demUp.log(this,"adj");
                reportDemand(demUp);
                break;

            case MID_REPORT:
                priceUp = getPrice();
                priceAu = demDn.getEquPrice();
                do_report_end();
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
     * Adjust aggregate demand for transmission parameters
     * 
     * @param agg Original demand curve
     * @return Curve adjusted for transmission
     */
    private Demand adjustTrans(Demand agg) {
        Demand newD;
        newD = agg.addCost(cost, this);
        newD = newD.addCapacity(cap);
        return newD;
    }

    /**
     * Report the equilibrium price from a middle node to its children
     */
    private void do_report_end() {
        String dos;

        dos = Env.curDOS;

        // find this node's equilibrium price in isolation

        
        // find the node's actual price accounting for transmission

        if (priceUp <= -1) 
            priceDn = -1;
        else 
            priceDn = demDn.getP(priceUp,pc0,pc1,cost,cap);
        
        Env.log.println("node "+own_id+" DOS run "+dos+": own p="+priceAu+", grid p="+priceDn);

        //write the balance prices on csv file for each case and report to children 

        Env.printResult(this,dos,priceDn,0);
        reportPrice(priceDn);
    }

    /**
     * Set Price bounds
     * 
     * These are used in finding a downstream price consistent with
     * transmission parameters.
     * 
     * @param p0 Lower bound
     * @param p1 Upper bound
     */
    public void setPc(int p0, int p1) {
        pc0 = p0;
        pc1 = p1;
    }

}
