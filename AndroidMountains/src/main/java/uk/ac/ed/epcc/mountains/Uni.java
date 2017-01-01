package uk.ac.ed.epcc.mountains;

import java.io.Serializable;

public final class Uni implements Serializable{

private float[]  u;
private int  i97, j97;

private float c;
static final float  cd = 7654321.0F / 16777216.0F;
static final float  cm = 16777213.0F / 16777216.0F;

static final double pi = 3.1415926536D;
static final double two = 2.0D, zero = 0.0D;


public Uni(int seed)
{
  int is1, is2;
//  int is1max = 31328, is2max = 30081;
  int i, j, k, l, m;
  float s, t;
  int ii, jj;

  u = new float[97];

  is1 = seed / 30082;
  is2 = seed - (30082 * is1);

  i = ((is1 / 177) % 177) + 2;
  j = (is1 % 177) + 2;
  k = ((is2 / 169) % 178) + 1;
  l = is2 % 169;

  for(ii = 0; ii < 97; ii++) {
    s = 0.0F;
    t = 0.5F;
    for(jj = 0; jj < 24; jj++) {
      m = (((i * j) % 179) * k) % 179;
      i = j;
      j = k;
      k = m;
      l = ((53 * l) + 1) % 169;
      if( ((l * m) % 64) >= 32)
	s = s + t;
      t = 0.5F * t;
    }
    u[ii] = s;
  }


  c = 362436.0F / 16777216.0F;
  i97 = 96;
  j97 = 32;

}

public Uni(){
  this((int)(System.currentTimeMillis()/1000L));
}

float nextFloat()
{
  float r;

  r = u[i97] - u[j97];

  if(r < 0.0F)
    r += 1.0F;

  u[i97] = r;

  --i97;
  if(i97 < 0)
    i97 = 96;

  --j97;
  if(j97 < 0)
    j97 = 96;

  c -= cd;
  if(c < 0.0F)
    c += cm;

  r -= c;
  if(r < 0.0F)
    r += 1.0F;
  return(r);
}

double nextGaussian()
{
	double ran1, ran2;

	for( ran1=zero ; ran1 == zero ; ){
		ran1 = (double) nextFloat();
	}

	ran2 = (double) nextFloat();
	return ( Math.sqrt(-two * Math.log(ran1)) * Math.cos(two * pi * ran2) );
}


}


