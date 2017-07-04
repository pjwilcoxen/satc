public class Demand {
   
    static final int MAXBIDS = 400;
    public Bidstep[] bids;

    public Demand() {
        bids = new Bidstep[MAXBIDS];
    }

    public Demand(Bidstep[] bids) {
        this.bids = bids;
    }

    /**
     * Calculate the net demand at a given price
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
     */
    public int getP(int pr, int pc0, int pc1, int cost, int cap) { 
        int report = 0;
        int i;
        int p;

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
     * Add capacity constrain on the net demand
     */
    public Demand addCapacity(int cap) {
        Demand newD;
        Bidstep[] tmp;
        int i;
        int j;
        
        newD = new Demand();
        tmp  = newD.bids;

        j = 0;

        //skip the steps with more quantity than cap
        for(i =0; bids[i].q_min >= cap; i++);
        
        if(bids[i] == null)
            return new Demand(new Bidstep[0]);

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
    
  
}
