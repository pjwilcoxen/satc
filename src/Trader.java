import java.util.ArrayList;
import sim.engine.SimState;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Agent representing an end user or supplier
 */
public abstract class Trader extends Grid {

    /**
     * Maximum number of steps in each generated bid
     */
    static final int MAXSTEP = 14;

    /**
     * Actual number of steps used, for reference
     */
    int steps;

    // manage randomization

    static final int IDRAW  = 1;
    static final int ISTEP  = 2;
    static final int IPRICE = 3;

    double rDraw  ;
    double rStep  ;
    double rPrice ;

    // optional downstream demand sent by a service provider

    Demand recDn ;

    /**
     * Trader agent
     * 
     * @param up_id   ID of parent node
     * @param own_id  Own ID
     */
    public Trader(int up_id, int own_id) {
        super(up_id, own_id);
    }

    /** 
     * Initialize for a new population
     */
    @Override 
    public void popInit() {
        super.popInit();
        steps     = 0;
        demDn    = null;
        rDraw     = runiform(IDRAW);
        rStep     = runiform(ISTEP);
        rPrice    = runiform(IPRICE);
    }

    /** 
     * Reset at the beginning of a DOS run
     */
    @Override
    public void runInit() {
        super.runInit();
        double cutoff = Double.parseDouble(Env.curDOS);
        if( rBlock < cutoff ) 
            Env.setBlock(own_id);
        recDn = null;
    }

    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        int q;
        Demand recD;

        switch (Env.stageNow) {

            case TRADER_SEND:
                
                // build load
                
                demDn = drawLoad();
                demDn.log(this,Demand.Type.BASE);
                demUp = demDn; // reserved for trans adjustments

                // look for one from a service provider

                recDn = getOneDemand();
                if( recDn != null ){
                    recDn.log(this,Demand.Type.REC);
                    demUp = recDn; // reserved for trans adjustments
                }
               
                // send it

                reportDemand(demUp);
                break;

            case CALC_LOADS:
                if( gridTier != Env.curTier )
                    return;
                priceUp = getPrice();
                priceDn = priceUp;
                q_actual = demDn.getQ(priceDn);
                writePQ();
                break;
                
            default:
                break;
        }
    }

    /**
     * Build the agent's net demand curve
     */
    protected abstract Demand drawLoad();
    
    /** 
     * Get a demand curve sent by a service provider
     * 
     * @return Demand curve or null if none was present
     */
    Demand getOneDemand() {
       ArrayList<Demand> dList = getDemands();
       if( dList.size() > 1 )
           throw new RuntimeException("Multiple demands received by "+own_id);
       if( dList.size() == 1 )
           return dList.get(0);
       return null;
    }

    /**
     * Read the list of draws of random agent characteristics
     */
    public static void readDraws() throws FileNotFoundException, IOException {
        switch(Env.agentType) {
        case MONTE:
            TraderMonte.readDraws();
            break;
        case PECAN:
            break;
        default:
            break;
        }
    }
}

