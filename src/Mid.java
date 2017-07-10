import sim.engine.SimState;

/** 
 * Middle tier market node
 */
public class Mid extends Market {

    static final int IDOS = 0;

    //local version of the global transmission cost
    int cost;

    //local version of the global capacity constraint
    int cap; 
   
    //indicates the upper and lower level around the balance prices considering transaction cost
    int pc0;
    int pc1;

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
        cost = Env.transCost ;
        cap  = Env.transCap;
    }

    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        Demand tmp;

        switch (Env.stageNow) {
            
            case AGG_END:
                getDemands();
                aggDemands();
                tmp = adjustTrans();
                reportDemand(tmp);
                break;

            case REPORT_END:
                getPrice();
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
    }

    /**
     * Adjust aggregate demand for transmission parameters
     */
    private Demand adjustTrans() {
        Demand newD;
        newD = aggD.addCost(cost, this);
        newD = newD.addCapacity(cap);
        return newD ;
    }

    /**
     * Report the balance prices from the middle nodes to leaf nodes
     */
    private void do_report_end() {

        int this_bl;
        int report;
        String dos;

        dos = Env.curDOS;

        // next block is reporting only before accounting for
        // cost and capacity. should it be retained somewhere?
        //
        // find the balance price for each case of dropped nodes

        this_bl = aggD.getBl();
        Env.log.println("node "+own_id+" DOS run "+dos+" own price: "+this_bl);
        
        //set the report value

        //report -1 in the case of no balance point
        //call findReportPrice to adjust the repor price by considering the transaction cost and capacity constrains

        if (bl <= -1) 
            report = -1;
        else 
            report = findReportPrice();
        
        Env.log.println("node "+own_id+" DOS run "+dos+" joint price: "+report);

        //write the balance prices on csv file for each case and report to children 

        Env.printResult(this,dos,report,0);
        reportPrice(report);
    }

    /**
     * Find the actual price for the end users considering transaction cost and capacity limit
     */
    private int findReportPrice() {
        return aggD.getP(bl,pc0,pc1,cost,cap);
    }

    public void setP_c(int p0, int p1) {
        pc0 = p0;
        pc1 = p1;
    }

}
