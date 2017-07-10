import sim.engine.SimState;

/**
 * Root nodes are markets that have no parent nodes
 */
public class Root extends Market {

    static final int IDOS = 0;

    /**
     * Root node
     * 
     * @param own_id ID number of the node
     */
    public Root(int own_id) {
        super(0,own_id);
    }
            
    /** 
     * Initialize for a new population
     */
    @Override
    public void popInit() {
        super.popInit();
    }

    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        switch (Env.stageNow) {
            case AGG_MID:
                Env.log.println("node "+own_id);
                for(int dos_id=0 ; dos_id<Env.nDOS ; dos_id++)
                    do_agg_mid(dos_id);
                clearQueuesD();
                break;

            case REPORT_MID:
                Env.log.println("node "+own_id);
                for(int dos_id=0 ; dos_id<Env.nDOS ; dos_id++ )
                    reportPrice(getBl(dos_id),dos_id);
                break;
                
            default:
                break;
        }
    }

    @Override
    public void reportDemand(Demand dem,int dos_id ) {
        assert false;
    }

    /**
     * Aggregate net demands of middle nodes
     */
    private void do_agg_mid(int dos_id) {
        int this_bl;
        String dos;
        
        dos = Env.dos_runs[dos_id];

        aggD[dos_id] = sumDemands(dos_id) ;
        Env.printLoad(this,Env.dos_runs[dos_id],aggD[dos_id]);

        // find the equilibrium price
        
        this_bl = aggD[dos_id].getBl();

        if( this_bl == -1 )
            Env.log.println("No equilibrium at root node "+own_id+" for DOS run: "+dos);

        // make a note of this price
         
        bl[dos_id] = this_bl;
            
        // write the balance prices to the csv file
        // and log it as well

        Env.printResult(this,dos,this_bl,0);
        Env.log.println("node "+own_id+" DOS run "+dos+" own price: "+this_bl);
    }
}
