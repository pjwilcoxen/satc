/**
 * Building block of Demand class
 */
public class Bidstep {
    public int p;
    public int q_min;
    public int q_max;

    public Bidstep(int p, int q_min, int q_max) {
        this.p     = p;
        this.q_min = q_min;
        this.q_max = q_max;
    }
}
