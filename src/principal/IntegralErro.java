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
public class IntegralErro extends Metodos {

    @Override
    ArrayList<Double> Run(Controladores controlador) {
        switch(controlador){
            case P:
                this.Kp = (this.Tau/(this.K*this.Theta));
                break;
            case PI:
                this.Kp = 0.9*(this.Tau/(this.K*this.Theta));
                this.Ki = 3.33*Theta;
                
                break;
            case PID:
                this.Kp = 1.2*(this.Tau/(this.K*this.Theta));
                this.Ki = 2*this.Theta;
                this.Kd = 0.5*this.Theta;
                
                break;
            default:
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        }
        
        return new ArrayList<Double>();
    }
}