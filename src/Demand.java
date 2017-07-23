import java.io.IOException;
import java.util.ArrayList;
import static java.lang.Math.pow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Class for holding and manipulating net demand curves
 */
public class Demand {

    /**
     * One step of a demand or supply curve
     */
    static class Bidstep {
        int p;
        int q_min;
        int q_max;

        Bidstep(int p, int q_min, int q_max) {
            this.p     = p;
            this.q_min = q_min;
            this.q_max = q_max;
        }

        Bidstep(Bidstep old) {
            p     = old.p;
            q_min = old.q_min;
            q_max = old.q_max;
        }
        
        Bidstep shift_p(int dp) {
            return new Bidstep(p+dp,q_min,q_max);
        }

    }
    
    // A complete curve is a list of steps
    
    ArrayList<Bidstep> list;

    /**
     * Demand curve
     */
    public Demand() {
        list = new ArrayList<>();
    }

    /**
     * Add a step to the list
     *
     * @param bid Bid to add
     */
    void add(Bidstep bid) {
        list.add(bid);
    }

    /**
     * Enforce some sanity checks
     *
     * @param msg Message showing where check was called
     */
    void check(String msg) {
        int last_p = -1000;
        for(Bidstep bid: list) {
            if( bid.p < last_p ) 
                throw new RuntimeException("Non-ascending bids "+msg);
            last_p = bid.p;
        }
    }

    /**
     * Aggregate a list of demand curves
     *
     * @param dList ArrayList of Demand curves to be aggregated
     * @return new Demand curve
     */
    static Demand agg(ArrayList<Demand> dList) {
        Demand newD = null;
        if( dList.isEmpty() )
            throw new RuntimeException("Empty list in Demand.agg()");
        for(Demand curD: dList) 
            if( newD == null )
                newD = curD;
            else
                newD = newD.aggregateDemand(curD);
        newD.check("after agg");
        return newD;
    }
     
    /**
     * Build a list of strings representing the bid
     * 
     * Within each bid the strings will be p, q_min, q_max.
     * 
     * @return List of strings representing bids 
     */
    public ArrayList<String> toStrings() {
        ArrayList<String> slist = new ArrayList<>();
        for(Bidstep bid: list) {
            slist.add(Integer.toString(bid.p));
            slist.add(Integer.toString(bid.q_min));
            slist.add(Integer.toString(bid.q_max));
        }
        return slist;
    }

    /**
     * Calculate the net demand at a given price
     * 
     * @param price Price 
     * @return Quantity
     */
    public int getQ(int price) {

        if( price <= -1 )
            return 0;

        // find the first step at or above the given price

        for(Bidstep bid: list)
            if( bid.p == price)     
                return bid.q_min;
            else if( bid.p > price)
                return bid.q_max;
        
        // didn't find one

        return -1;
    }

    /**
     * Find the actual price for the end users 
     *
     * Includes transaction cost and capacity limit
     * 
     * @param pUp Tentative price
     * @param agent Agent whose transmission parameters should be used
     * @return Actual price
     */
    public int getPriceDn(int pUp, Grid agent) { 
        int last_p;
        int pDn;

        int pc0  = agent.p_notrans_lo;
        int pc1  = agent.p_notrans_hi;
        int cost = agent.cost;
        int cap  = agent.cap;

        if( pUp <= -1 )
            return -1;
        
        // is the upstream price is between the Q=0 thresholds? if so,
        // return the middle of the range

        if ( pUp >= pc0 && pUp <= pc1 )
            return (pc0 + pc1)/2;
        
        // is the price above the upper threshold and thus in the 
        // supply zone?

        if (pUp > pc1) {
            last_p = 0;

            // downstream price received by sellers if the constraint isn't 
            // binding: it's the upstream price less the transmission cost
        
            pDn = pUp - cost;

            // scan up the bids for first one with p > pUp or q_max to the left of cap
            
            for(Bidstep bid: list) {
                if( bid.p <= pUp && bid.q_max >= -cap ) {
                    last_p = bid.p;
                    continue;
                }
                if( bid.q_max < -cap ) 
                    return pDn <= last_p ? pDn : last_p ;
                return pDn;
            }

            // nothing left: return the last price
            
            return last_p;
        } 
        
        // price must be below the lower threshold so we're in the
        // demand zone

        last_p = 0;

        // downstream price paid by buyers if the constraint isn't 
        // binding: it's the upstream price plus the transmission cost
        
        pDn = pUp + cost;

        // skip up the curve for bids with more quantity than cap
       
        for(Bidstep bid: list) {
            if( bid.q_min >= cap ) {
                last_p = bid.p;
                continue;
            }
            return pDn > bid.p ? pDn : bid.p ;
        }

        // nothing left: constraint is binding and return last price
        
        return last_p;
    }

    /**
     * Add capacity constraint on the net demand
     * 
     * @param cap Transmission capacity
     * @return New demand curve
     */
    public Demand addCapacity(int cap) {
        Bidstep newbid;
        Demand newD;

        newD = new Demand();
        for(Bidstep old: list) {
        
            // skip demands beyond cap to the right or left

            if( cap <= old.q_min )continue;
            if( old.q_max < -cap )continue;

            // create a new step

            newbid = new Bidstep(old);

            // impose the constraints, if necessary

            if( newbid.q_max >  cap ) newbid.q_max =  cap;
            if( newbid.q_min < -cap ) newbid.q_min = -cap;

            // add it

            newD.add(newbid);
        }

        newD.check("after addCapacity");
        return newD;
    }

    /**
     * Aggregate two net demand curves
     * 
     * @param dem2 Demand curve to add to this one
     * @return New demand curve
     */
    public Demand aggregateDemand(Demand dem2) {
        Demand newD;
        Bidstep new_bid;
        Bidstep l;
        Bidstep r;
        int p;
        int q_max;
        int numL;
        int numR;
        int iL;
        int iR;

        newD = new Demand();

        // 
        // combine bids until we run off the top of either curve
        //
        // treats sum as undefined above top of first curve to 
        // run out of bids; may want to reconsider this
        //

        new_bid = null;

        numL = list.size();
        numR = dem2.list.size();

        iL = 0;
        iR = 0;

        while( iL < numL && iR < numR ) {
           
            // name the bids left and right for convenience

            l = list.get(iL);
            r = dem2.list.get(iR);

            // find the price and q_max of the next step

            p = l.p < r.p ? l.p : r.p ;
            q_max = l.q_max + r.q_max ;

            // fix q_min on the previous step and save it

            if( new_bid != null ) {
                new_bid.q_min = q_max;
                newD.add(new_bid);
            }

            // create the new step with a placeholder for q_min

            new_bid = new Bidstep(p,0,q_max);
           
            // figure out which bid(s) to pull next

            if( l.p <= r.p )iL++;
            if( r.p <= l.p )iR++;
        }

        // last step

        if( new_bid != null ) {
            new_bid.q_min = new_bid.q_max - 100;
            newD.add(new_bid);
        }

        newD.check("after aggregateDemand");
        return newD;
    }
    
    /**
     * Create demand curves based on initial load, elasticity, and number of steps
     * 
     * @param trader Trader whose demand we're building
     * @return New demand curve
     */
    public static Demand makeDemand(Trader trader) {
        return do_make("D",trader);
    }
    
    /**
     * Create supply curves with reverse quantities in comparison to demand curve
     * 
     * @param trader Trader whose supply we're building
     * @return New demand
     */
    public static Demand makeSupply(Trader trader) {
        return do_make("S",trader);
    }
  
    /**
     * Build a demand or supply curve 
     */
    private static Demand do_make(String type, Trader trader) {
        Demand newD;
        boolean makeS;
        int sign;

        newD = new Demand();

        // get information about the trader; make local copies
        // for slightly greater clarity in calculations

        double elast  = trader.elast;
        double load   = trader.load;
        int    steps  = trader.steps;
        double rPrice = trader.rPrice;

        makeS = type.equals("S"); 
        sign  = makeS ? -1 : 1 ;       
        
        int iniprice = 40 + (int) (rPrice * 12 - 6);
        
        int p0 = iniprice/steps;
        int p1 = iniprice*2/steps;

        int q1=(int)(sign*load*pow((double)p0/iniprice,elast));
        int q2=(int)(sign*load*pow((double)p1/iniprice,elast));

        // first step
        if( makeS )
            newD.add( new Bidstep(p0, q1, q2) );
        else
            newD.add( new Bidstep(p0, q2, q1) );
        
        // create the steps below the price=40
        
        for(int i=1 ; i<steps ; i++){
            p1 = iniprice*(i+1)/steps;
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            if( makeS )
                newD.add( new Bidstep(p1, q1, q2) );
            else
                newD.add( new Bidstep(p1, q2, q1) );
        }
        
        // create twice the number of steps above price=40

        for(int i=1 ; i<2*steps ; i++){
            p1 = iniprice + 360*i/(2*steps);
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            if( makeS )
                newD.add( new Bidstep(p1, q1, q2) );
            else
                newD.add( new Bidstep(p1, q2, q1) );
        }
        
        newD.check("after do_make");
        return newD;
    }

    /** 
     * Find an equilibrium price for a net demand curve
     * 
     * Returns the highest price with a nonnegative q_max and a 
     * negative q_min; otherwise return -1.
     * 
     * @return The equilibrium price for this net demand curve
     */
    public int getEquPrice() {
        for(Bidstep bid: list)
            if( bid.q_min < 0 && bid.q_max >= 0 )
                return bid.p;
        return -1;
    }

    /**
     * Change the step prices to account for the transmission cost
     * 
     * @param c Transmission cost
     * @param agent Grid agent
     * @return New demand curve
     */
    public Demand addCost(int c, Grid agent) {
        Demand newD;
        int p_lo = -1;
        int p_hi = -1;

        newD = new Demand();
        for(Bidstep old: list) {
            
            // shift pure demand bids down

            if( old.q_min >= 0 ) {
                newD.add(old.shift_p(-c));
                continue;
            }

            // shift pure supply bids up

            if( old.q_max <=0 ) {
                newD.add(old.shift_p(c));
                continue;
            }

            // we're left with a horizontal step with q_min<0 and q_max>0
            // split it and add the new pieces

            newD.add( new Bidstep(old.p-c,0,old.q_max) );
            newD.add( new Bidstep(old.p+c,old.q_min,0) );
        }

        // find the deadband prices

        for(Bidstep bid: newD.list) {
            if( bid.q_min == 0 )p_lo = bid.p;
            if( bid.q_max == 0 )p_hi = bid.p;
        }
        agent.setPc(p_lo,p_hi);

        newD.check("after addCost");
        return newD;
    }

    /**
     * Print this demand to the log file
     * 
     * @param owner Agent owning this demand curve
     * @param casetag Tag indicating DOS case
     */
    public void log(Agent owner, String casetag) {

        CSVFormat loadFormat;
        ArrayList<String> header;
        ArrayList<String> values;

        header = new ArrayList<>();
        header.add("pop");
        header.add("id");
        header.add("tag");
        header.add("dos");
        header.add("sd_type");
        header.add("load");
        header.add("elast");
        for (int i = 0; i < 20; i++) {
            header.add("p" + i);
            header.add("q_min" + i);
            header.add("q_max" + i);
        }
        values = new ArrayList<>();
        try {
            if (Env.loadPrinter == null) {
                loadFormat = CSVFormat.DEFAULT;
                Env.loadPrinter = new CSVPrinter(Env.net, loadFormat);
                Env.loadPrinter.printRecord(header);
            }
            values.add(Integer.toString(Env.pop));
            values.add(Integer.toString(owner.own_id));
            values.add(casetag);
            values.add(Env.curDOS);
            if (owner instanceof Trader) {
                Trader trader = (Trader) owner;
                values.add(trader.sd_type);
                values.add(Double.toString(trader.load));
                values.add(Double.toString(trader.elast));
            } else {
                values.add("");
                values.add("");
                values.add("");
            }
            for (String str : toStrings()) {
                values.add(str);
            }
            Env.loadPrinter.printRecord(values);
        } catch (IOException e) {
            throw new RuntimeException("Error writing to load file");
        }
    }

    /**
     * Adjust aggregate demand for transmission parameters
     *
     * @param agg Original demand curve
     * @return Curve adjusted for transmission
     */
    Demand adjustTrans(Grid agent) {
        Demand newD;
        newD = addCost(agent.cost, agent);
        newD = newD.addCapacity(agent.cap);
        newD.check("after adjustTrans");
        return newD;
    }
}
