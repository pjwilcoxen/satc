import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Set;
import java.util.Iterator;
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

        Bidstep(int q_min, int q_max) {
            this.q_min = q_min;
            this.q_max = q_max;
        }
    }
    
    /**
     * Types of demand curve
     */
    
    public static enum Type {
        /**
         * Basic curve for traders
         */
        BASE,
        /**
         * Market down demand
         */
        DOWN,
        /**
         * Market up demand
         */
        UP,
        /**
         * Demand received from third party
         */
        REC
    }

    // A complete curve is a list of steps ordered by price
    
    TreeMap<Integer,Bidstep> bids;

    // Transmission parameters if this is an upstream curve

    boolean isUp;
    int cost;
    int cap;
    Integer pcap_hi;
    Integer min_wta;
    Integer max_wtp;
    Integer pcap_lo;

    /**
     * Demand curve
     */
    public Demand() {
        bids    = new TreeMap<>();
        isUp    = false;
        cost    = 0;
        cap     = 0;
        pcap_hi = null;
        min_wta = null;
        max_wtp = null;
        pcap_lo = null;
    }

    /**
     * Add a step to the curve
     *
     * @param p Price
     * @param q_min Minimum quantity
     * @param q_max Maximum quantity
     */
    public void add(int p, int q_min, int q_max) {
        Bidstep newbid = new Bidstep(q_min,q_max);
        bids.put( p, newbid );
    }

    /**
     * Add a step to the curve
     *
     * @param bid Bid to add
     */
    private void add(int p, Bidstep bid) {
        bids.put(p,bid);
    }

    /**
     * List of prices in this curve
     * 
     * @return Set of prices in ascending order
     */
    public Set<Integer> prices() {
        return bids.keySet();
    }

    /**
     * No bids?
     * 
     * @return True if the curve has no bids
     */
    public boolean isEmpty() {
        return bids.isEmpty();
    }

    /**
     * Get the bid for a given price
     */
    public Bidstep getBid(Integer p) {
        Bidstep step = bids.get(p);
        if( step == null )
            throw new RuntimeException("No bid exists for given price");
        return step;
    }

    /**
     * Aggregate a list of demand curves
     *
     * @param dList ArrayList of Demand curves to be aggregated
     * @return new Demand curve
     */
    public static Demand agg(ArrayList<Demand> dList) {
        Demand newD = null;
        if( dList.isEmpty() )
            throw new RuntimeException("Empty list in Demand.agg");
        for(Demand curD: dList) 
            newD = aggTwo(newD,curD);
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
        bids.forEach( (p,bid) -> {
            slist.add(Integer.toString(p));
            slist.add(Integer.toString(bid.q_min));
            slist.add(Integer.toString(bid.q_max));
        }); 
        return slist;
    }

    /**
     * Calculate the net demand at a given price
     * 
     * @param price Price 
     * @return Quantity
     */
    public int getQ(int price) {
        Integer pHi;
        Integer pLo;
        Bidstep bid;

        if( price <= -1 )
            return 0;

        // look for the nearest bids above and below the current price

        pHi = bids.ceilingKey(price);
        pLo = bids.floorKey(price);

        // case 1: horizontal part of a step. return value closest to
        // the vertical axis (minimum absolute value)
        
        if( pHi != null && pHi.equals(pLo) ) {
            bid = getBid(pHi);
            if( Math.abs(bid.q_min) < Math.abs(bid.q_max) )
                return bid.q_min;
            else
                return bid.q_max;
        }

        // case 2: vertical part of a step. return q_max of pHi but 
        // it will be the same as q_min of pLo
        
        if( pHi != null && pLo != null ) {       
            bid = getBid(pHi);
            return bid.q_max;
        }

        // case 3: below the first bid: return q_max of the lowest bid.
        // assumes that there's an implied step down to p=0 from the 
        // rightmost point on either a demand, net demand or pure supply 
        // curve.
        
        if( pLo == null ) {       
            bid = getBid(pHi);
            return bid.q_max;
        }

        // case 4: above the last bid: return q_min of the highest bid.
        // assumes that either a demand or a supply curve continue 
        // up indefinitely from the last left-most point.
        
        bid = getBid(pLo);
        return bid.q_min;
    }

    /**
     * Find the actual price for the end users 
     *
     * Includes transaction cost and capacity limit. Must be applied
     * to an upstream demand curve.
     * 
     * @param pUp Tentative price
     * @return Actual price
     */
    public int getPriceDn(int pUp) { 
        boolean has_s;
        boolean has_d;

        assert isUp;

        if( pUp <= -1 )
            return -1;
        
        has_s = min_wta != null;
        has_d = max_wtp != null;

        assert has_s || has_d;

        // case 1: selling. at or above the constraint on upstream sales.
        // return the downstream price associated with the constraint

        if( has_s && pcap_hi != null && pUp >= pcap_hi )
            return pcap_hi - cost;

        // case 2: selling. above minimum wta. assume selling and
        // return the downstream price associated with pUp

        if( has_s && pUp > min_wta ) 
            return pUp - cost;
        
        // case 3: no trade. supply only and below min wta. return 
        // the downstream selling price

        if( has_s && !has_d )
            return pUp - cost;

        // if we're here there must have a demand portion. assert 
        // that and then don't check for it 

        assert has_d;

        // case 4: no trade. in the zone between min_wta and 
        // max_wtp where q will be zero

        if( has_s && pUp >= max_wtp && pUp <= min_wta)
            return (max_wtp + min_wta)/2;
       
        // case 5: no trade. above max_wtp. return downstream 
        // buying price

        if( pUp > max_wtp )
            return pUp + cost;

        // case 6: buying. below max_wta and above the demand cap.
        // return the downstream price

        if( pcap_lo == null || pUp > pcap_lo ) 
            return pUp + cost;

        // case 7: buying. at or below the constraint on demand. return
        // the downstream price associated with the constraint

        return pcap_lo + cost;
    }

    /**
     * Aggregate two curves
     * 
     * @param demL Left demand curve
     * @param demR Right demand curve
     * @return New demand curve
     */
    private static Demand aggTwo(Demand demL, Demand demR) {
        Iterator<Integer> iterL;
        Iterator<Integer> iterR;
        Demand newD;
        Bidstep new_bid;
        Bidstep l;
        Bidstep r;
        boolean needL;
        boolean needR;
        int p;
        Integer q_max;
        Integer q_min;
        Integer last_p;

        assert demR != null;
        
        newD = new Demand();
        
        // special case for convenience in agg: left demand is null 
        // and right demand is not; make newD identical to demR
        
        if( demL == null ) {
            demR.bids.forEach( (pL,bid) -> { 
                newD.add(pL,bid); 
            });
            return newD;
        }
        
        if( demL.isEmpty() || demR.isEmpty() )return newD;

        // 
        // combine bids until we run off the top of both curves
        //

        iterL = demL.prices().iterator();
        iterR = demR.prices().iterator();

        Integer pL = iterL.next();
        Integer pR = iterR.next();

        l = demL.getBid(pL);
        r = demR.getBid(pR);

        last_p = null;
        q_max  = null;
        q_min  = null;

        while( pL != null || pR != null ) {

            // find the price and q_max of the next step

            if( pL != null && pR != null ) {
                p = pL < pR ? pL : pR ;
                q_max = l.q_max + r.q_max ;
                q_min = l.q_min + r.q_min ;
                needL = pL <= pR;
                needR = pR <= pL;
            }
            else if( pR == null ) {
                p = pL;
                q_max = l.q_max + r.q_min ;
                q_min = l.q_min + r.q_min ;
                needL = true;
                needR = false;
            }
            else {
                p = pR;
                q_max = l.q_min + r.q_max ;
                q_min = l.q_min + r.q_min ;
                needL = false;
                needR = true;
            }

            newD.add(p,0,q_max);
            
            // fix q_min on the previous step and save it

            if( last_p != null )
                newD.bids.get(last_p).q_min = q_max;

            last_p = p;

            // pull next needed bids
            
            if( needL ) 
                if( iterL.hasNext() ) {
                    pL = iterL.next();
                    l = demL.getBid(pL);
                }
                else
                    pL = null;

            if( needR ) 
                if( iterR.hasNext() ) {
                    pR = iterR.next();
                    r = demR.getBid(pR);
                }
                else
                    pR = null;
        }

        // last step

        newD.add(last_p, q_min, q_max);

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
        newD.add(p0, q2, q1);
        
        // create the steps below the price=40
        
        for(int i=1 ; i<steps ; i++){
            p1 = iniprice*(i+1)/steps;
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            newD.add(p1, q2, q1);
        }
        
        // create twice the number of steps above price=40

        for(int i=1 ; i<2*steps ; i++){
            p1 = iniprice + 360*i/(2*steps);
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            newD.add(p1, q2, q1);
        }
        
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
        for(Integer p : prices()) {
            Bidstep bid = getBid(p);

            // case 1: crossing is a horizontal segment
            
            if( bid.q_min < 0 && bid.q_max > 0 )
                return p;

            // case 2: crossing is a vertical segment; for backward
            // compatibility return higher of the two prices
            //
            // eventually this should return the midpoint between
            // the upper and lower prices
            //
            
            if( bid.q_max == 0 )
                return p;
        }
        
        // curve doesn't cross the y axis
        
        return -1;
    }

    /**
     * Adjust aggregate demand for transmission parameters
     * 
     * @param agent Grid agent
     * @return New demand curve
     */
    Demand addTrans(Grid agent) {
        Demand newD;
        Bidstep old;
        int cost;
        int cap;
        int q_min;
        int q_max;
        
        cost = agent.cost;
        cap  = agent.cap;

        newD = new Demand();
        newD.isUp = true;
        newD.cost = cost;
        newD.cap  = cap;

        for(Integer p: prices()) {

            // get next step moving up the curve

            old   = getBid(p);
            q_min = old.q_min;
            q_max = old.q_max;
 
            // skip it if it's beyond the cap on either side

            if( q_min >  cap )continue;
            if( q_max < -cap )continue;

            // impose the constraints and note the prices
            // where they occur

            if( q_max >= cap ) {
                q_max = cap;
                newD.pcap_lo = p-cost;
            }

            if( q_min < -cap ) {
                q_min = -cap;
                newD.pcap_hi = p+cost;
            }

            // shift pure demand bids down

            if( q_min >= 0 ) {
                newD.add(p-cost, q_min, q_max);
                continue;
            }

            // shift pure supply bids up

            if( q_max <=0 ) {
                newD.add(p+cost, q_min, q_max);
                continue;
            }

            // we're left with a horizontal step with q_min<0 and q_max>0
            // split it and add the new pieces

            newD.add(p-cost,     0, q_max);
            newD.add(p+cost, q_min,     0);
        }

        // find the deadband prices
        //
        // done this way for backward compatibility.  eventually needs 
        // to be more robust: it relies on there being at most one 
        // crossing, and the curve having a negative slope.
        //

        for(Integer p: newD.prices()) {
            Bidstep bid = newD.getBid(p);
            if( bid.q_min >= 0 )
                if( newD.max_wtp == null || newD.max_wtp < p )
                    newD.max_wtp = p;
            if( bid.q_max <= 0 )
                if( newD.min_wta == null || newD.min_wta > p )
                    newD.min_wta = p;
        }

        return newD;
    }

    /**
     * Print this demand to the log file
     * 
     * @param owner Agent owning this demand curve
     * @param dtype Type of demand; see Demand.Type
     */
    public void log(Agent owner, Type dtype ) {
        int key;
        String dstr;
        
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
        header.add("steps");
        for (int i = 0; i < 400; i++) {
            header.add("p" + i);
            header.add("q_min" + i);
            header.add("q_max" + i);
        }
        
        key = 10*owner.own_id ;
        switch( dtype ) {
            case BASE: key += 0; dstr="base"; break;
            case DOWN: key += 1; dstr="down"; break;
            case UP  : key += 2; dstr="up"  ; break;
            case REC : key += 3; dstr="rec" ; break;
            default:
                throw new RuntimeException("Unexpected demand type");
        }
        
        values = new ArrayList<>();
        values.add(Integer.toString(Env.pop));
        values.add(Integer.toString(owner.own_id));
        values.add(dstr);
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
        values.add(Integer.toString(bids.size()));
        for (String str : toStrings()) {
            values.add(str);
        }
        Env.saveDemand(key, header, values);
    }
}
