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
     * Build a list of strings representing the bid
     * 
     * Within each bid the strings will be p, q_min, q_max.
     * 
     * @return List of strings representing bids 
     */
    public ArrayList<String> toStrings() {
        ArrayList<String> list = new ArrayList<>();
        for(int i=0 ; bids[i] != null ; i++ ) {
            list.add(Integer.toString(bids[i].p));
            list.add(Integer.toString(bids[i].q_min));
            list.add(Integer.toString(bids[i].q_max));
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
        int i;

        if( price <= -1 )
            return 0;

        for(i=0 ; (bids[i] != null)  && (bids[i].p < price) ; i++);

        assert bids[i] != null ;
                
        if(bids[i].p == price)     //where the price is the same as step price
            return bids[i].q_min;
        else                       //where the price is between two steps
            return bids[i].q_max;
    }

    /**
     * Find the actual price for the end users 
     *
     * Includes transaction cost and capacity limit
     * 
     * @param pr Tentative price
     * @param pc0 lower bound of Q=0 range for upstream price
     * @param pc1 upper bound of Q=0 range for upstream price
     * @param cost Transmission cost
     * @param cap Transmission capacity
     * @return Actual price
     */
    public int getP(int pr, int pc0, int pc1, int cost, int cap) { 
        int report = 0;
        int i;
        int p;

        if( pr <= -1 )
            return -1;
        
        // if the price is between local balance price +c/-c

        if ((pr >= pc0) && (pr <= pc1)) {
            return ((pc0 + pc1) / 2);
        }
		
        // if the price is more than local balance price +c

        if (pr > pc1) {
            p = 0;

            //skip the bids with more quantity than (-1) * cap & less price than the balance price
            for (i = 0; ((bids[i] != null)
                        && (bids[i].p <= pr)
                        && (bids[i].q_max >= ((-1) * cap))); i++)
                            p = bids[i].p;

            if (bids[i] == null) {
                return p;
            }
            
            //if the target step passed the cap line
            if (bids[i].q_max < ((-1) * cap)) {
                if(pr <= p + cost) 
                    return pr - cost;
                else
                    return p;
            }
            
            //put limit equal to cap
            return pr - cost;
        } 
		
        // if the price is less than local balance price +c    

        if (pr < pc0) {
            p = 0;

            //skip the bids with more quantity than cap
            for (i = 0; ((bids[i] != null)
                    && (bids[i].q_min >= cap)); i++)
                         p = bids[i].p;

            if (bids[i] == null) {
                return p;
            } 
            
            if (pr > bids[i].p - cost) 
                return pr + cost;

            //put limit equal to cap
            return bids[i].p;
        }

        assert false;
        return report;
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
    public Demand addCost(int c, Agent agent) {
        Demand newD;
        Bidstep[] tmp;
        int i;

        newD = new Demand();
        tmp  = newD.bids;

        //decrease the price level of steps with positive quantity

        for(i=0 ; (bids[i] != null) && (bids[i].q_min >= 0) ; i++)
            tmp[i] = new Bidstep(bids[i].p-c,bids[i].q_min,bids[i].q_max);
        
        //if there is no step with positive quantity 

        if(i == 0){
            for( ; bids[i] != null ; i++)
                tmp[i] = new Bidstep(bids[i].p+c,bids[i].q_min,bids[i].q_max);
        }

        if(bids[i] != null ){
            //if there is a vertical overlap with y axis
            if(bids[i].q_max == 0){
                
                //set the two upper and lower limits around the balance price
                int mid = ((bids[i-1].p + bids[i].p)/2);
                agent.setPc(mid,c);

                //increase the price level of steps with positive quantity
                for(; bids[i] != null ; i++ )
                    tmp[i] = new Bidstep(bids[i].p+c,bids[i].q_min,bids[i].q_max);
            
            //if a horizontal step indicate the balance price
            } else {
                
                //set the two upper and lower limits around the balance price
                agent.setPc(bids[i-1].p,c);

                //divide the middle step into two steps with +c/-c prices
                
                tmp[i]   = new Bidstep(bids[i].p-c,0,bids[i].q_max);
                tmp[i+1] = new Bidstep(bids[i].p+c,bids[i].q_min,0);
                i++;
                
                //increase the price level of steps with positive quantity
                
                for(; bids[i] != null ; i++)
                    tmp[i+1] = new Bidstep(bids[i].p+c,bids[i].q_min,bids[i].q_max);
            }
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
    Demand adjustTrans(Agent agent) {
        Demand newD;
        newD = this.addCost(agent.cost, agent);
        newD = newD.addCapacity(agent.cap);
        return newD;
    }
}
