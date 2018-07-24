import java.util.ArrayList;
import sim.engine.SimState;

/**
 * Adversary Elvira.
 * 
 * Attacks markets with constrained transmission to their upstream
 * nodes. Forges demand messages but modifying historical demands
 * sent by one or more compromised nodes. 
 * 
 * Markets that were demand constrained get tweaks lowering net demand; 
 * markets constrained on the supply side get tweaks raising net demand. 
 * 
 * Multiple markets may be attacked by listing them in the configuration
 * file with plus signs between them: 201+202. The total change is divided 
 * up over all compromised agents in all markets being attacked. 
 */
public class Adv_Elvira extends Adversary {

    private int eachShift;
    private boolean do_attack;

    /**
     * Private class for information used in forging messages
     */
    static class ForgeInfo {
       int from_id;
       int to_id;
       int tier;
       boolean sup_con;
       Channel channel;

       ForgeInfo(int from, int to) {
          this.from_id = from;
          this.to_id = to;
          this.sup_con = false;
          this.channel = null;
       }
    }

    /**
     * List of ForgeInfo objects, one for each victim
     */
    private final ArrayList<ForgeInfo> forgeList = new ArrayList<>();
          
    /**
     * Constructor
     * 
     * @param own_id Agent's id
     */
    public Adv_Elvira(int own_id) {
        super(own_id);
    }

    /**
     * Initialize for a new population
     */
    @Override
    public void popInit() {
        super.popInit();
    }

    /**
     * Reset at the beginning of a DOS run
     */
    @Override
    public void runInit() {
        super.runInit();

        String[] targets ;
        int targetId;
        int shift;
        Intel targetIntel;
        History targetHistory;
        String targetConstr;
        ForgeInfo fi;

        // 
        //  Figure out how large the total attack should be and which 
        //  markets we'd like to attack. Throw an exception if there
        //  aren't any target markets.
        //

        shift   = Integer.parseInt(getConfig("shift"));
        targets = getConfig("target").split("\\+");
       
        assert targets.length > 0 ;

        //
        //  Now look through all of the target markets and see which 
        //  child nodes are compromised. Result will be a list indicating
        //  which demand messages will be forged.
        //

        forgeList.clear() ;

        for( String targetStr: targets ) {
                    
            // 
            //  Look in history to see if flows into or out of the market 
            //  were constrained. Only attack if they were.
            //

            targetId      = Integer.parseInt(targetStr);
            targetIntel   = getIntel(targetId);
            targetHistory = targetIntel.history;
            targetConstr  = targetHistory.getConstr(period);

            do_attack = targetConstr.equals("D") | targetConstr.equals("S") ;
            
            if( ! do_attack )
               continue;

            //
            //  Ok, we're attacking. Build a list of compromised traders
            //  that have the target market as their parent. While we're at 
            //  it, divert messages from the victims. Also, set the trigger
            //  tier to be one level up from the victim, since the forgery
            //  will happen at the market tier.
            //
        
            for (Intel i : intel.values()) {
                if (i.compromised && (i.par_id == targetId)) {
                    fi = new ForgeInfo(i.agent_id,i.par_id);
                    fi.sup_con = targetConstr.equals("S");
                    fi.tier    = i.tier + 1 ;
                    fi.channel = Env.getAgent(i.agent_id).channel;
                    fi.channel.divert_from(i.agent_id,this.own_id);
                    forgeList.add(fi);
                }
            }
        }
        
        //
        //  Now divide the shock into parts based on the number of agents
        //  whose demands will be forged. 
        //

        if( forgeList.size() > 0 )
            eachShift = Math.round( (float) shift / forgeList.size());
    }

    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {
        int thisShift;
        
        switch (Env.stageNow) {

        case PRE_AGGREGATE:
            
            if( ! do_attack ) {
                Util.debug("No attack: no constrained markets");
                return;
            }

            if( forgeList.isEmpty() ) {
                Util.debug("No attack: no vulnerable agents found");
                return;
            }
            
            //  
            // OK, looks like we should try to corrupt things with some
            // forged messages. Go through the list of victims and do
            // the dirty work. 
            //
            
            for( ForgeInfo fi: forgeList ) {
                Integer traderId;
                
                //
                // Avoid sending spurious messages by being careful to 
                // inject messages at the tier after the originals were sent. 
                //
                
                if( fi.tier != Env.curTier )
                    continue;
                
                //
                // Good to go: build and inject the message. Start by 
                // recovering the previous net demand and figuring out
                // which way to shift it.
                //
                
                traderId          = fi.from_id ;
                Intel traderIntel = getIntel(traderId);
                Demand histDemand = traderIntel.history.upD.get(period);
                
                thisShift = eachShift;
                if( fi.sup_con )thisShift = -eachShift;
                
                //
                // Build the new demand
                //
                
                Demand fakeDemand = new Demand();
                for (int bidPrice : histDemand.bids.keySet()) {
                    fakeDemand.add(bidPrice,
                        histDemand.getBidMin(bidPrice) - thisShift,
                        histDemand.getBidMax(bidPrice) - thisShift);
                }

                //
                // Add it to the demand log file
                //
                
                fakeDemand.log(Env.getAgent(traderId),Demand.Type.FAKE);

                //
                // Build and send the message; note that we're lying about
                // the from address.
                //
                
                Msg msg = new Msg(this, fi.to_id);
                msg.setDemand(fakeDemand);
                msg.setFrom(fi.from_id);

                Util.debug("message injected to "+fi.to_id+
                           " from "+this.own_id+
                           " as "+fi.from_id+", shift "+thisShift);

                fi.channel.inject(msg);
            }
            
            break;
        }
    }
}
