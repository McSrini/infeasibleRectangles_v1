/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.commonDatatypes;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.*;
import ca.mcmaster.infeasiblerectangles_v1.IR_Driver;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.*;

/**
 *
 * @author tamvadss
 */
public class Rectangle {
    
    //note that some vars can be free
    public List <String> zeroFixedVariables = new ArrayList <String>();
    public List <String> oneFixedVariables = new ArrayList <String>();
    
    public double lpRelaxValueMinimization;     
    public double maximization_lpRelaxValue;     
         
    private static Logger logger=Logger.getLogger(Rectangle.class);
        
    static {
                    
        logger=Logger.getLogger(Rectangle.class);
        logger.setLevel(Level.DEBUG);
        
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+Rectangle.class.getSimpleName()+ LOG_FILE_EXTENSION);
           
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
    }
    
    public Rectangle (List <String> zeroFixedVariables , List <String> oneFixedVariables ){
        this.zeroFixedVariables .addAll(zeroFixedVariables);
        this.oneFixedVariables  .addAll( oneFixedVariables);
      
    }
    
    //get min possible value
    public double getLpRelaxValueMinimization () {
        
        this.lpRelaxValueMinimization = ZERO;
        
        for (VariableCoefficientTuple tuple: IR_Driver.objective.objectiveExpr){
            if (this.oneFixedVariables.contains(tuple.varName) ) this.lpRelaxValueMinimization+=tuple.coeff;
            if (!this.oneFixedVariables.contains(tuple.varName) && !this.zeroFixedVariables.contains(tuple.varName)   ){
                //free var
                if (tuple.coeff<ZERO) this.lpRelaxValueMinimization+=tuple.coeff;
            }
        }
        
        return lpRelaxValueMinimization;
    }
    
    public String toString (){
        String result="" ;
        result += "zero fixed vars :";
        for (String str: zeroFixedVariables){
            result += str + ",";
        }
        result += "\none fixed vars :";
        for (String str: oneFixedVariables){
            result += str + ",";
        }
        return result;
    }

}
