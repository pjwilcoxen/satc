/**
 * Building block of bid class
 */
public class Bid {
    public int ptice;
    public int q_min;
    public int q_max;

    public int getPtice() {
        return ptice;
    }

    public void setPtice(int ptice) {
        this.ptice = ptice;
    }

    public void setQ_min(int q_min) {
        this.q_min = q_min;
    }

    public void setQ_max(int q_max) {
        this.q_max = q_max;
    }

    public int getQ_min() {
        return q_min;
    }

    public int getQ_max() {
        return q_max;
    }

    public Bid(int ptice, int q_min, int q_max) {
        this.ptice = ptice;
        this.q_min = q_min;
        this.q_max = q_max;
    }
    
}
