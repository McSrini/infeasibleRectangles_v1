/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.commonDatatypes;

import ca.mcmaster.infeasiblerectangles_v1.IR_Driver; 
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class Objective {
        
    public List<VariableCoefficientTuple>   objectiveExpr ; 
      
    public Objective( List<VariableCoefficientTuple>  expr   ) {
         
        Collections.sort(expr);
        objectiveExpr = expr;
        
    }
    
    public String toString() {
        String str = "";
        
        for (VariableCoefficientTuple tuple : objectiveExpr) {
            str += ("Var is " + tuple.varName  + " and its coeff is "+ tuple.coeff+"\n") ;
        }
        return str;
    }
}
