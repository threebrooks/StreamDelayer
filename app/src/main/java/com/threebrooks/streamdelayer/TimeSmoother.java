package com.threebrooks.streamdelayer;

import android.util.Log;

public class TimeSmoother {

    double S_t1, b_t1;
    double base_alpha, base_gamma;
    long lastT;

    TimeSmoother(double _alpha, double _gamma, double S, double b) {
        base_alpha = _alpha;
        base_gamma = _gamma;
        resetTo(S,b);
    }

    public void resetTo(double S, double b) {
        Log.d(MainActivity.TAG,"Resetting to "+S+","+b);
        S_t1 = S;
        b_t1 = b;
        lastT = System.currentTimeMillis();
    }

    public void addVal(double s) {
        double delta_t = (System.currentTimeMillis()-lastT)/1000.0;
        if (delta_t == 0.0) return;

        double alpha = Math.exp(Math.log(base_alpha)*delta_t);
        double gamma = Math.exp(Math.log(base_gamma)*delta_t);
        double S_t = (1.0-alpha)*s+alpha*(S_t1+b_t1*delta_t);
        double b_t = (1.0-gamma)*(S_t-S_t1)/delta_t+gamma*b_t1;

        S_t1 = S_t;
        b_t1 = b_t;
        lastT = System.currentTimeMillis();
    }

    public double getCurrentVal() {
        double delta_t = (System.currentTimeMillis()-lastT)/1000.0;
        return S_t1+delta_t*b_t1;
    }
}
