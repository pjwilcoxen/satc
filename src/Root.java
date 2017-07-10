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
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        switch (Env.stageNow) {

            case AGG_MID:
                getDemands();
                aggDemands();
                findEquilibrium();
                break;

            case REPORT_MID:
                reportPrice(bl);
                break;
                
            default:
                break;
        }
    }

    @Override
    public void reportDemand(Demand dem) {
        assert false;
    }

    /**
     * Aggregate net demands of middle nodes
     */
    private void findEquilibrium() {
        int this_bl;
        String dos;

        // find the equilibrium price
        
        this_bl = aggD.getBl();

        dos = Env.curDOS;

        if( this_bl == -1 )
            Env.log.println("No equilibrium at root node "+own_id+" for DOS run: "+dos);

        // make a note of this price
         
        bl = this_bl;
            
        // write the balance prices to the csv file
        // and log it as well

        Env.printResult(this,dos,this_bl,0);
        Env.log.println("node "+own_id+" DOS run "+dos+" own price: "+this_bl);
    }
}
