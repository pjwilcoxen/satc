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

    public Msg(Agent agent,int from,int to) {
        this.agent = agent;
        this.from  = from;
        this.to    = to;
        this.type  = Types.NONE;
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
