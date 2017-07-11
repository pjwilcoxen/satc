import sim.engine.SimState;

/**
 * Agent representing an end user or supplier
 */
public class Trader extends Agent {

    /**
     * Maximum number of steps in each generated bid
     */
    static final int MAXSTEP = 14;

    /**
     * Actual number of steps used, for reference
     */
    int steps;

    //indicates initial load for end user extracted from "testdraw.csv"
    //indicates the elasticity of end user extracted from "testdraw.csv" 
    double load;
    double elast;

    //indicates supply or demand type
    String sd_type;

    // demand for this agent
    Demand demand;

    static final int IDRAW  = 1;
    static final int ISTEP  = 2;
    static final int IPRICE = 3;

    double rDraw  ;
    double rStep  ;
    double rPrice ;

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
        demand    = null;
        rDraw     = runiform(IDRAW);
        rStep     = runiform(ISTEP);
        rPrice    = runiform(IPRICE);
        drawLoad();
        Env.printLoad(this,"base",demand);
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
    }

    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        int q;
        
        switch (Env.stageNow) {

            case TRADER_SEND:
                reportDemand(demand);
                break;

            case CALC_LOADS:
                aPrice = getPrice();
                q = demand.getQ(aPrice);
                Env.printResult(this,Env.curDOS,aPrice,q);
                break;
                
            default:
                break;
        }
    }

    /**
     * Build the agent's net demand curve
     */
    private void drawLoad() {
 
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
            demand = Demand.makeDemand(this);
        else 
            demand = Demand.makeSupply(this);
    }
     
}

