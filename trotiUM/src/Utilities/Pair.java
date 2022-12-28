package Utilities;

import java.io.Serializable;

/** Pair.java
 * This class is used to create a Pair of two objects..
 * 
 * @author: Miguel Gomes
 * @version: 1.0
 */

public class Pair<L,R> implements Serializable{
    private static final long serialVersionUID = 1L;
    private L l;
    private R r;
    public Pair(L l, R r){
        this.l = l;
        this.r = r;
    }
    public Pair(){
	    this.l = null;
	    this.r = null;
    }

    public L getL(){ return l; }

    public R getR(){ return r; }
    
    public void setL(L l){ this.l = l; }
    
    public void setR(R r){ this.r = r; }
    
    public String toString () {
        return ("("+l.toString()+", "+r.toString()+")");
    }
}
