/*
 * coded expression sound evaluator & generator
 */
package com.vs.anu.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;

/*
 
Usage:
 
  AlgebraicSound asFunc;
  
  if (asFunc!=null) { asFunc.SoundStop(); asFunc=null; } else {
				switch (iForm) { 
				case 0: asFunc=new VoiceRithm(); 			break;
				case 1: asFunc=new Flower(); 				break;
				case 2: asFunc=new Bowl(); 					break;
				case 3: asFunc=new HarmonicExpansion(); 	break;
				}
				asFunc.setDuration(secs);
				asFunc.setPitch(freq);
				asFunc.SoundStart();
			}
 */

public abstract class AlgebraicSound {
    public double x = 0, t = 0;
    protected int sampleRate = 22050 / 2;
    private double M_PI = Math.PI, delta = 2. * M_PI / sampleRate;
    public double maxVol = Short.MAX_VALUE, midVol = maxVol / 2, phi = 0.618033988749895, pi = M_PI;
    private int maxFastSin = 100;
    private FastSin[] fs;
    private double yL, yR, maxL, minL, maxR, minR, diffL, diffR, scaleL, scaleR, secs, durationSecs = 5, hz = 440;

    public boolean isPlaying = false;
    private playSoundTask ps = new playSoundTask();
    int bs = 1024;
    short[] sbuff = new short[bs]; // stereo
    double deltaSec = (bs / 2.) / sampleRate;

    public abstract double evalLeft();    // works with public 'x' var. generate in the range -Short.MAX_VALUE..+Short.MAX_VALUE

    public abstract double evalRight();

    public abstract void prapare();

    protected void finalize() {
        SoundStop();
    }

    private void init() {
        fs = new FastSin[maxFastSin];
        minL = minR = +Double.MAX_VALUE;
        maxL = maxR = -Double.MAX_VALUE;
        diffL = diffR = 0;
        scaleL = scaleR = 1;
        scale();
    }

    private void initFS(int nw, double amp, double hz) {
        if (nw < maxFastSin & fs[nw] == null) fs[nw] = new FastSin(amp, hz, sampleRate, 0);
    }

    public void setPitch(double hz) {
        this.hz = hz;
    }

    public double getPitch() {
        return hz;
    }

    public int setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        init();
        x = t = secs = 0;
        delta = 2. * M_PI / sampleRate;
        deltaSec = (bs / 2.) / sampleRate;
        return sampleRate;
    }

    public void setDuration(double durationSecs) {
        this.durationSecs = durationSecs;
        secs = 0;
    }

    public double nextX() {
        return t = (x += delta);
    }

    public double getX() {
        return t = x;
    }

    public void updateMinMax() {
        minL = Math.min(yL, minL);
        maxL = Math.max(yL, maxL);
        diffL = Math.abs(maxL - minL);
        minR = Math.min(yR, minR);
        maxR = Math.max(yR, maxR);
        diffR = Math.abs(maxR - minR);
    }

    public void scale() {
        long lookUPms = 200;
        for (long t = System.currentTimeMillis(); System.currentTimeMillis() - t < lookUPms; ) {
            yL = evalLeft();
            yR = evalRight();
            updateMinMax();
            for (int j = 0; j < 10; j++) nextX();
        }
        x = t = 0;
        scaleL = 1.8 * maxVol / diffL; // 1.8 = 2 - 10% to avoid possible overrun
        scaleR = 1.8 * maxVol / diffR;
    }

    public double wave(int nw, double amp, double hz) {
        initFS(nw, amp, hz);
        return fs[nw].calc();
    }

    public double wave(int nw, double hz) {
        return wave(nw, maxVol, hz);
    }

    public double wave(double amp, double hz) {
        return wave(0, amp, hz);
    } // use fs[0]

    public double wave(double hz) {
        return wave(0, maxVol, hz);
    }

    public double note(int note, int octave) {
        return Math.sin(x * MusicFreq.NoteOct2Freq(note, octave));
    }

    public double osc(double f) {
        return Math.sin(x * f);
    }

    public double hz2oct(double f, int o) {
        return MusicFreq.FreqInOctave(f, o);
    }

    public double sin(double x) {
        return Math.sin(x);
    }

    public double cos(double x) {
        return Math.sin(x);
    }

    public double sec(double x) {
        return x * 2 * pi;
    }

    public double exp(double x) {
        return Math.exp(x);
    }

    public void SoundStart() {
        ps = new playSoundTask(); // need a new copy each time
        isPlaying = true;
        ps.execute(); // generate the sound
    }

    public void SoundStop() {
        isPlaying = false;
        if (ps != null) ps.cancel(true);
        ps = null;
    }

    public void SoundSwitch() {
        if (isPlaying) SoundStop();
        else SoundStart();
    }

    public void SoundPlay(float secs) {
        SoundStart();
        delay((int) (secs * 1000f));
        SoundStop();
    }

    public void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
        }
    }

    public void checkLoop(double deltaSec) {
        if ((secs += deltaSec) >= durationSecs) x = t = secs = 0;
    } // loop 'durationSecs'

    public void fillBuffer() {
        for (int i = 0; i < bs; i += 2) {
            sbuff[i + 0] = (short) (yL = scaleL * evalLeft());
            sbuff[i + 1] = (short) (yR = scaleR * evalRight());
            nextX();
        }
        checkLoop(deltaSec);
    }

    public short[] getBuffer() {
        return sbuff;
    }

    public int getBufferSize() {
        return bs;
    }

    public int getGenPerSec(int secs) {
        return secs * sampleRate / (bs / 2);
    } // number of generated buffers per sec.

    class playSoundTask extends AsyncTask<Void, Void, Void> {
        AudioTrack track;
        int minSize;


        // init the audio system
        public void InitAudio() {
            minSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    minSize, AudioTrack.MODE_STREAM);
            track.play();
        }

        @Override
        protected void onPreExecute() { // init track & wg
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            InitAudio();
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected Void doInBackground(Void... params) { // gen & write
            setSampleRate(sampleRate);
            prapare();
            while (isPlaying) {
                fillBuffer();
                track.write(sbuff, 0, bs);
            }
            return null;
        }
    }
}


/*
 * several implementations
 
class Flower extends AlgebraicSound {
	double am=3.14, f=getPitch(), d=10, f1=f+2, rc=pi;
	double	flowerR() {return sin(am*t)*sin(t*f)* sin((f/d)/(t+1)) +
			phi*sin(am*t)*sin(t*f*phi)* sin((f*phi/d)/(t+1))+
			(phi/3)*sin(am*t)*sin(t*f*2)* sin((f*2/d)/(t+1));}
	double flowerL() {return sin(am*t)*sin(t*(f+rc))* sin(((f+rc)/d)/(t+1)) +
			phi*sin(am*t)*sin(t*(f+rc)*phi)* sin(((f+rc)*phi/d)/(t+1))+
			(phi/3) * sin(am*t)*sin(t*(f+rc)*2)* sin(((f+rc)*2/d)/(t+1)); }

	@Override public double 	evalLeft() 	{	return flowerL();	} // use public param 'x' must be in the range +-Short.Max_VALUE
	@Override public double 	evalRight() 	{	return flowerR();	}
	@Override public void 		prapare() { f=getPitch(); 	}
}
class AlienMarch extends AlgebraicSound {
	double alienMarch() {	return sin(10/(t+1))*(	sin(t*(1+sin(t*80)))+sin(t*(1+sin(t*180))) )*osc(2.5); }
	@Override public double 	evalLeft() 	{	return alienMarch();	} // use public param 'x' must be in the range +-Short.Max_VALUE
	@Override public double 	evalRight() 	{	return alienMarch();	}
	@Override public void 		prapare() {}
}
class VoiceRithm extends AlgebraicSound {
	double f=194;
	double voiceRithm()  { return 1.6*sin(3*t)*sin(t*f)* sin(44/(t+1)) +
			sin(3*t)*sin(t*1.6*f)* sin(44/(t+1)) +
			0.6*sin(3*t)*sin(t*0.6*f)* sin(44/(t+1)) ; }
	@Override public double evalLeft() 	{	return voiceRithm();	} // use public param 'x' must be in the range +-Short.Max_VALUE
	@Override public double evalRight() 	{	return voiceRithm();	}
	@Override public void 		prapare() 	{ 	f=getPitch()*2; 	}
}
class Bowl extends AlgebraicSound {
	double f0=194.18, f1=293.7, delay=3, bal=0.3;
	double bowlR()  { return (1+osc(bal))*	(osc(f0)* sin(f0/(t+sec(delay)) +
			phi*osc(f1)* sin(f1/(t+sec(delay))))); }
	double bowlL()  { return osc(bal)*	(
			osc(f0)* sin(f0/(t+sec(delay)) +
					phi*osc(f1)* sin(f1/(t+sec(delay))))); }

	@Override public double evalLeft() 	{	return bowlL();	} // use public param 'x' must be in the range +-Short.Max_VALUE
	@Override public double evalRight() 	{	return bowlR();	}
	@Override public void 	  prapare() { f0=getPitch(); 	}
}
class HarmonicExpansion extends AlgebraicSound {
	double f0=194.18, rc=3;
	double aexR()  { return  exp(-0.3*t)*(sin(sin(f0*t)*t)+phi * sin(sin(f0*t)*t)); }
	double aexL()  { return  exp(-0.3*t)*(sin(sin((f0+rc)*t)*t)+phi * sin(sin((f0+rc)*t)*t)); }
	@Override public double evalLeft() 	{	return aexR();	} // use public param 'x' must be in the range +-Short.Max_VALUE
	@Override public double evalRight() 	{	return aexL();	}
	@Override public void 		prapare() { f0=getPitch(); 	}
}
class HasrmonicVibs extends AlgebraicSound { // too complex for an arm cpu
	double fact1=1.6, fact2=.6, f0, div=10, diff=4;
	double hvR() {return fact1*sin(3*t)*sin(t*f0)* sin(div/(t+1)) +
			sin(3*t)*sin(t*fact1*f0)* sin(div/(t+1)) +
			fact2*sin(3*t)*sin(t*fact2*f0)* sin(div/(t+1)) +
			0.3*sin(3*t)*sin(t*140)* sin(div/(t+1));
	}
	double hvL() {return fact1*sin(3*t)*sin(t*(f0+diff))* sin(div/(t+1)) +
			sin(3*t)*sin(t*fact1*(f0+diff))* sin(div/(t+1)) +
			fact2*sin(3*t)*sin(t*fact2*(f0+diff))* sin(div/(t+1)) +
			0.3*sin(3*t)*sin(t*140)* sin(div/(t+1));
	}
	@Override public double 	evalLeft() 	{	return hvL();	} // use public param 'x' must be in the range +-Short.Max_VALUE
	@Override public double 	evalRight() {	return hvR();	}
	@Override public void 		prapare() 	{ 	f0=getPitch(); 	}

}

class asDemo extends AlgebraicSound { // for testing purposes
	asDemo(float secs) {SoundPlay(secs);} // play 'secs' seconds 

	double evalSin440() { return maxVol*Math.sin(440*x);} // evaluators samples, remember 'maxVol'
	double evalSin444() { return maxVol*Math.sin(444*x);}
	double eval1() {return evalSin440();}
	double eval2() {return wave(maxVol,480);}
	double eval3() {return maxVol*note(0,0);}
	double eval4() {return wave(1,1,3)*wave(490*(1+wave(2,1,14)));}
	double eval5() {return evalSin444();}

	double bwL() {return wave(0,440);}
	double bwR() {return wave(1,444);}

	double fp=194.18;
	double planetL() {	return  osc(hz2oct(fp,-7))*osc(fp); }
	double planetR() {	return 
			osc(hz2oct(fp,-7))*osc(fp*(1+Math.exp(-1/(2*x+1))))	+
			phi*osc(hz2oct(fp*phi,-1))+
			phi*osc(hz2oct(fp*phi,1));
	}
	double alienMarch() {	return sin(10/(t+1))*(	sin(t*(1+sin(t*80)))+sin(t*(1+sin(t*180))) )*osc(2.5); }
	double voiceRithm()  { return 1.6*sin(3*t)*sin(t*440)* sin(44/(t+1)) +
			sin(3*t)*sin(t*1.6*440)* sin(44/(t+1)) +
			0.6*sin(3*t)*sin(t*0.6*440)* sin(44/(t+1)) ; }

	double am=3.14, f=409.27, d=10, f1=f+2, rc=pi;
	double	flowerR() {return sin(am*t)*sin(t*f)* sin((f/d)/(t+1)) +
			phi*sin(am*t)*sin(t*f*phi)* sin((f*phi/d)/(t+1))+
			(phi/3)*sin(am*t)*sin(t*f*2)* sin((f*2/d)/(t+1));}
	double flowerL() {return sin(am*t)*sin(t*(f+rc))* sin(((f+rc)/d)/(t+1)) +
			phi*sin(am*t)*sin(t*(f+rc)*phi)* sin(((f+rc)*phi/d)/(t+1))+
			(phi/3) * sin(am*t)*sin(t*(f+rc)*2)* sin(((f+rc)*2/d)/(t+1)); }

	@Override public double evalLeft() 	{	return flowerL();	} // use public param 'x' must be in the range +-Short.Max_VALUE
	@Override public double evalRight() 	{	return flowerR();	}
	@Override public void 		prapare() { 	}
}

*/

