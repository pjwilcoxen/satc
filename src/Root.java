import java.util.ArrayList;
import sim.engine.SimState;

/**
 * Root nodes are markets that have no parent nodes
 */
public class Root extends Market {

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
        ArrayList<Demand> dList;
        
        switch (Env.stageNow) {

            case AGG_MID:
                dList  = getDemands();
                aggD   = aggDemands(dList);
                aPrice = findRootPrice();
                break;

            case REPORT_MID:
                reportPrice(aPrice);
                break;
                
            default:
                break;
        }
    }

    /**
     * Aggregate net demands of middle nodes
     */
    private int findRootPrice() {
        int this_bl;
        String dos;

        // find the equilibrium price
        
        this_bl = aggD.getEquPrice();

        dos = Env.curDOS;

        if( this_bl == -1 )
            Env.log.println("No equilibrium at root node "+own_id+" for DOS run: "+dos);

        // write the balance prices to the csv file and log it as well

        Env.printResult(this,dos,this_bl,0);
        Env.log.println("node "+own_id+" DOS run "+dos+" own price: "+this_bl);
        
        return this_bl;
    }
}
