public class Msg {

    public static String logHeader = "pop,dos,sender,from,to,type";

    public static enum Types {
        NONE,
        DEMAND,
        PRICE
    };

    Agent sender;
    int from;
    int to;
    Types type;
    Demand demand;
    int price;

    public int dos_id;

    public Msg(Agent sender,int to) {
        this.sender = sender;
        this.from   = sender.own_id;
        this.to     = to;
        this.type   = Types.NONE;
    }

    public String logString() {
        String msg;
        msg = ""+Env.pop
            + ","+Env.dos_runs[dos_id]
            + ","+sender.own_id
            + ","+from
            + ","+ to
            + ","+type;
        return msg;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setDemand(Demand demand) {
        assert type == Types.NONE;
        assert demand != null;
        this.demand = demand;
        type = Types.DEMAND;
    }

    public void setPrice(int price) {
        assert type == Types.NONE;
        this.price = price;
        type = Types.PRICE;
    }

    public boolean isDemand() {
        return type == Types.DEMAND;
    }

    public boolean isPrice() {
        return type == Types.PRICE;
    }

    public Demand getDemand() {
        assert type == Types.DEMAND;
        return demand;
    }

    public int getPrice() {
        assert type == Types.PRICE;
        return price;
    }
}
