/**
 * Building block of bid class
 */
public class Bidstep {
    public int p;
    public int q_min;
    public int q_max;

    public int getP() {
        return p;
    }

    public void setQ_min(int q_min) {
        this.q_min = q_min;
    }

    public int getQ_min() {
        return q_min;
    }

    public int getQ_max() {
        return q_max;
    }

    public Bidstep(int p, int q_min, int q_max) {
        this.p     = p;
        this.q_min = q_min;
        this.q_max = q_max;
    }
    
}
