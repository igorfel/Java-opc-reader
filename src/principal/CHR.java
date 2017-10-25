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
public class CHR extends Metodos {

    @Override
    ArrayList<Double> Run(Controladores controlador) {
        switch(controlador){
            case P:
                this.Kp = 0.3*(this.K*this.Theta);
                break;
            case PI:
                this.Kp = 0.6*(this.Theta/(this.K*this.Theta));
                this.Ki = 4*this.Theta;
                
                break;
            case PID:
                this.Kp = 0.95*(this.Tau/(this.K*this.Theta));
                this.Ki = 2.375*this.Theta;
                this.Kd = 0.421*this.Theta;
                
                break;
            default:
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
        ArrayList<Double> result = new ArrayList<>();
        result.add(this.Kp);
        result.add(this.Ki);
        result.add(this.Kd);
        
        return result;
    }
    
}
