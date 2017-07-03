public class Msg {

    public static enum Types {
        NONE,
        DEMAND,
        PRICE
    };

    Agent agent;
    int from;
    int to;
    Types type;
    Bidstep bids[];
    int price;

    public int dos_id;

    public Msg(Agent agent,int to) {
        this.agent = agent;
        this.from  = agent.own_id;
        this.to    = to;
        this.type  = Types.NONE;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setDemand(Bidstep bids[]) {
        assert type == Types.NONE;
        assert bids != null;
        this.bids = bids;
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

    public Bidstep[] getDemand() {
        assert type == Types.DEMAND;
        return bids;
    }

    public int getPrice() {
        assert type == Types.PRICE;
        return price;
    }
}
