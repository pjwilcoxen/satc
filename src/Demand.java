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
     * Maximum steps in a demand curve
     */
    public static final int MAXBIDS = 400;

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
        
        Bidstep shift_p(int dp) {
            return new Bidstep(p+dp,q_min,q_max);
        }
    }
    
    // A complete curve is a list of steps
    
    Bidstep[] bids;

    /**
     * Demand curve
     */
    public Demand() {
        bids = new Bidstep[MAXBIDS];
    }

    /**
     * Convert the curve to a list
     */
    private ArrayList<Bidstep> asList() {
        ArrayList<Bidstep> bidlist = new ArrayList<>();
        for(int i=0 ; bids[i] != null ; i++)
            bidlist.add(bids[i]);
        return bidlist;
    }

    /**
     * Build a list of strings representing the bid
     * 
     * Within each bid the strings will be p, q_min, q_max.
     * 
     * @return List of strings representing bids 
     */
    public ArrayList<String> toStrings() {
        ArrayList<String> list = new ArrayList<>();
        for(Bidstep bid: asList()) {
            list.add(Integer.toString(bid.p));
            list.add(Integer.toString(bid.q_min));
            list.add(Integer.toString(bid.q_max));
        }
        return list;
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

        for(Bidstep bid: asList())
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
        Bidstep bid;
        int i;
        int p;
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
            p = 0;

            // downstream price received by sellers if the constraint isn't 
            // binding: it's the upstream price less the transmission cost
        
            pDn = pUp - cost;

            // scan up the bids for first one with p > pUp or q_max to the left of cap
            
            for (i=0; bids[i] != null && bids[i].p <= pUp && bids[i].q_max >= -cap; i++)
                p = bids[i].p;

            // nothing left: return the last price
            
            if( bids[i] == null )
                return p;
            
            // was the cap binding? if so, return which ever is smaller:
            // pDn or this step's p.
            
            if( bids[i].q_max < -cap ) 
                if( pDn <= p ) 
                    return pDn ;
                else
                    return p;
            
            // constraint wasn't binding so return pDn

            return pDn;
        } 
        
        // price must be below the lower threshold so we're in the
        // demand zone

        p = 0;

        // downstream price paid by buyers if the constraint isn't 
        // binding: it's the upstream price plus the transmission cost
        
        pDn = pUp + cost;

        // skip up the curve for bids with more quantity than cap
        
        for(i=0; bids[i] != null && bids[i].q_min >= cap ; i++)
            p = bids[i].p;

        // nothing left: constraint is binding and return last price
        
        if (bids[i] == null) 
            return p;
        
        // found first bid that doesn't violate the capacity constraint.
        // if the unconstrained downstream price pDn is higher than it, 
        // the constraint isn't binding so return pDn
        
        if ( pDn > bids[i].p) 
            return pDn;

        // ok, the constraint is binding: the bid price is higher than 
        // pDn so the local price will rise to that instead.
        
        return bids[i].p;
    }

    /**
     * Add capacity constraint on the net demand
     * 
     * @param cap Transmission capacity
     * @return New demand curve
     */
    public Demand addCapacity(int cap) {
        Demand newD;
        Bidstep[] tmp;
        int i;
        int j;
        
        newD = new Demand();
        tmp  = newD.bids;

        j = 0;

        //skip the steps with a minimum greater than the cap

        for(i=0; bids[i].q_min >= cap; i++);
        
        if(bids[i] == null)
            return newD;

        //set the right corner step
        
        if(bids[i].q_max > cap){
            tmp[j++]= new Bidstep(bids[i].p,bids[i].q_min,cap);
            i++;
        }

        //consider the steps between two capacity limits
        
        for( ; (bids[i] != null) && (bids[i].q_min >= ((-1)*(cap))) ; i++)
            tmp[j++] = new Bidstep(bids[i].p,bids[i].q_min,bids[i].q_max);

        if (bids[i] == null) 
            return newD;

        //set the left corner step
        tmp[j] = new Bidstep(bids[i].p,((-1)*(cap)),bids[i].q_max);
        
        return newD;
    }

    /**
     * Aggregate two net demand curves
     * 
     * @param dem2 Demand curve to add to this one
     * @return New demand curve
     */
    public Demand aggregateDemand(Demand dem2) {
        Demand aggD;
        Bidstep[] aggBid;
        Bidstep[] bid1;
        Bidstep[] bid2;
        int i;
        int j;
        int k;
        int min_p; 

        aggD   = new Demand();
        aggBid = aggD.bids;
        bid1   = bids;
        bid2   = dem2.bids;

        i=0;
        j=0;
        k=0;
        
        //screen all the steps in the two input array of bids
        while((bid1[i]!= null)&&(bid2[j]!=null)){
            
            //initiate the aggregate step considering the minimum price level and sum of the right corners as the max quantity
            min_p = bid1[i].p < bid2[j].p ? bid1[i].p : bid2[j].p ;
            aggBid[k] = new Bidstep(min_p,0,bid1[i].q_max + bid2[j].q_max);
            
            if(bid1[i].p < bid2[j].p){
                //if it is the fist step
                if(k == 0)
                    aggBid[k].q_min = bid1[i].q_min + bid2[j].q_max;
                i++;
            } 
            else if(bid1[i].p > bid2[j].p) {
                //initiate the left corner of the first step
                if(k == 0)
                    aggBid[k].q_min = bid1[i].q_max + bid2[j].q_min;
                j++;
            }
            else { 
                //initiate the left corner of the first step
                if(k == 0)
                    aggBid[k].q_min = bid1[i].q_min + bid2[j].q_min;
                i++;
                j++;
            }

            //initiate the left corner of each step based on the right corner of the next step
            if(k > 1)
                aggBid[k-1].q_min = aggBid[k].q_max;
            
            k++;
            
          }

        //initiate the last step
        aggBid[k-1].q_min = aggBid[k-1].q_max - 100;
        
        return aggD;
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
        Bidstep[] result;

        newD = new Demand();
        result = newD.bids;
        boolean makeS;
        int sign;

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
            result[0] = new Bidstep(p0, q1, q2);
        else
            result[0] = new Bidstep(p0, q2, q1);
        
        // create the steps below the price=40
        
        for(int i=1 ; i<steps ; i++){
            p1 = iniprice*(i+1)/steps;
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            if( makeS )
                result[i] = new Bidstep(p1, q1, q2);
            else
                result[i] = new Bidstep(p1, q2, q1);
        }
        
        // create twice the number of steps above price=40

        for(int i=1 ; i<2*steps ; i++){
            p1 = iniprice + 360*i/(2*steps);
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            if( makeS )
                result[steps+i-1] = new Bidstep(p1, q1, q2);
            else
                result[steps+i-1] = new Bidstep(p1, q2, q1);
        }
        
        return newD;
    }

    /** 
     * Find an equilibrium price for a net demand curve
     * 
     * Returns the highest price with a positive q_max and a negative
     * q_min.
     * 
     * @return The equilibrium price for this net demand curve
     */
    public int getEquPrice() {
        int i;
        int bl;
        int min;

        bl  = -2;
        min = 1;

        for(i=0 ; (bids[i] != null) && (bids[i].q_max >= 0) ; i++) {
            bl  = bids[i].p;
            min = bids[i].q_min;
        }

        // no equilibrium because there are no steps with positive q_max
        
        if( i==0 )
            return -1;
        
        // no equilibrium because we've run off the top end of the 
        // demand curve and the minimum demanded is still positive
        
        if( (bids[i] == null) && (min > 0) ) 
            return -1;

        // make sure something was found
        
        assert bl != -2;

        return bl;
    }

    /**
     * Change the step prices to account for the transmission cost
     * 
     * @param c Transmission cost
     * @param agent Midlevel market
     * @return New demand curve
     */
    public Demand addCost(int c, Grid agent) {
        Demand newD;
        Bidstep[] tmp;
        int i;
        int mid;

        newD = new Demand();
        tmp  = newD.bids;

        // decrease the price level of steps with positive quantity

        for(i=0 ; (bids[i] != null) && (bids[i].q_min >= 0) ; i++)
            tmp[i] = bids[i].shift_p(-c);
        
        // raise the price if this is a pure supply curve with 
        // all steps to the left of the y axis

        if(i == 0)
            for( ; bids[i] != null ; i++)
                tmp[i] = bids[i].shift_p(c);

        // return if there aren't any more steps, which means we're 
        // not crossing the y axis

        if( bids[i] == null )
           return newD;

        // ok, started out positive but the curve crosses the y axis. 
        // there are two cases: (1) the vertical part of the step lies
        // on the y axis; and (2) the vertical part of the step lies 
        // to the left of the y axis

        if(bids[i].q_max == 0){
            
            // case 1: step has a vertical overlap with y axis

            // set the two upper and lower limits around the balance price

            mid = ((bids[i-1].p + bids[i].p)/2);
            agent.setPc(mid,c);

            // bump up the remaining supply steps

            for(; bids[i] != null ; i++ )
                tmp[i] = bids[i].shift_p(c);
        
        } else {
            
            // case 2: step is to the left of the y axis
            
            // set the two upper and lower limits around the balance price

            agent.setPc(bids[i-1].p,c);

            // divide the middle step into two steps with +c/-c prices
            
            tmp[i] = bids[i].shift_p(-c);
            tmp[i].q_min = 0;

            tmp[i+1] = bids[i].shift_p(c);
            tmp[i+1].q_max = 0;

            i++;
            
            // bump up the remaining supply steps
            
            for(; bids[i] != null ; i++)
                tmp[i+1] = bids[i].shift_p(c);
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

    /**
     * Adjust aggregate demand for transmission parameters
     *
     * @param agg Original demand curve
     * @return Curve adjusted for transmission
     */
    Demand adjustTrans(Grid agent) {
        Demand newD;
        newD = this.addCost(agent.cost, agent);
        newD = newD.addCapacity(agent.cap);
        return newD;
    }
}
