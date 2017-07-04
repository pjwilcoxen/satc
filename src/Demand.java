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

}
