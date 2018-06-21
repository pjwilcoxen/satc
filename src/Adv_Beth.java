import java.util.HashMap;
import java.util.Map;
import sim.engine.SimState;

/**
 * Class that extends Adversary.  Implements an malicious
 * actor that sends false bids indiscriminately.  Does
 * not hide their own identity
 */
public class Adv_Beth extends Adversary{

    Demand falseBid = new Demand();

    /**
     * Constructor
     */
    public Adv_Beth(int own_id) {
        super(own_id);
        falseBid.add(1,-1000,1000);
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
    }

    /**
     * Actions based on current simulation step
     *
     * @param state Mason state
     */
    @Override
    public void step(SimState state) {

        switch (Env.stageNow) {

        case PRE_AGGREGATE:

            /*// Repeat over intel
            for(Map.Entry<Integer,Intel> entry: intel.entrySet()) {
                Intel i = entry.getValue();

				            // If Adam can send to this agent, then send false bid
				            if(i.send){
                    Msg msg = new Msg(this, i.agent_id);
                    msg.setDemand(falseBid);
                    Channel channel = Channel.find(i.channel);
                    assert channel != null;
                    channel.send(msg);
                }
            }*/
            //Repeat over intel
            /*for(Map.Entry<Integer,Intel> entry: intel.entrySet()) {
                Intel i = entry.getValue();

                if(Env.curTier != i.tier)
                  continue;
                for(Map.Entry<Integer,Intel> entry2: intel.entrySet()) {
                  Intel j = entry2.getValue();
                  if(j.par_id == i.agent_id) {
                    if(i.send) {
                       Msg msg = new Msg(this, i.agent_id);
                       msg.setFrom(j.agent_id);
                       msg.setDemand(falseBid);
                       Channel channel = Channel.find(i.channel);
                       assert channel != null;
                       channel.send(msg);
                       break;
                  }
                }
              }
            }*/
            for(Map.Entry<Integer,Intel> child: intel.entrySet()) {
              Intel c = child.getValue();
              Intel target;
              if(intel.get(c.par_id) == null) {
                continue;
              }
              else {
                target = intel.get(c.par_id);
                if(Env.curTier != target.tier) {
                  continue;
                }
                if(target.send) {
                  Msg msg = new Msg(this, target.agent_id);
                  msg.setFrom(c.agent_id);
                  msg.setDemand(falseBid);
                  Channel channel = Channel.find(target.channel);
                  assert channel != null;
                  channel.send(msg);
                  break;
                }
              }
            }
            break;

        default:
            break;
        }
    }
}
