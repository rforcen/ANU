package com.vs.anu.ogl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.vs.anu.color.*;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.AttributeSet;

public class GLanimation extends GLSurfaceView {
    Renderer renderer;

    public GLanimation(Context context) {
        super(context);
        initGL();
    }

    public GLanimation(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGL();
    }

    void initGL() {
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new Renderer(this);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        refresh();
    }

    public void refresh() {
        requestRender();
    }

    public void stopAnim() {
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void startAnim() {
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    class Renderer implements GLSurfaceView.Renderer {
        Anu anu;
        float ang = 0;

        class TimeControl {
            private long startTime, endTime, dt, lapSleep = 45;

            TimeControl(long lapSleep) {
                this.lapSleep = lapSleep;
                startTime = System.currentTimeMillis();
            }

            public void start() {
                startTime = System.currentTimeMillis();
            }

            public void check() {
                endTime = System.currentTimeMillis();
                dt = endTime - startTime;
                if (dt < lapSleep) try {
                    Thread.sleep(lapSleep - dt);
                } catch (InterruptedException e) {
                }
            }
        }

        TimeControl timeControl = new TimeControl(43);
        private float zoom = -8f;


        public Renderer(GLSurfaceView glSurface) {
            glSurface.setRenderer(this);
            initModels();
        }

        private void initModels() {
            anu = new Anu();
            anu.createAnuTubes();
        }

        @Override
        public synchronized void onDrawFrame(GL10 gl) {
            timeControl.start();

            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);  // Clear the screen to black
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0, 0, zoom);

            enableTransparency(gl);
            enableColorArrayMaterial(gl);
            enableNormalArray(gl);
            disableTextures(gl);

            drawModelAnu(gl);

            timeControl.check();
        }

        private void drawModelAnu(GL10 gl) {
            gl.glScalef(.9f, 1.5f, 1);
            gl.glRotatef(90, 1, 0, 0);
            gl.glRotatef(ang++, 0, 0, 1);
            gl.glColor4f(0, 0, 1, 1);
            anu.drawAnuTubes(gl);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int w, int h) {//Define the view frustum
            gl.glViewport(0, 0, w, h);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            float ratio = (float) w / h;
            GLU.gluPerspective(gl, 45.0f, ratio, 1, 100f);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
            gl.glEnable(GL10.GL_DEPTH_TEST);
            gl.glDepthFunc(GL10.GL_LEQUAL);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisable(GL10.GL_DITHER);

            sceneInit(gl, 0);
        }

        public void setColor(GL10 gl, int color) {
            gl.glColor4f(ColorScale.getRedf(color), ColorScale.getGreenf(color), ColorScale.getBluef(color), 1);
        }

        public void enableColorArray(GL10 gl) {
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        }

        public void disableColorArray(GL10 gl) {
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        }

        public void enableColorArrayMaterial(GL10 gl) {
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            gl.glEnable(GL10.GL_COLOR_MATERIAL);
        }

        public void enableNormalArray(GL10 gl) {
            gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
        }

        public void setScale(GL10 gl, float scale) {
            gl.glScalef(scale, scale, scale);
        }

        public void enableTransparency(GL10 gl) {
            gl.glEnable(GL10.GL_BLEND);
        }

        public void setColor(GL10 gl, int color, float alpha) {
            gl.glColor4f(ColorScale.getRedf(color), ColorScale.getGreenf(color), ColorScale.getBluef(color), alpha);
        }

        public void disableTextures(GL10 gl) {
            gl.glDisable(GL10.GL_TEXTURE_2D);
        }

        public void sceneInit(GL10 gl, int color) { // works nice for golden solid colors (requires normals)
            float lmodel_ambient[] = {0, 0, 0, 0};
            float lmodel_twoside[] = {GL10.GL_FALSE};
            float light0_ambient[] = {0.1f, 0.1f, 0.1f, 1.0f};
            float light0_diffuse[] = {1.0f, 1.0f, 1.0f, 0.0f};
            float light0_position[] = {0.8660254f, 0.5f, 1, 0};
            float light0_specular[] = {1, 1, 1, 0};
            float bevel_mat_ambient[] = {0, 0, 0, 1};
            float bevel_mat_shininess[] = {40};
            float bevel_mat_specular[] = {1, 1, 1, 0};
            float bevel_mat_diffuse[] = {1, 0, 0, 0};

            gl.glEnable(GL10.GL_CULL_FACE);
            gl.glCullFace(GL10.GL_BACK);
            gl.glEnable(GL10.GL_DEPTH_TEST);
            gl.glClearDepthf(1);

            //gl.glClearColor(ColorScale.getRedf(color), ColorScale.getGreenf(color), ColorScale.getBluef(color), 1);

            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, light0_ambient, 0);
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, light0_diffuse, 0);
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, light0_specular, 0);
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, light0_position, 0);
            gl.glEnable(GL10.GL_LIGHT0);

            gl.glLightModelfv(GL10.GL_LIGHT_MODEL_TWO_SIDE, lmodel_twoside, 0);
            gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
            gl.glEnable(GL10.GL_LIGHTING);

            gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_AMBIENT, bevel_mat_ambient, 0);
            gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_SHININESS, bevel_mat_shininess, 0);
            gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_SPECULAR, bevel_mat_specular, 0);
            gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_DIFFUSE, bevel_mat_diffuse, 0);

            gl.glEnable(GL10.GL_COLOR_MATERIAL);
            //			gl.glShadeModel(GL10.GL_SMOOTH);
        }

    }
}

