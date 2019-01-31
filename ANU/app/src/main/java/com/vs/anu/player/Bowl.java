package com.vs.anu.player;

public class Bowl extends AlgebraicSound {
	double f0=194.18, f1=293.7, delay=3, bal=0.3;
	double bowlR()  { return (1+osc(bal))*	(osc(f0)* sin(f0/(t+sec(delay)) + phi*osc(f1)* sin(f1/(t+sec(delay))))); }
	double bowlL()  { return osc(bal)*	(osc(f0)* sin(f0/(t+sec(delay)) + phi*osc(f1)* sin(f1/(t+sec(delay))))); }
	@Override public double evalLeft() 	{	return bowlL();	} // use public param 'x' must be in the range +-Short.Max_VALUE
	@Override public double evalRight() {	return bowlR();	}
	@Override public void 	  prapare() { 	f0=getPitch(); 	}
}