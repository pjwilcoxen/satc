import java.io.IOException;
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
    
    // A complete curve is a list of steps ordered by price
    
    TreeMap<Integer,Bidstep> bids;

    // Transmission parameters if this is an upstream curve

    int cost;
    int cap;
    Integer pcap_hi;
    Integer pq0_hi ;
    Integer pq0_lo ;
    Integer pcap_lo;

    /**
     * Demand curve
     */
    public Demand() {
        bids = new TreeMap<>();
        cost    = 0;
        cap     = 0;
        pcap_hi = null;
        pq0_hi  = null;
        pq0_lo  = null;
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
    private Bidstep getBid(Integer p) {
        return bids.get(p);
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

        // case 1: horizontal part of a step. for backward compatibility,
        // return q_min. eventually this should return a random number
        // between q_min and q_max
        
        if( pHi != null && pHi.equals(pLo) ) {
            bid = getBid(pHi);
            return bid.q_min;
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
     * Includes transaction cost and capacity limit
     * 
     * @param pUp Tentative price
     * @param agent Agent whose transmission parameters should be used
     * @return Actual price
     */
    public int getPriceDn(int pUp, Demand demUp) { 
        Bidstep bid;
        int last_p;
        int pDn;

        int pc0  = demUp.pq0_lo;
        int pc1  = demUp.pq0_hi;
        int cost = demUp.cost;
        int cap  = demUp.cap;

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
            
            for(Integer p: prices()) {
                bid = getBid(p); 
                if( p <= pUp && bid.q_max >= -cap )
                    last_p = p;
                else if( bid.q_max < -cap ) 
                    return pDn <= last_p ? pDn : last_p ;
                else
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
       
        for(Integer p: prices()) {
            bid = getBid(p);
            if( bid.q_min >= cap ) {
                last_p = p;
                continue;
            }
            return pDn > p ? pDn : p ;
        }

        // nothing left: constraint is binding and return last price
        
        return last_p;
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
        int q_max;
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
        // combine bids until we run off the top of either curve
        //
        // treats sum as undefined above top of first curve to 
        // run out of bids; may want to reconsider this
        //

        iterL = demL.prices().iterator();
        iterR = demR.prices().iterator();

        Integer pL = iterL.next();
        Integer pR = iterR.next();

        last_p = null;

        while( true ) {

            // get new bids if needed

            l = demL.getBid(pL);
            r = demR.getBid(pR);

            // find the price and q_max of the next step

            p = pL < pR ? pL : pR ;
            q_max = l.q_max + r.q_max ;

            newD.add(p,0,q_max);
            
            // fix q_min on the previous step and save it

            if( last_p != null )
                newD.bids.get(last_p).q_min = q_max;

            last_p = p;

            // pull next needed bid(s). a little convoluted so
            // we don't break the second update after doing
            // the first.

            //
            // eventually this should be smarter and assume that when 
            // the first curve runs out we keep using 0 if it was 
            // a demand curve or q_min if it was a supply curve
            //
            
            needL = pL <= pR;
            needR = pR <= pL;
            
            if( needL ) {
                if( !iterL.hasNext() )break;
                pL = iterL.next();
            }

            if( needR ) {
                if( !iterR.hasNext() )break;
                pR = iterR.next();
            }
        }

        // last step

        newD.add(last_p, q_max-100, q_max);

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
            newD.add(p0, q1, q2);
        else
            newD.add(p0, q2, q1);
        
        // create the steps below the price=40
        
        for(int i=1 ; i<steps ; i++){
            p1 = iniprice*(i+1)/steps;
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            if( makeS )
                newD.add(p1, q1, q2);
            else
                newD.add(p1, q2, q1);
        }
        
        // create twice the number of steps above price=40

        for(int i=1 ; i<2*steps ; i++){
            p1 = iniprice + 360*i/(2*steps);
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            if( makeS )
                newD.add(p1, q1, q2);
            else
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
    Demand adjustTrans(Grid agent) {
        Demand newD;
        Bidstep old;
        int cost;
        int cap;
        int q_min;
        int q_max;
        
        cost = agent.cost;
        cap  = agent.cap;

        newD = new Demand();
        newD.cost   = cost;
        newD.cap    = cap;
        newD.pq0_lo = -1;
        newD.pq0_hi = -1;

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

            if( q_max > cap ) {
                q_max = cap;
                pcap_lo = p-cost;
            }

            if( q_min < -cap ) {
                q_min = -cap;
                pcap_hi = p+cost;
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
            if( bid.q_min == 0 )newD.pq0_lo = p;
            if( bid.q_max == 0 )newD.pq0_hi = p;
        }

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
}
