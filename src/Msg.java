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
    
    public static enum Security {
        /**
         * Message includes a hash
         */
        HASHED,
        /**
         * Message has been encrypted
         */
        ENCRYPTED,
        /**
         * Message has been digitally signed by the sender
         */
        SIGNED,
        /**
         * A token has been issued along with the message
         */
        TOKEN,
        /**
         * No security measures are in place
         */
        NONE
    }

    Agent sender;
    int from;
    int to;
    Types type;
    Demand demand;
    int price;
    ArrayList<Security> security = new ArrayList<>();
    String privateKey;
    String publicKey;

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
        this.security.add(Security.NONE);
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
    
    /**
     * Returns the type of security in the message
     * 
     * @return security list
     */
    public ArrayList<Security> getSecurity() {
        return security;
    }
    
	/**
     * Returns true if message has given security measure
	 *
     * @return boolean
     */
    public boolean hasSecurity(Security s) {
        if (security.contains(s)) {
			return true;
		}
		else {
			return false;
		}
    }
	
    /**
     * Encrypts the message
     *
     * Uses private and public keys to encrypt the message
     */
    public void encrypt(String pub) {
        security.add(Security.ENCRYPTED);
        this.publicKey = pub;
    }
    
    /**
     * Signs the message
     *
     * Uses private key to sign the message
     */
    public void sign(String pri) {
        security.add(Security.SIGNED);
        this.privateKey = pri;
    }
    
    /**
     * Decrypts the message
     *
     * @return True if decryption was successful
     */
    
    public boolean decrypt(String pri) {
        
        if (security.contains(Security.ENCRYPTED) && Env.resolvePrivate(pri) == Env.resolvePublic(publicKey)) {
            security.remove(Security.ENCRYPTED);
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Verifies the signature of the message
     *
     * @return True if verification was successful
     */
    public boolean verify(String pub) {

        if (security.contains(Security.SIGNED) && Env.resolvePublic(pub) == Env.resolvePrivate(privateKey)) {
            security.remove(Security.SIGNED);
            return true;
        }
        else {
            return false;
        }
    }
}