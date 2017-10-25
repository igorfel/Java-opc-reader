/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package principal;

import java.util.ArrayList;

/**
 *
 * @author igorf
 */
public abstract class Metodos {
    double Kp, Ki, Kd;
    double K, Tau, Theta;
    
    Metodos(){
        Kp = 0;
        Ki = 0;
        Kd = 0;
    }
    
    Metodos(double K, double Tau, double Theta) {
        this.K = K;
        this.Tau = Tau;
        this.Theta = Theta;
    }
    
    abstract ArrayList<Double> Run(Controladores controlador);

    public double getKp() {
        return Kp;
    }

    public void setKp(double Kp) {
        this.Kp = Kp;
    }

    public double getKi() {
        return Ki;
    }

    public void setKi(double Ki) {
        this.Ki = Ki;
    }

    public double getKd() {
        return Kd;
    }

    public void setKd(double Kd) {
        this.Kd = Kd;
    }

    public double getP() {
        return K;
    }

    public void setP(double P) {
        this.K = P;
    }

    public double getPI() {
        return Tau;
    }

    public void setPI(double PI) {
        this.Tau = PI;
    }

    public double getPID() {
        return Theta;
    }

    public void setPID(double PID) {
        this.Theta = PID;
    }
    
    
}
