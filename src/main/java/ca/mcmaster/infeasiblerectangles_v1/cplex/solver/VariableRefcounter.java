/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex.solver;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.*;
import ca.mcmaster.infeasiblerectangles_v1.IR_Driver;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.VariableCoefficientTuple;
import java.util.*;

/**
 *
 * @author tamvadss
 */
public class VariableRefcounter {
    
    //the refcount for each var
    Map<String , Integer> varRefcounts = new HashMap<String , Integer> ();
    //the vars for each refcount
    Map<Integer, List<String>> refcountsVars = new HashMap<Integer, List<String > > ();
    //same map but with lists sorted
    Map<Integer, List<String>> sortedRefcountsVars = new HashMap<Integer, List<String > > ();
    
    public VariableRefcounter (List<Rectangle>   descendentRectangles , List<String> varNames  ) {
        
        //init
        for (String name : varNames ){
            varRefcounts.put(name, ZERO);
        }
        
        for (Rectangle rect:   descendentRectangles){
            for (String zeroVar :rect.zeroFixedVariables) {
                int currentCount = varRefcounts.get(zeroVar);
                varRefcounts.put(zeroVar, ONE+currentCount);
            }
            for (String oneVar : rect.oneFixedVariables){
                int currentCount = varRefcounts.get(oneVar);
                varRefcounts.put(oneVar, ONE+currentCount);
            }
        }
        
        for (Map.Entry<String, Integer> entry : varRefcounts.entrySet()) {
            
            String varnameKey = entry.getKey();
            Integer refcount = entry.getValue();
            if (refcount==ZERO) continue;
            
            List<String > currentVarList = (refcountsVars.get(refcount )==null  ) ?  new ArrayList<String > () : refcountsVars.get(refcount );
            currentVarList.add( varnameKey);
            refcountsVars.put (refcount,currentVarList ) ;
        }
        
        //for each list of vars, get them in order of decreasing objective coeff magnitude
        for (Map.Entry<Integer, List<String> > entry : refcountsVars.entrySet()) {
             List<String > currentVarList = entry.getValue();
             sortedRefcountsVars.put (entry.getKey(), currentVarList.size()>ONE ? sortByObjMagnitude (currentVarList): currentVarList ) ;
        }
        
    }
    
    public void getVarRefcounts (List <String> branchingVariablesInPriorityOrder){
        for (Map.Entry<Integer, List<String> > entry : sortedRefcountsVars.entrySet()) {
            branchingVariablesInPriorityOrder.addAll (entry.getValue()) ;
        }
    }
    
    private List<String >  sortByObjMagnitude ( List<String >currentVarList) {
          
        List<String > sortedVarList = new ArrayList<String >();
        for (VariableCoefficientTuple tuple: IR_Driver.objective.objectiveExpr){
            if (currentVarList.contains( tuple.varName)) sortedVarList.add(tuple.varName);
            if (sortedVarList.size()==currentVarList.size()) break;
        }
        
        Collections.reverse(sortedVarList);
        return sortedVarList;
    }
    
}
