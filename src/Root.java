import sim.engine.SimState;

/**
 * Root nodes are markets that have no parent nodes
 */
public class Root extends Market {

    public Root(int own_id) {
        super(0,own_id);
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
                do_agg_mid();
                break;

            case REPORT_MID:
                do_report_mid();
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
    private void do_agg_mid() {

        int j;
        int this_bl;

        Env.log.println("node "+own_id);

        for(int dos_id=0 ; dos_id<Env.nDOS ; dos_id++) {
            aggD[dos_id] = sumDemands(dos_id) ;
            Env.printLoad(this,Env.dos_runs[dos_id],aggD[dos_id]);
        }

        //find the balance price for each case of dropped nodes
        for(j=0; j < Env.nDOS ; j++) {

            this_bl = aggD[j].getBl();
            if( this_bl == -1 )
                Env.log.println("failed at drop: " + j);

            //set the balance price as the class variable
            setBl(this_bl, j);
            
            // write the balance prices to the csv file

            String dos = Env.dos_runs[j];
            Env.printResult(this,dos,this_bl,0);

            // write a log message
            Env.log.println("node "+own_id+" DOS run "+dos+" own price: "+this_bl);
        }

        clearQueuesD();

        //increment agent's view of time
        myTime++;
    }

    /**
     * Report the balance price from the root node to middle nodes
     */
    private void do_report_mid() {
        
        Env.log.println("node "+own_id);

        for(int i=0 ; i<Env.nDOS ; i++ ) {
            reportPrice(getBl(i),i);
        }
    }

}
