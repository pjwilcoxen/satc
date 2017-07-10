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
    int[][] p_c;

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
        p_c  = new int[Env.nDOS][2];
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
                Env.log.println("node "+own_id);
                for(int dos_id=0 ; dos_id<Env.nDOS ; dos_id++) {
                    getDemands(dos_id);
                    aggDemands(dos_id);
                    tmp = adjustTrans(dos_id);
                    reportDemand(tmp,dos_id);
                }
                break;

            case REPORT_END:
                Env.log.println("node "+own_id);
                for (int dos_id=0; dos_id<Env.nDOS ; dos_id++) {
                    getPrices(dos_id);
                    do_report_end(dos_id);
                }
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
        p_c = new int[Env.nDOS][2];
    }

    /**
     * Adjust aggregate demand for transmission parameters
     */
    private Demand adjustTrans(int dos_id) {
        Demand newD;
        newD = aggD[dos_id].addCost(cost, dos_id, this);
        newD = newD.addCapacity(cap);
        return newD ;
    }

    /**
     * Report the balance prices from the middle nodes to leaf nodes
     */
    private void do_report_end(int dos_id) {

        int bl;
        int report;
        String dos;

        dos = Env.dos_runs[dos_id];

        // next block is reporting only before accounting for
        // cost and capacity. should it be retained somewhere?
        //
        // find the balance price for each case of dropped nodes

        bl = aggD[dos_id].getBl();
        Env.log.println("node "+own_id+" DOS run "+dos+" own price: "+bl);
        
        //set the report value

        //report -1 in the case of no balance point
        //call findReportPrice to adjust the repor price by considering the transaction cost and capacity constrains

        if (getBl(dos_id) <= -1) 
            report = -1;
        else 
            report = findReportPrice(dos_id);
        
        Env.log.println("node "+own_id+" DOS run "+dos+" joint price: "+report);

        //write the balance prices on csv file for each case and report to children 

        Env.printResult(this,dos,report,0);
        reportPrice(report,dos_id);
    }

    /**
     * Find the actual price for the end users considering transaction cost and capacity limit
     */
    private int findReportPrice(int dos_id) {
        Demand dem;
        int pr;
        int pc0;
        int pc1;

        pr  = getBl(dos_id);
        pc0 = getP_c(dos_id)[0];
        pc1 = getP_c(dos_id)[1];
        dem = aggD[dos_id];

        return dem.getP(pr,pc0,pc1,cost,cap);
    }

    private int[] getP_c(int dos_id) {
        return p_c[dos_id];
    }

    public void setP_c(int p0, int p1, int dos_id) {
        p_c[dos_id][0] = p0;
        p_c[dos_id][1] = p1;
    }


}
