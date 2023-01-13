import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import static java.lang.Math.pow;
import org.apache.commons.csv.*;

/**
 * Agent representing an end user or supplier under Monte Carlo
 */
public class TraderMonte extends Trader {

    // initial load and elasticity and type from monte carlo file

    double load;
    double elast;
    String sd_type;

    static class Draw {
        int n;
        double load;
        double elast;
    }

    static ArrayList<Draw> drawListD = new ArrayList<>();
    static ArrayList<Draw> drawListS = new ArrayList<>();

    /**
     * TraderMonte agent
     *
     * @param up_id   ID of parent node
     * @param own_id  Own ID
     * @param sd_type Supply or demand type
     */
    public TraderMonte(int up_id, int own_id, String sd_type) {
        super(up_id, own_id);
        this.sd_type = sd_type;
    }

    /**
     * Initialize for a new population
     */
    @Override
    public void popInit() {
        super.popInit();
        load      = 0;
        elast     = 0;
    }

    /**
     * Build the agent's net demand curve
     */
    @Override
    protected Demand drawLoad() {
        Demand newD;
        Draw draw;
        int max = 9858;
        int rand;

        // generate a random number of input lines to skip
        // max was originally hard-coded and is left that
        // way for compatibility

        rand = (int)(rDraw * max);

        if( sd_type.equals("D") )
            draw = drawListD.get(rand);
        else
            draw = drawListS.get(rand);

        // parameters to use in constructing this curve

        load  = draw.load;
        elast = draw.elast;
        steps = (int) (rStep * MAXSTEP + 2);

        //call draw function based on the type of end user
        newD = do_make();

        return newD;
    }


    /**
     * Build a demand or supply curve
     */
    private Demand do_make() {
        Demand newD;
        boolean makeS;
        int sign;

        newD = new Demand();

        // get information about the trader; make local copies
        // for slightly greater clarity in calculations

        double elast  = this.elast;
        double load   = this.load;
        int    steps  = this.steps;
        double rPrice = this.rPrice;

        makeS = sd_type.equals("S");
        sign  = makeS ? -1 : 1 ;

        int iniprice = 40 + (int) (rPrice * 12 - 6);

        int p0 = iniprice/steps;
        int p1 = iniprice*2/steps;

        int q1=(int)(sign*load*pow((double)p0/iniprice,elast));
        int q2=(int)(sign*load*pow((double)p1/iniprice,elast));

        // first step
        newD.add(p0, q2, q1);

        // create the steps below the price=40

        for(int i=1 ; i<steps ; i++){
            p1 = iniprice*(i+1)/steps;
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            newD.add(p1, q2, q1);
        }

        // create twice the number of steps above price=40

        for(int i=1 ; i<2*steps ; i++){
            p1 = iniprice + 360*i/(2*steps);
            q1 = q2;
            q2 = (int)(sign*load*pow((double)p1/iniprice,elast));
            newD.add(p1, q2, q1);
        }

        return newD;
    }


    /**
     * Read the list of draws of random agent characteristics
     */
    public static void readDraws() throws FileNotFoundException, IOException {
        BufferedReader br;
        CSVParser csvReader;
        Draw draw;
        String sd_type;

        br = new BufferedReader(Util.openRead(Env.fileDraws));
        csvReader = CSVFormat.DEFAULT.withHeader().withIgnoreHeaderCase().parse(br);

        for(CSVRecord rec: csvReader) {
            draw = new Draw();
            sd_type    = rec.get("type");
            draw.load  = Double.parseDouble(rec.get("load"));
            draw.elast = Double.parseDouble(rec.get("elast"));
            if( sd_type.equals("D") )
                drawListD.add(draw);
            else
                drawListS.add(draw);
        }

        br.close();
    }


}
