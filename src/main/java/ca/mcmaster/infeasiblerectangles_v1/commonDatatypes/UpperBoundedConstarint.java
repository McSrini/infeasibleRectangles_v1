/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.commonDatatypes;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class UpperBoundedConstarint {
     
    public List<VariableCoefficientTuple>   sortedConstraintExpr = new ArrayList<VariableCoefficientTuple>() ;     
    public double upperBound; 
     
    //private constructor for local use in this file
    private   UpperBoundedConstarint(  ){
        
    }
    
    
    public UpperBoundedConstarint (LowerBoundConstraint lbc) {
        this.sortedConstraintExpr.addAll(lbc.sortedConstraintExpr );
        this.upperBound = lbc.lowerBound;
    }
    
    //even with the worst possible choice of free vars, am I still feasible ?
    public boolean isGauranteedFeasible (boolean isStrict){
        double worstValue = ZERO ;
        for (VariableCoefficientTuple tuple : sortedConstraintExpr) {
            if (tuple.coeff>ZERO)    worstValue +=tuple.coeff;
        }
        return isStrict? (worstValue < this.upperBound) : (worstValue <= this.upperBound);
    }
    
        //constraint  , disregarding fixed vars    
    public UpperBoundedConstarint  getReducedConstraint (  List <String> varsFixedAtZero, List <String> varsFixedAtOne){
        
        UpperBoundedConstarint reducedConstraint = new UpperBoundedConstarint();
        
        reducedConstraint.sortedConstraintExpr = new ArrayList<VariableCoefficientTuple> ();
        reducedConstraint.upperBound = this.upperBound;
                
        List <String> fixedVarNamesZero = new ArrayList <String>();
        List <String> fixedVarNamesOne = new ArrayList <String>();
        for (String binvar : varsFixedAtOne){
            fixedVarNamesOne.add(binvar );
        }
        for (String binvar : varsFixedAtZero){
            fixedVarNamesZero.add(binvar );
        }
                
        for (VariableCoefficientTuple tuple : this.sortedConstraintExpr){
            if (!fixedVarNamesOne.contains(tuple.varName) && !fixedVarNamesZero.contains(tuple.varName)  ) {
                reducedConstraint.sortedConstraintExpr .add(tuple);
            }else {
                //find the val to which this var is fixed                
                if (fixedVarNamesOne.contains(tuple.varName)) reducedConstraint.upperBound-= tuple.coeff;
            }
        }
        return reducedConstraint;
    }
    
    
}
