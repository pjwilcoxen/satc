import java.util.ArrayList;
import sim.engine.SimState;

/**
 * Agent representing an end user or supplier
 */
public class Trader extends Grid {

    /**
     * Maximum number of steps in each generated bid
     */
    static final int MAXSTEP = 14;

    /**
     * Actual number of steps used, for reference
     */
    int steps;

    // initial load and elasticity and type from monte carlo file

    double load;
    double elast;
    String sd_type;

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
     * @param sd_type Supply or demand type
     */
    public Trader(int up_id,int own_id, String sd_type) {
        super(up_id,own_id);
        this.sd_type = sd_type;
    }

    /** 
     * Initialize for a new population
     */
    @Override 
    public void popInit() {
        super.popInit();
        load      = 0;
        elast     = 0;
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
                demDn.log(this,"base");
                demUp = demDn; // reserved for trans adjustments

                // look for one from a service provider

                recDn = getOneDemand();
                if( recDn != null ){
                    recDn.log(this,"recdn");
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
    private Demand drawLoad() {
 
        Demand newD;
        Env.Draw draw;
        int max = 9858; 
        int rand;
        int row;
        
        // generate a random number of input lines to skip
        // max was originally hard-coded and is left that 
        // way for compatibility

        rand = (int)(rDraw * max);
        
        if( sd_type.equals("D") )
           draw = Env.drawListD.get(rand);
        else
           draw = Env.drawListS.get(rand);
        
        // parameters to use in constructing this curve

        load  = draw.load;
        elast = draw.elast;
        steps = (int) (rStep * MAXSTEP + 2);

        //call draw function based on the type of end user

        if (sd_type.equals("D")) 
            newD = Demand.makeDemand(this);
        else 
            newD = Demand.makeSupply(this);
        
        return newD;
    }
    
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
}

