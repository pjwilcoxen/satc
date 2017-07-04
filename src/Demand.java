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

        for(i=0 ; (bids[i] != null)  && (bids[i].p < price) ; i++);
          
        assert bids[i] != null ;
                
        if(bids[i].p == price)     //where the price is the same as step price
            return bids[i].q_min;
        else                       //where the price is between two steps
            return bids[i].q_max;
    }

    /**
     * Find the actual price for the end users considering transaction cost and capacity limit
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
                        && (bids[i].getP() <= pr)
                        && (bids[i].getQ_max() >= ((-1) * cap))); i++)
                            p = bids[i].getP();

            if (bids[i] == null) {
                return p;
            }
            
            //if the target step passed the cap line
            if (bids[i].getQ_max() < ((-1) * cap)) {
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
                    && (bids[i].getQ_min() >= cap)); i++)
                         p = bids[i].getP();

            if (bids[i] == null) {
                return p;
            } 
            
            if (pr > bids[i].getP() - cost) 
                return pr + cost;

            //put limit equal to cap
            return bids[i].getP();
        }

        assert false;
        return report;
    }
}
