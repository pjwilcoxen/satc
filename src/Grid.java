import java.util.ArrayList;

/**
 * General purpose class for grid-connected agents
 */
public abstract class Grid extends Agent {

    // permanent attributes of the agent
    
    final int par_id;
    int gridTier = 0;

    /**
     * Children of the agent
     */
    final ArrayList<Grid> children = new ArrayList<>();

    // transmission cost and capacity to parent node

    int cost;
    int cap; 
    
    // this agent's view of up and downstream demand and price
    
    Demand demDn;
    Demand demUp;

    int priceUp; // price received from parent
    int priceDn; // price reported to children
    int priceAu; // price in autarky

    /**
     * Constructor for grid-connected objects
     * 
     * @param par_id Parent ID
     * @param own_id Own ID
     */
    Grid(int par_id, int own_id) {
        super(own_id);
        this.par_id = par_id;
    }

    /**
     * Initialize this agent for a new DOS run
     */
    @Override
    public void runInit() {
        super.runInit();
        demUp = null;
        if( ! (this instanceof Trader) )
            demDn = null;
        priceUp = 0;
        priceDn = 0;
        priceAu = 0;
    }
    
    /**
     * Send a demand to the node's parent
     * 
     * @param dem Demand curve
     */
    public void reportDemand(Demand dem) {
        Msg msg = new Msg(this,par_id);
        msg.setDemand(dem);
        assert channel != null;
        channel.send(msg);
    }

    /**
     * getTier
     *
     * Deduce and return the tier of this agent in the grid where
     * traders are 1 and numbers rise from there.
     *
     * @return Tier number
     */
    public int getTier() {
        int kid_cur;
        int kid_max;
        
        if( gridTier != 0 )
            return gridTier;
        
        kid_max = 0;
        for(Grid kid: children) {
            kid_cur = kid.getTier();
            if( kid_cur>kid_max )kid_max = kid_cur;
        }

        gridTier = kid_max + 1;
        return gridTier;
    }

    /**
     * Write an agent's price and quantity record to the output file
     */
    void writePQ() {
        int q = 0;
        if( this instanceof Trader )
            q = demDn.getQ(priceDn);
        Env.saveResult(this,priceDn,q);           
    }
}