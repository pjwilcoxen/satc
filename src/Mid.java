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
        switch (Env.stageNow) {
            case AGG_END:
                do_agg_end();
                break;

            case REPORT_END:
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
        p_c = new int[Env.nDOS][2];
    }

    /**
     * Aggregate net demands of leaf nodes
     */
    private void do_agg_end() {

        Demand this_agg;
        Demand tmp;
        
        Env.log.println("node "+own_id);

        for(int dos_id=0 ; dos_id<Env.nDOS ; dos_id++) {

            // do the aggregation and save the result

            this_agg = sumDemands(dos_id) ;
            aggD[dos_id] = this_agg;
            Env.printLoad(this,Env.dos_runs[dos_id],this_agg);

            // adjust for transmission cost and constraint

            tmp = this_agg.addCost(cost, dos_id, this);
            tmp = tmp.addCapacity(cap);

            // send to parent

            reportDemand(tmp,dos_id);
        }

        clearQueuesD();

    }

    /**
     * Report the balance prices from the middle nodes to leaf nodes
     */
    private void do_report_end() {

        int child_id;
        int bl;

        for(Msg msg: getMsgs(Msg.Types.PRICE)) 
            setBl(msg.getPrice(),msg.dos_id);

        Env.log.println("node "+own_id);
 
        // next block is reporting only before accounting for
        // cost and capacity. should it be retained somewhere?

        //find the balance price for each case of dropped nodes
        for (int j = 0; j < Env.nDOS ; j++) {
            bl = aggD[j].getBl();
            Env.log.println("node "+own_id+" DOS run "+Env.dos_runs[j]+" own price: "+bl);
        }
        
        int[] report = new int[Env.nDOS];
        //set the report values for each case of dropped nodes
        for (int dos_id = 0; dos_id < Env.nDOS ; dos_id++) {
            //report -1 in the case of no balance point
            if (getBl(dos_id) <= -1) {
                report[dos_id] = -1;
            //call findReportPrice to adjust the repor price by considering the transaction cost and capacity constrains
            } else {
                report[dos_id] = findReportPrice(dos_id);
            }
            Env.log.println("node "+own_id+" DOS run "+Env.dos_runs[dos_id]+" joint price: "+report[dos_id]);
        }

        //write the balance prices on csv file for each case and report to children 

        for(int i=0 ; i<Env.dos_runs.length ; i++) {
            Env.printResult(this,Env.dos_runs[i],report[i],0);
            reportPrice(report[i],i);
        }
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
