import java.util.ArrayList;

/**
 * Message class
 */
public class Msg {

    /**
     * Header for log files
     */
    public static final String LOGHEADER = "pop,dos,sender,from,to,type";

    /**
     * Known types of messages
     */
    public static enum Types {
        /**
         * Unknown message type
         */
        NONE,
        /**
         * Message contains a demand curve
         */
        DEMAND,
        /**
         * Message contains a price
         */
        PRICE
    };

    Agent sender;
    int from;
    int to;
    Types type;
    Demand demand;
    int price;

    /**
     * Message
     * 
     * @param sender Agent sending the message
     * @param to ID of destination agent
     */
    public Msg(Agent sender,int to) {
        this.sender = sender;
        this.from       = sender.own_id;
        this.to         = to;
        this.type       = Types.NONE;
    }

    /**
     * Produce a CSV-style log string for the message
     * 
     * @return Log string
     */
    public String logString() {
        String msg;
        msg = ""+Env.pop
            + ","+Env.curDOS
            + ","+sender.own_id
            + ","+from
            + ","+ to
            + ","+type;
        return msg;
    }

    /**
     * Override the from field
     * 
     * @param from ID of apparent sender
     */
    public void setFrom(int from) {
        this.from = from;
    }

    /**
     * Make this a demand message
     * 
     * @param demand Demand curve to send
     */
    public void setDemand(Demand demand) {
        assert type == Types.NONE;
        assert demand != null;
        this.demand = demand;
        type = Types.DEMAND;
    }

    /**
     * Make this a price message
     * 
     * @param price Price to send
     */
    public void setPrice(int price) {
        assert type == Types.NONE;
        this.price = price;
        type = Types.PRICE;
    }

    /**
     * Is this a demand message?
     * 
     * @return True if a demand message
     */
    public boolean isDemand() {
        return type == Types.DEMAND;
    }

    /**
     * Is this a price message?
     * 
     * @return True if a price message
     */
    public boolean isPrice() {
        return type == Types.PRICE;
    }

    /**
     * Get the demand curve from the message
     * 
     * @return Demand
     */
    public Demand getDemand() {
        assert type == Types.DEMAND;
        return demand;
    }

    /** 
     * Get the price from the message
     * 
     * @return Price
     */
    public int getPrice() {
        assert type == Types.PRICE;
        return price;
    }
}