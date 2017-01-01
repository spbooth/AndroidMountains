package uk.ac.ed.epcc.mountains;

import java.io.Serializable;

import android.util.Log;


/* {{{ public class Mountain*/

public final class Mountain implements Serializable{
  public static final int DEFAULT_BACK = 0;
public static final int DEFAULT_FRONT = 1;
public static final double DEFAULT_FDIM = 0.65;
public static final int DEFAULT_STOP = 1;
public static final int DEFAULT_LEVELS = 9;
static final  double mean=0.0;  // Mean height
  boolean rg1=false;
  boolean rg2=false;
  boolean rg3=true;
  boolean cross=true;
  int force_front=DEFAULT_FRONT;
  int force_back=DEFAULT_BACK;
  double forceval=-0.5;
  double mix=0.0;
  double midmix=0.0;
  double fdim=DEFAULT_FDIM;
  int levels=DEFAULT_LEVELS;
  int stop=DEFAULT_STOP;
  private int width;        /* width of surface in points */
  private double fwidth;    /* width of surface 
                            * in same units as height 
                            */
  public static final double mwidth=1.0;    /* longest fractal lengthscale 
                                      * in same units as height 
                                      */
  public double mheight; /* mean height of fractal */
//  public Random r=new Random();
  public Uni r=new Uni();

  private Fold f=null;
  private boolean initialised=false;
  private static final boolean debug=false;

  public synchronized void set_seed(int i){
    r =new Uni(i);
  }

  public void debugMsg(String msg){
	  if(debug){
		  Log.d(getClass().getName(),msg);
	  }
  }
/* {{{ set_size()*/

  public synchronized boolean set_size(int l, int s){
    if( s < 0 || l < s ) return false;
    if( levels == l && stop == s ){
    	return false;
    }
    if(initialised){
      clear();
    }
    levels=l;
    stop=s;
    return true;
  }

/* }}} */
/* {{{ set_rg*/

  public synchronized boolean set_rg(boolean r1, boolean r2, boolean r3){
	  if( rg1 == r1 && rg2 == r2 && rg3 == r3){
		  return false;
	  }
    // Changing this would mess up the pipeline so re-initialise
    if(initialised){
      clear();
    }
    rg1=r1;
    rg2=r2;
    rg3=r3;
    return true;
  }

/* }}} */
/* {{{ set_cross() */

  public synchronized void set_cross(boolean c){
	  if( cross == c ){
		  return;
	  }
    // We can change this during the update
    cross = c;
  }

/* }}} */
/* {{{ set_fdim */

  public synchronized void set_fdim(double fd){
    // We can change this during the update
    if( fd < 0.5 || fd > 1.0 || fdim == fd) return;
    fdim = fd;
    if( initialised ){
      f.sync(fd);
    }
  }

/* }}} */
/* {{{ set_front */

  public synchronized void set_front(int level){
    force_front=level;
  }

/* }}} */
/* {{{ set_back */

  public synchronized void set_back(int level){
    force_back=level;
  }

/* }}} */

/* {{{ get_width() */

  public int get_width(){
    if( ! initialised ) init();

    return width;
  }

/* }}} */
/* {{{ get_fwidth() */

  public double get_fwidth(){
    if( ! initialised ) init();

    return fwidth;
  }

/* }}} */
/* {{{ init() */

  public synchronized void init(){
    int pwid;
    double len;

    if( initialised ){
      debugMsg("Mountain.init called multiple times");
      return;
    }else{
      debugMsg("Mountain.init called once");
    }
    /* the fractal width should be 1.0 */
    pwid = 1 + (1 << (levels-stop));
    width = 1 + (1 << levels);
    fwidth = mwidth * (double) width/(double) pwid;
    mheight = Math.pow(mwidth, 2.0 * fdim);
    len = mwidth/(double) pwid;
    f=new Fold(this,levels,stop,len);
    f.sync(fdim);
    initialised=true;
  }

/* }}} */
/* {{{ clear() */

  public synchronized void clear(){
    if( ! initialised ) return;

    f.clear();
    f = null;
    initialised = false;
  }

/* }}} */
/* {{{ next_strip() */

  public double[] next_strip(){
    double[] s;
    if( ! initialised ){
      init();
    }
    debugMsg("Mountain.next_strip called");
    s = f.next_strip();
    return s;
  }

public double getForceval() {
	return forceval;
}

public void setForceval(double forceval) {
	this.forceval = forceval;
}

/* }}} */
}

/* }}} */

/* {{{ class Fold*/

final class Fold implements Serializable{
 static final int START=0;
 static final int STORE=1;
 static final int NSTRIP=8;
 Mountain p;
 int count;
 int stop;
 double length;
 double scale;
 double midscale;
 double s[][] = new double[NSTRIP][];
 double[] save=null;
 int state=START;
 int level;
 Fold next;
 private final boolean debug=false;

/* {{{  Fold(Mountain param,Fold up,int levels,int stop, double length)*/

 Fold(Mountain param,int levels,int stop, double length){
   int i;
   p=param;
   if( (levels < stop) || (stop<0))
     {
       // error
       System.exit(1);
     }
   this.length=length;
   this.level=levels;
   this.count= (1<<levels)+1;
   this.stop = stop;
   for(i=0;i<NSTRIP;i++){
     this.s[i]=null;
   }
   if( levels > stop ){
     this.next = new Fold(param,(levels-1),stop,(2.0*length));
   }else{
     this.next = null;
   }

  }

/* }}} */
/* {{{  void clear() */

  void clear(){
    int i;

    /* null all pointers to speed memory reclaim */
    if( next != null ){
      next.clear();
      next=null;
    }
    for(i=0;i<NSTRIP;i++){
      s[i] = null;
    }
    save=null;
  }

/* }}} */
/* {{{  void sync()*/
  static final double root2 = java.lang.Math.sqrt( 2.0 );

  synchronized void sync(double fdim){
  
    scale = java.lang.Math.pow(length, (2.0 * fdim));
    midscale = java.lang.Math.pow((length*root2), (2.0 * fdim));
    if( next != null ){
      next.sync(fdim);
    }
  }
/* }}} */
/* {{{  double[] next_strip()*/

  synchronized double[] next_strip(){
    double[] result=null;
    int t,i, iter;

    if( level == stop ){
      result= this.random_strip();
    }else{
  /*
   * There are two types of strip,
   *  A strips - generated by the lower recursion layers.
   *             these contain the corner points and half the side points
   *  B strips - added by this layer, this contains the mid points and
   *             half the side points.
   *
   * The various update routines test for null pointer arguments so
   * that this routine will not fail while filling the pipeline.
   */
    while( result == null )
    {
      /* {{{ iterate*/

      switch(state)
      {
        case START:
          /* {{{   perform an update. return first result*/

          t=0;
          /* read in a new A strip at the start of the pipeline */
          s[t+0] = double_strip(next.next_strip());
          /* make the new B strip */
          s[t+1]= Strip(0.0);
          if( s[t+2] == null )
          {
            /* we want to have an A B A pattern of strips at the
             * start of the pipeline.
             * force this when starting the pipe
             */
            s[t+2]=s[t+0];
            s[t+0] = double_strip(next.next_strip());
          }
          /*
           * create the mid point
           * t := A B A
           */
          x_update(midscale,0.0,s[t+0],s[t+1],s[t+2]);
          
          if(p.rg1)
          {
            /*
             * first possible regeneration step
             * use the midpoints to regenerate the corner values
             * increment t by 2 so we still have and A B A pattern
             */
            if( s[t+3] == null )
            {
              /* rather than do no update add offset to old value */
              v_update(midscale,1.0,s[t+1],s[t+2],s[t+1]);
            }else{
              v_update(midscale,p.midmix,s[t+1],s[t+2],s[t+3]);
            }
            t+=2;
          }
          
          /*
           * fill in the edge points
           * increment t by 2 to preserve the A B A pattern
           */
          if( p.cross )
          {
            t_update(scale,0.0,s[t+0],s[t+1],s[t+2]);
            p_update(scale,0.0,s[t+1],s[t+2],s[t+3]);
            t+=2;
          }else{
            hside_update(scale,0.0,s[t+0],s[t+1],s[t+2]);
            vside_update(scale,0.0,s[t+2]);
            t+=2;
          }
          
          if(p.rg2)
          {
            /*
             * second regeneration step update midpoint
             * from the new edge values
             */
            if( p.cross )
            {
              if( s[t+2] == null )
              {
                /* add random offset to old rather than skip update */
                p_update(scale,p.mix,s[t+0],s[t+1],s[t+0]);
              }else{
                p_update(scale,p.mix,s[t+0],s[t+1],s[t+2]);
              }
            }else{
              vside_update(scale,p.mix,s[t+1]);
            }
          
          }
          /* increment t by 1
           * this gives a B A B pattern to regen-3
           * if regen 3 is not being used it leaves t pointing to the
           * 2 new result strips
           */
          t++;
          if(p.rg3)
          {
            /* final regenration step
             * regenerate the corner points from the new edge values
             * this needs a B A B pattern
             * leave t pointing to the 2 new result strips
             *
             * this has to be a t_update
             */
            if( s[t+2] == null )
            {
              /* add random offset to old rather than skip update */
              t_update(scale,1.0,s[t+0],s[t+1],s[t+0]);
            }else{
              t_update(scale,p.mix,s[t+0],s[t+1],s[t+2]);
            }
            t++;
          
          }
          result=s[t+1];
          save=s[t+0];
          s[t+0]=s[t+1]=null;
          state = STORE;
          break;

          /* }}} */
        case STORE:
          /* {{{   return second value from previous update. */
          result = save;
          save=null;
          for(i=NSTRIP-1;i>1;i--)
          {
            s[i] =s[i-2];
          }
          s[0] = s[1]=null;
          state = START;
          break;
          /* }}} */
        default:
	  // Error
      }

      /* }}} */
    }
  }
  iter = level - stop;
  if( p.force_front > iter){
   result[0] = p.forceval * p.mheight;
  }
  if( p.force_back > iter){
    result[count-1] = p.forceval * p.mheight;
  }
  //for(i=0;i<result.length;i++){
  //  System.out.println(result[i]);
  //}
  return(result);
  }

/* }}} */
/* {{{  void x_update(double scale, double mix, double[] a, double[] b, double[] c)*/

  void x_update(double scale, double mix, double[] a, double[] b, double[] c){
    int i;
    double w;
    double mp[], lp[], rp[];

    /* don't run unless we have all the parameters */
    if( a==null || c==null ) return;
  
    w = (1.0 - mix)*0.25;
    mp=b;
    lp=a;
    rp=c;
    
    if( mix <= 0.0 ){
      /* {{{ random offset to average of new points*/
      for(i=0; i<count-2; i+=2)
	{
	  mp[i+1] = 0.25 * ( lp[i] + rp[i] + lp[i+2] + rp[i+2])
            + (scale * p.r.nextGaussian());
	}
      /* }}} */
    }else if( mix >= 1.0 ){
      /* {{{ random offset to old value*/
      for(i=0; i<count-2; i+=2)
	{
	  mp[i+1] = mp[i+1]
            + (scale * p.r.nextGaussian());
	}
      /* }}} */
    }else{
      /* {{{ mixed update*/
      for(i=0; i<count-2; i+=2)
	{
	  mp[i+1] = (mix * mp[i+1]) + w * ( lp[i] + rp[i] + lp[i+2] + rp[i+2])
            + (scale * p.r.nextGaussian());
	}
      /* }}} */
    }
  }

/* }}} */
/* {{{  void p_update(double scale, double mix, double[] a, double[] b, double[] c)*/

  void p_update(double scale, double mix, double[] a, double[] b, double[] c){
  int i;
  double w;
  double mp[], lp[], rp[];

  /* don't run if we have no parameters */
  if( a==null || b==null ) return;

  /* if c is missing we can do a vside update instead
   * should really be a sideways t but what the heck we only
   * need this at the start
   */
  if( c==null )
  {
    vside_update(scale,mix,b);
    return;
  }

  w = (1.0 - mix)*0.25;
  mp=b;
  lp=a;
  rp=c;

  if( mix <= 0.0 ){
    /* {{{ random offset to average of new points*/
    for(i=0; i<count-2; i+=2)
    {
      mp[i+1] = 0.25 * ( lp[i+1] + rp[i+1] + mp[i] + mp[i+2] )
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }else if(mix >= 1.0){
    /* {{{ random offset to old values*/
    for(i=0; i<count-2; i+=2)
    {
      mp[i+1] = mp[i+1]
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }else{
    /* {{{ mixed update*/
    for(i=0; i<count-2; i+=2)
    {
      mp[i+1] = (mix * mp[i+1]) + w * ( lp[i+1] + rp[i+1] + mp[i] + mp[i+2] )
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }

  }

/* }}} */
/* {{{  void t_update(double scale, double mix, double[] a, double[] b, double[] c)*/

  void t_update(double scale, double mix, double[] a, double[] b, double[] c){
  int i;
  double w, we;
  double mp[], lp[], rp[];
  final double THIRD=(1.0/3.0);

  /* don't run unless we have all the parameters */
  if( a==null || c==null ) return;

  w = (1.0 - mix)*0.25;
  we = (1.0 - mix)*THIRD;
  mp=b;
  lp=a;
  rp=c;

  if( mix <= 0.0){
    /* {{{ random offset to average of new points*/

    mp[0] = THIRD * ( lp[0] + rp[0] + mp[1] )
            + (scale * p.r.nextGaussian());
    for(i=1; i<count-3; i+=2)
    {
      mp[i+1] = 0.25 * ( lp[i+1] + rp[i+1] + mp[i] + mp[i+2] )
            + (scale * p.r.nextGaussian());
    }
    mp[i+1] = THIRD * ( lp[i+1] + rp[i+1] + mp[i] )
          + (scale * p.r.nextGaussian());

    /* }}} */
  }else if(mix >= 1.0){
    /* {{{ random offset to old values*/
    for(i=0; i<count; i+=2)
    {
      mp[i] = mp[i]
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }else{
    /* {{{ mixed update*/
    mp[0] = (mix * mp[0]) + we * ( lp[0] + rp[0] + mp[1] )
            + (scale * p.r.nextGaussian());
    for(i=1; i<count-3; i+=2)
    {
      mp[i+1] = (mix * mp[i+1]) + w * ( lp[i+1] + rp[i+1] + mp[i] + mp[i+2] )
            + (scale * p.r.nextGaussian());
    }
    mp[i+1] = (mix * mp[i+1]) + we * ( lp[i+1] + rp[i+1] + mp[i] )
          + (scale * p.r.nextGaussian());
    /* }}} */
  }
  }

/* }}} */
/* {{{  void v_update(double scale, double mix, double[] a, double[] b, double[] c)*/

  void v_update(double scale, double mix, double[] a, double[] b, double[] c){
  int i;
  double w, we;
  double mp[], lp[], rp[];

  /* don't run unless we have all the parameters */
  if( a==null || c==null ) return;

  w = (1.0 - mix)*0.25;
  we = (1.0 - mix)*0.5;
  mp=b;
  lp=a;
  rp=c;

  if( mix <= 0.0){
    /* {{{ random offset of average of new points*/
    mp[0] = 0.5 * ( lp[1] + rp[1] )
            + (scale * p.r.nextGaussian());
    for(i=1; i<count-3; i+=2)
    {
      mp[i+1] = 0.25 * ( lp[i] + rp[i] + lp[i+2] + rp[i+2] )
            + (scale * p.r.nextGaussian());
    }
    mp[i+1] = 0.5 * ( lp[i] + rp[i] )
            + (scale * p.r.nextGaussian());
    /* }}} */
  }else if(mix >= 1.0){
    /* {{{ random offset to old values*/
    for(i=0; i<count; i+=2)
    {
      mp[i] = mp[i]
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }else{
    /* {{{ mixed update*/
    mp[0] = (mix * mp[0]) + we * ( lp[1] + rp[1] )
            + (scale * p.r.nextGaussian());
    for(i=1; i<count-3; i+=2)
    {
      mp[i+1] = (mix * mp[i+1]) + w * ( lp[i] + rp[i] + lp[i+2] + rp[i+2] )
            + (scale * p.r.nextGaussian());
    }
    mp[i+1] = (mix * mp[i+1]) + we * ( lp[i] + rp[i] )
            + (scale * p.r.nextGaussian());
    /* }}} */
  }
  }

/* }}} */
/* {{{  void hside_update(double scale, double mix, double[] a, double[] b, double[] c)*/

  void hside_update(double scale, double mix, double[] a, double[] b, double[] c){
  int i;
  double w;
  double mp[], lp[], rp[];

  /* don't run unless we have all the parameters */
  if( a==null || c==null ) return;

  w = (1.0 - mix)*0.5;
  mp=b;
  lp=a;
  rp=c;

  if( mix <= 0.0 ){
    /* {{{ random offset to average of new points*/
    for(i=0; i<count; i+=2)
    {
      mp[i] = 0.5 * ( lp[i] + rp[i] )
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }else if(mix >= 1.0){
    /* {{{ random offset to old points*/
    for(i=0; i<count; i+=2)
    {
      mp[i] = mp[i]
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }else{
    /* {{{ mixed update*/
    for(i=0; i<count; i+=2)
    {
      mp[i] = (mix * mp[i]) + w * ( lp[i] + rp[i] )
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }
  }

/* }}} */
/* {{{  void vside_update(double scale, double mix, double[] a)*/

  void vside_update(double scale, double mix, double[] a){
  int i;
  double w;
  double mp[];

  /* don't run unless we have all the parameters */
  if( a==null ) return;


  w = (1.0 - mix)*0.5;
  mp=a;

  if( mix <= 0.0){
    /* {{{ random offset to average of new points*/
    for(i=0; i<count-2; i+=2)
    {
      mp[i+1] = 0.5 * ( mp[i] + mp[i+2] )
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }else if(mix >= 1.0){
    /* {{{ random offset to old values*/
    for(i=0; i<count-2; i+=2)
    {
      mp[i+1] = mp[i+1]
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }else{
    /* {{{ mixed update*/
    for(i=0; i<count-2; i+=2)
    {
      mp[i+1] = (mix * mp[i+1]) + w * ( mp[i] + mp[i+2] )
            + (scale * p.r.nextGaussian());
    }
    /* }}} */
  }
  }

/* }}} */
/* {{{  double[] random_strip()*/

  double[] random_strip(){
    double[] result= new double[count];
    int i;
    for(i=0;i<count;i++){
      result[i] = p.mean + (scale * p.r.nextGaussian());
    }
    return result;
  }
 
/* }}} */
/* {{{ Strip( Value ) */

  double[] Strip(double value){
    double[] res= new double[count];
    int i;
    for(i=0;i<count;i++){
      res[i] = value;
    }
    return res;
  }

/* }}} */
/* {{{ Strip() */

  double[] Strip(){
    double[] res= new double[count];
    return res;
  }

/* }}} */
/* {{{ double_strip */

  double[] double_strip(double[] orig){
    int l,i,j;
    double[] result;

    l=orig.length*2 - 1;
    result = new double[l];
    result[0] = orig[0];
    j=1;
    for(i=2;i<l;i=i+2){
      result[i-1] = 0.0;
      result[i] = orig[j];
      j++;
    }
    return result;
  }

/* }}} */
}

/* }}} */

