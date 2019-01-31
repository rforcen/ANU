package com.vs.anu.ogl;

import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.opengles.GL10;

public class Anu {
    fbVertex fbAnu;
    float deltaPhi = 0.05f, deltaTheta = 0.28f; // nice (.1, .28)
    int n = 420; // nice 320
    List<Tube> tubes = new ArrayList<Tube>();
    private float[] p1 = new float[3], p2 = new float[3];

    public fbVertex createAnuCoords() {
        return createAnuCoords(1, .90f);
    } // nice (1, .95)

    public fbVertex createAnuCoords(float R, float r) {
        float phi = 0, theta = 0;
        fbAnu = new fbVertex(n * 3);
        for (int i = 0; i < n; i++, phi += deltaPhi, theta += deltaTheta)
            fbAnu.add((R + r * cos(phi)) * cos(theta), (R + r * cos(phi)) * sin(theta), r * sin(phi));
        return fbAnu;
    }

    public void drawAnu(GL10 gl) {
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, fbAnu.getBuffer());
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, fbAnu.getSize() / 3);
    }

    public void createAnuTubes() {
        float tubeRadius = 0.04f, gold[] = {0.5f, 0.5f, 0, 0}, defaultColor[] = gold; // rgbA
        int slices = 12;
        fbAnu = createAnuCoords();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0, i3 = i * 3, i31 = (i + 1) * 3; j < 3; j++) {
                p1[j] = fbAnu.getBuffer().get(i3 + j);
                p2[j] = fbAnu.getBuffer().get(i31 + j);
            }
            tubes.add ( new Tube(p1, p2, tubeRadius, slices, defaultColor, 1.05f) );
        }
    }

    public void drawAnuTubes(GL10 gl) {
        for (Tube tube : tubes) tube.drawSolid(gl);
    }

    private float cos(float x) {
        return (float) Math.cos(x);
    }

    private float sin(float x) {
        return (float) Math.sin(x);
    }
}
