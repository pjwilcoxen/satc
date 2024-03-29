import sim.engine.SimState;

/**
 * Class that extends Adversary.  Implements an malicious
 * actor that sends false bids indiscriminately.  Does
 * not hide their own identity
 */
public class AdvDarth extends Adversary{

    private int shift = 0;
    private boolean do_attack;
    private Channel diversionChannel;

    /**
     * Constructor
     *
     * @param own_id Agent's id
     */
    public AdvDarth(int own_id) {
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

        int traderId;
        Intel traderIntel;

        int targetId;
        Intel targetIntel;
        History targetHistory;
        String targetConstr;

        traderId = Integer.parseInt(config.get("trader"));
        traderIntel = getIntel(traderId);

        targetId = Integer.parseInt(config.get("target"));
        targetIntel = getIntel(targetId);
        targetHistory = targetIntel.history;
        targetConstr = targetHistory.getConstr(period);

        do_attack = (targetConstr.equals("D") || targetConstr.equals("S")) && traderIntel.compromised;
        if (do_attack) {
            diversionChannel = Env.getAgent(traderId).channel;
            diversionChannel.divert_from(traderId, this.own_id);

            shift = Integer.parseInt(config.get("shift"));
            if (targetConstr.equals("S"))
                shift = -shift;
        }
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
            Demand fakeDemand = new Demand();

            if (do_attack) {
                System.out.print("Attack Triggered!\n");

                // Generate false bid
                int traderId = Integer.parseInt(config.get("trader"));
                Intel traderIntel = getIntel(traderId);
                Demand histDemand = traderIntel.history.upD.get(period);

                for (int bidPrice : histDemand.bids.keySet()) {
                    fakeDemand.add(bidPrice,
                                   histDemand.getBidMin(bidPrice) - shift,
                                   histDemand.getBidMax(bidPrice) - shift);
                }

                int targetId = Integer.parseInt(config.get("target"));
                Msg msg = new Msg(this, targetId);
                msg.setDemand(fakeDemand);
                msg.setFrom(traderId);

                assert diversionChannel != null ;

                diversionChannel.inject(msg);

            } else {
                System.out.print("No Attack Triggered!\n");
            }

            break;

        default:
            break;
        }
    }
}
