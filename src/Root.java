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
                priceUp = findRootPrice();
                break;

            case ROOT_REPORT:
                reportPrice(priceUp);
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
        
        this_bl = demDn.getEquPrice();

        dos = Env.curDOS;

        if( this_bl == -1 )
            Env.log.println("No equilibrium at root node "+own_id+" for DOS run: "+dos);

        // write the balance prices to the csv file and log it as well

        Env.printResult(this,dos,this_bl,0);
        Env.log.println("node "+own_id+" DOS run "+dos+" own price: "+this_bl);
        
        return this_bl;
    }
}
