public class Demand {
   
   static final int MAXBIDS = 400;
   public Bidstep[] bids;

   public Demand() {
       bids = new Bidstep[MAXBIDS];
   }

   public Demand(Bidstep[] bids) {
       this.bids = bids;
   }
}
