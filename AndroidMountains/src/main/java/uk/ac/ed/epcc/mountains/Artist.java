package uk.ac.ed.epcc.mountains;

import java.lang.Math;

public class Artist{
static final double SIDE=1.0;
static final int BLACK=0;
static final int WHITE=1;
static final int SEA_LIT=2;
static final int SEA_UNLIT=3;
static final int SKY=4;
static final int BAND_BASE=5;
static final int BAND_SIZE=80;
static final int N_BANDS=3;
static final int DEF_COL=(BAND_BASE + (N_BANDS * BAND_SIZE));
static final int MIN_COL=(BAND_BASE + (N_BANDS * 2));

private boolean initialised=false;
private int base=0;
private final Mountain m;
private final Screen s;
private double shadow[];
private double a_strip[], b_strip[];

 private final int graph_height;     /* height of display */
 private final int graph_width;     /* width of display */
 
  int n_col=DEF_COL;
  int band_size=BAND_SIZE;

private int width;            /* width of terrain strip */

private static float ambient=0.3F;        /* level of ambient light */
private static final double contrast=1.0;       /* contrast,
                              * increases or decreases effect of cosine rule */
private static final double contour=0.3;
private static final double vfract=0.6;         /* relative strength of vertical light relative
                              * to the main light source
                              */
private static final double altitude=2.5;
private static final double distance=4.0;
private static final double phi=(40.0 * Math.PI )/180.0;/* angle of the light (vertical plane)*/
private static final double alpha=0.0;         /* angle of the light (horizontal plane) 
                             * must have -pi/4 < alpha < pi/4
                             */
private static final double base_shift=0.5;    /* offset from calcalt to artist coordinates */
private static final double sealevel=0.0;
private static final double stretch=0.6;       /* vertical stretch */
private boolean draw_map=false;
private boolean reflec=true;
private int repeat=20;
private  int pos=0;
private int scroll=0;
  private double shift;
  private double varience;
  private double delta_shadow;
  private double shadow_slip;
  private double shadow_register;
  private double cos_phi;
  private double sin_phi;
  private double tan_phi;
  private double x_fact;
  private double y_fact;
  private double vangle;
  private double vscale;
  private double tan_vangle;                                     
  private double viewpos;        /* position of viewpoint */
  private  double viewheight;      /* height of viewpoint */
  private  double focal;
  private  double vstrength; /* strength of vertical light source */
  private  double lstrength; /* strength of vertical light source */


public  Artist(int width,int height, Screen ss, Mountain mm){
    m=mm;
    s=ss;
    graph_width=width;
    graph_height=height;

}
/* {{{ set_clut*/

  private void set_clut(){
    int band, shade;
    float red,green,blue,top,bot;

      float rb[] = { 0.450F, 0.600F, 1.000F };
      float gb[] = { 0.500F, 0.600F, 1.000F };
      float bb[] = { 0.333F, 0.000F, 1.000F };

  	  s.makeClut(n_col);
      s.setClut(BLACK, 0.0F, 0.0F, 0.0F);
      s.setClut(WHITE, 1.0F, 1.0F, 1.0F);
      s.setClut(SKY,0.404F,0.588F,1.000F);
      s.setClut(SEA_LIT,0.000F,0.500F,0.700F);
      s.setClut(SEA_UNLIT,0.000F,
           (float) ((ambient+(vfract/(1.0+vfract)))*0.500),
           (float) ((ambient+(vfract/(1.0+vfract)))*0.700));
				  
  /* max_col can over-rule band_size */
  while( (BAND_BASE +band_size*N_BANDS) > n_col )
  {
    band_size--;
  }
    
  for( band=0 ; band<N_BANDS; band++)
  {
    for(shade=0 ; shade < band_size ; shade++)
    {
      /* {{{   set red */

      top = rb[band];
      bot = ambient * top;
      red = bot + ((shade * (top - bot))/(band_size-1));
      if( red < 0 ) red = 0.0F;
      if( red > 1.0 ) red = 1.0F;

      /* }}} */
      /* {{{   set green */
      top = gb[band];
      bot = ambient * top;
      green = bot + ((shade * (top - bot))/(band_size-1));
      if( green < 0 ) green = 0.0F;
      if( green > 1.0 ) green = 1.0F;

      /* }}} */
      /* {{{   set blue */
      top = bb[band];
      bot = ambient * top;
      blue = bot + ((shade * (top - bot))/(band_size-1));
      if( blue < 0 ) blue = 0.0F;
      if( blue > 1.0 ) blue = 1.0F;
      /* }}} */
      s.setClut(BAND_BASE + (band*band_size) + shade,red,green,blue);
    }
  }
  }

/* }}} */
/* {{{ extract*/

private double[] extract(){
  double[] res;
  int i;
  res = m.next_strip();
  for(i=0;i<res.length;i++){
    res[i] = shift + (vscale*res[i]);
    //System.out.print(shift);
    //System.out.print(" ");
    //System.out.print(vscale);
    //System.out.print(" ");
    //System.out.println(res[i]);
  }
  return res;
}

/* }}} */
/* {{{ init_Artist_variables*/

public synchronized void init_artist_variables()
{
  double dh, dd;
 
  width=m.get_width();

  set_clut();
  cos_phi = Math.cos( phi );
  sin_phi = Math.sin( phi );
  tan_phi = Math.tan( phi );

  x_fact = cos_phi* Math.cos(alpha);
  y_fact = cos_phi* Math.sin(alpha);
  /* Need to make space between columns 1.0 for get_col */
  vscale = stretch * (double) width/m.get_fwidth();  
  //System.out.println(m.mwidth);
  //System.out.println(m.fwidth);
  //System.out.println(m.width);
  delta_shadow = tan_phi /Math.cos(alpha);
  shadow_slip = Math.tan(alpha);
  /* guess the average height of the fractal */
  varience = Math.pow( m.mwidth,(2.0 * m.fdim));
  varience = vscale * varience ;
  shift = base_shift * varience;
  varience = varience + shift;


  /* set the position of the view point */
  viewheight = altitude * width;
  viewpos = - distance * width;

  /* set viewing angle and focal length (vertical-magnification)
   * try mapping the bottom of the fractal to the bottom of the
   * screen. Try to get points in the middle of the fractal
   * to be 1 pixel high
   */
  dh = viewheight;
  dd = (width / 2.0) - viewpos;
  focal = Math.sqrt( (dd*dd) + (dh*dh) );
  tan_vangle = ((viewheight-sealevel)/ - viewpos);
  vangle = Math.atan ( tan_vangle );
  vangle -= Math.atan( (double)(graph_height/2) / focal ); 


  /* initialise the light strengths */
  vstrength = vfract * contrast /( 1.0 + vfract );
  lstrength = contrast /( 1.0 + vfract );
  if( repeat >= 0 ){
    pos=0;
  }else{
    pos=graph_width-1;
  }	

  /* use first set of heights to set shadow value */
  shadow = extract();
  a_strip = extract();
  b_strip = extract();
  blank_region(0, 0,graph_width-1, graph_height-1);
  initialised=true;
}

/* }}} */
/* {{{ get_col*/

private int get_col(double p, double p_minus_x, double p_minus_y, double shadow){
  double delta_x, delta_y;
  double delta_x_sqr, delta_y_sqr;
  double hypot_sqr;
  
  double norm, dshade;
  double effective;
  int index;
  int band, shade;
  /* {{{   if underwater*/
  if ( p < sealevel )
  {
    if( shadow > sealevel )
    {
      return( SEA_UNLIT );
    }else{
      return( SEA_LIT );
    }
  }
  /* }}} */
  /*
   * We have three light sources, one slanting in from the left
   * one directly from above and an ambient light.
   * For the directional sources illumination is proportional to the
   * cosine between the normal to the surface and the light.
   *
   * The surface contains two vectors
   * ( 1, 0, delta_x )
   * ( 0, 1, delta_y )
   *
   * The normal therefore is parallel to
   * (  -delta_x, -delta_y, 1)/sqrt( 1 + delta_x^2 + delta_y^2)
   *
   * For light parallel to ( cos_phi, 0, -sin_phi) the cosine is
   *        (cos_phi*delta_x + sin_phi)/sqrt( 1 + delta_x^2 + delta_y^2)
   *
   * For light parallel to ( cos_phi*cos_alpha, cos_phi*sin_alpha, -sin_phi)
   * the cosine is
   * (cos_phi*cos_alpha*delta_x + cos_phi*sin_alpha*delta_y+ sin_phi)/sqrt( 1 + delta_x^2 + delta_y^2)
   *
   * For vertical light the cosine is
   *        1 / sqrt( 1 + delta_x^2 + delta_y^2)
   */
   
  delta_x = p - p_minus_x;
  delta_y = p - p_minus_y;
  delta_x_sqr = delta_x * delta_x;
  delta_y_sqr = delta_y * delta_y;
  hypot_sqr = delta_x_sqr + delta_y_sqr;
  norm = Math.sqrt( 1.0 + hypot_sqr );

  /* {{{   calculate effective height */
  effective = (p + (varience * contour *
          (1.0/ ( 1.0 + hypot_sqr))));
  /* }}} */
  /* {{{   calculate colour band. */

  band = (int )(( effective / varience) * (double)N_BANDS);
  if ( band < 0 )
  {
    band = 0;
  }
  if( band > (N_BANDS - 1))
  {
    band = (N_BANDS -1);
  }
  index = (BAND_BASE + (band * band_size));

  /* }}} */

  /* {{{ calculate the illumination stength*/
  /*
   * add in a contribution for the vertical light. The normalisation factor
   * is applied later
   *
   */
  dshade = vstrength;
  
  if( p >= shadow )
  {
    /*
     * add in contribution from the main light source
     */
    /* dshade += ((double) lstrength * ((delta_x * cos_phi) + sin_phi));*/
    dshade += ((double) lstrength *
               ((delta_x * x_fact) + (delta_y * y_fact) + sin_phi));
  }
  /* divide by the normalisation factor (the same for both light sources) */
  dshade /= norm;
  /* }}} */
  /* {{{   calculate shading */

  /* dshade should be in the range 0.0 -> 1.0
   * if the light intensities add to 1.0
   * now convert to an integer
   */
  shade = (int)(dshade * (double) band_size);
  if( shade > (band_size-1))
  {
    shade = (band_size-1);
  }
  /* {{{   if shade is negative then point is really in deep shadow */
  if( shade < 0 )
  {
      shade = 0;
  }
  /* }}} */

  /* }}} */
  index += shade;

  return(index);

}

/* }}} */
/* {{{ makemap*/

  synchronized int[] makemap(){
  int[] res= new int[width];
  int i;


  res[0] = BLACK;
  for(i=1 ; i<width ; i++)
  {
    res[i] = get_col(b_strip[i],a_strip[i],b_strip[i-1],shadow[i]);
  }
  return(res);

  }

/* }}} */
/* {{{ camera*/

private int[] camera()
{
  int i, coord, last;
  int[] res = new int[graph_height];
  int col;

  /* this routine returns a perspective view of the surface */

  /*
   * optimised painters algorithm
   *
   * scan from front to back, we can avoid calculating the
   * colour if the point is not visable.
   */
  for( i=0, last=0 ; (i < width)&&(last < graph_height) ; i++ )
  {
    if( a_strip[i] < sealevel )
    {
      a_strip[i] = sealevel;
    }
    coord = 1 + project( i, a_strip[i] );
    if( coord > last )
    {
      /* get the colour of this point, the front strip should be black */
      if( i==0 )
      {
        col = BLACK;
      }else{
        col = get_col(b_strip[i],a_strip[i],b_strip[i-1],shadow[i]);
      }
      if( coord > graph_height )
      {
        coord = graph_height;
      }
      for(;last<coord;last++)
      {
        res[last]=col;
      }
    }
  }
  for(;last<graph_height;last++)
  {
    res[last]=SKY;
  }
  return(res);
}

/* }}} */
/* {{{ mirror*/

private int[] mirror()
{
  int[] res, map;
  int last_col;
  int i,j, top, bottom, coord;
  int last_top, last_bottom;
  double pivot;
  /* this routine returns a perspective view of the surface
   * with reflections in the water
   *
   */
  res = new int[graph_height];
  last_col=SKY;
  last_top=graph_height-1;
  last_bottom=0;
  /*
   * many of the optimisation in the camera routine are
   * hard to implement in this case so we revert to the
   * simple painters algorithm modified to produce reflections
   * scan from back to front drawing strips between the
   * projected position of height and -height.
   * for water stipple the colour so the reflection is still visable
   */
  map=makemap();
  pivot=2.0*sealevel;
  for(i=width-1;i>0;i--)
  {
    if(map[i] < BAND_BASE)
    {
      /* {{{ stipple water values*/

      for(j=last_bottom;j<=last_top;j++)
      {
        res[j]=last_col;
      }
      last_col=map[i];
      /* invalidate strip so last stip does not exist */
      last_bottom=graph_height;
      last_top= -1;
      /* fill in water values */
      coord=1+project(i,sealevel);
      for(j=0;j<coord;j++)
      {
        /* do not print on every other point
         * if the current value is a land value
         */
        if( ((j+base)%2==1) || (res[j]<BAND_BASE) )
        {
          res[j]=map[i];
        }
      }
      /* skip any adjacent bits of water with the same colour */
      while(map[i]==last_col)
      {
        i--;
      }
      i++;  /* the end of the for loop will decrement as well */

      /* }}} */
    }else{
      /* {{{ draw land values*/

      top = project(i,a_strip[i]);
      bottom=project(i,pivot-a_strip[i]);
      if(last_col == map[i])
      {
        if( top > last_top)
        {
          last_top=top;
        }
        if( bottom < last_bottom)
        {
          last_bottom=bottom;
        }
      }else{
        if(top < last_top)
        {
          for(j=top+1;j<=last_top;j++)
          {
            res[j]=last_col;
          }
        }
        if(bottom > last_bottom)
        {
          for(j=last_bottom;j<bottom;j++)
          {
            res[j]=last_col;
          }
        }
        last_top=top;
        last_bottom=bottom;
        last_col=map[i];
      }

      /* }}} */
    }
  }
  /* {{{ draw in front face*/

  for(j=last_bottom;j<=last_top;j++)
  {
    res[j]=last_col;
  }
  if( a_strip[0] < sealevel )
  {
    coord=1+project(0,sealevel);
  }else{
    coord=1+project(0,a_strip[0]);
  }
  for(j=0;j<coord;j++)
  {
    res[j] = map[0];
  }

  /* }}} */
  base=1-base;
  return(res);
}

/* }}} */
/* {{{ project*/

private int project (int x, double y)
{
  int pos;
  double theta;

  theta = Math.atan( (double) ((viewheight - y)/( x - viewpos)) );
  theta = theta - vangle;
  pos = (graph_height/2) - (int)(focal * Math.tan( theta));
  if( pos > (graph_height-1))
  {
    pos = graph_height-1;
  }
  else if( pos < 0 )
  {
    pos = 0;
  }
  return( pos );
}

/* }}} */
/* {{{ next_col*/

private int []next_col ()
{
  int res[];
  int i,offset=0;
  
  /* {{{    update strips */
  if(! draw_map)
  {
    if(reflec)
    {
      res = mirror();
    }else{
      res = camera();
    }
  }else{
    res = makemap();
  }
  a_strip=b_strip;
  b_strip = extract();
  /* }}} */

  /* {{{  update the shadows*/

  /* shadow_slip is the Y component of the light vector.
   * The shadows can only step an integer number of points in the Y
   * direction so we maintain shadow_register as the deviation between
   * where the shadows are and where they should be. When the magnitude of
   * this gets larger then 1 the shadows are slipped by the required number of
   * points.
   * This will not work for very oblique angles so the horizontal angle
   * of illumination should be constrained.
   */
  shadow_register += shadow_slip;
  if( shadow_register >= 1.0 )
  {
    /* {{{  negative offset*/

    while( shadow_register >= 1.0 )
    {
      shadow_register -= 1.0;
      offset++;
    }
    for(i=width-1 ; i>=offset ; i--)
    {
      shadow[i] = shadow[i-offset]-delta_shadow;
      if( shadow[i] < b_strip[i] )
      {
        shadow[i] = b_strip[i];
      }
      /* {{{    stop shadow at sea level */

      if( shadow[i] < sealevel )
      {
        shadow[i] = sealevel;
      }

      /* }}} */
    }
    for(i=0;i<offset;i++)
    {
      shadow[i] = b_strip[i];
      /* {{{    stop shadow at sea level*/
      if( shadow[i] < sealevel )
      {
        shadow[i] = sealevel;
      }
      /* }}} */
    }

    /* }}} */
  }else if( shadow_register <= -1.0 ){
    /* {{{  positive offset*/
    while( shadow_register <= -1.0 )
    {
      shadow_register += 1.0;
      offset++;
    }
    for(i=0 ; i<width-offset ; i++)
    {
      shadow[i] = shadow[i+offset]-delta_shadow;
      if( shadow[i] < b_strip[i] )
      {
        shadow[i] = b_strip[i];
      }
      /* {{{    stop shadow at sea level */
      if( shadow[i] < sealevel )
      {
        shadow[i] = sealevel;
      }
      /* }}} */
    }
    for(;i<width;i++)
    {
      shadow[i] = b_strip[i];
      /* {{{    stop shadow at sea level*/
      if( shadow[i] < sealevel )
      {
        shadow[i] = sealevel;
      }
      /* }}} */
    }
    /* }}} */
  }else{
    /* {{{  no offset*/
    for(i=0 ; i<width ; i++)
    {
      shadow[i] -= delta_shadow;
      if( shadow[i] < b_strip[i] )
      {
        shadow[i] = b_strip[i];
      }
      /* {{{    stop shadow at sea level */
      if( shadow[i] < sealevel )
      {
        shadow[i] = sealevel;
      }
      /* }}} */
    }
    /* }}} */
  }

  /* }}} */
  
  return(res);
}

/* }}} */
/* {{{ blank_region */

  private void blank_region(int lx,int ly,int hx,int hy){
	  s.blank_region(SKY, lx, ly, hx, hy);
  }

/* }}} */
/* {{{ scroll_screen */

  private void scroll_screen(int dist){
	  s.scroll_screen(SKY, dist);
  }

/* }}} */
/* {{{ plot_pixel */

  private void plot_pixel(int x, int y, int col){
	  s.plot_pixel(x, y, col);

  }

/* }}} */
/* {{{ plot_column*/

synchronized public void plot_column()
{
  int l[];
  int j;
  int mapwid;

  if( ! initialised ){
    init_artist_variables();
  }

  /* blank if we are doing the full window */
  if( repeat >= 0){
    if(pos == 0){
      blank_region(0,0,graph_width-1,graph_height-1);
    }
  }else{
    if( pos == graph_width-1){
      blank_region(0,0,graph_width-1,graph_height-1);
    }
  }
  if( scroll != 0 ){
    scroll_screen(scroll);
  }

  l = next_col();
  if( draw_map )
  {
    if( graph_height > width ){
      mapwid=width;
    }else{
      mapwid=graph_height;
    }
    for( j=0 ;j<(graph_height-mapwid); j++)
    {
      plot_pixel(pos,((graph_height-1)-j),BLACK);
    }
    for(j=0; j<mapwid ; j++)
    {
      plot_pixel(pos,((mapwid-1)-j),l[j]);
    }
  }else{
    for(j=0 ; j<graph_height ; j++)
    {
      /* we assume that the scroll routine fills the
       * new region with a SKY value. This allows us to
       * use a testured sky for B/W displays
       */
      if( l[j] != SKY )
      {
        plot_pixel(pos,((graph_height-1)-j),l[j]);
      }
    }
  }
  scroll = 0;
  /* now update pos ready for next time */
  if( repeat >=0 ){
    pos++;
    if(pos >= graph_width)
    {
      pos -=  repeat;
      if( pos < 0 || pos > graph_width-1 )
      {
        pos=0; 
      }else{
        scroll = repeat;
      }
    }
  }else{
    pos--;
    if( pos < 0 ){
      pos -=   repeat;
      if( pos < 0 || pos > (graph_width-1) ){
	pos=graph_width-1;
      }else{
	scroll = repeat;
      }
    }
  }

}

public int getWidth() {
	return graph_width;
}
public int getHeight(){
	return graph_height;
}

public int getScroll() {
	return scroll;
}

public boolean isMap() {
	return draw_map;
}

public boolean setMap(boolean draw_map) {
	if( this.draw_map == draw_map){
		return false;
	}
	this.draw_map = draw_map;
	return true;
}

public boolean isReflec() {
	return reflec;
}

public boolean setReflec(boolean reflec) {
	if( this.reflec == reflec){
		return false;
	}
	this.reflec = reflec;
	return true;
}
public boolean isIntialised(){
	return initialised;
}

public int getRepeat() {
	return repeat;
}

public void setRepeat(int repeat) {
	this.repeat = repeat;
}

/* }}} */
}









