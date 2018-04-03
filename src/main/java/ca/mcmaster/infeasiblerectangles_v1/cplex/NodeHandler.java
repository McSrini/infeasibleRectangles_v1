/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO; 
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.USE_STRICT_INEQUALITY;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.UpperBoundedConstarint;
import ilog.concert.IloException;
import java.util.List;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
 

/**
 *
 * @author tamvadss
 */
public class NodeHandler extends IloCplex.NodeCallback {
         
      
    //the root of this tree has these
    private UpperBoundedConstarint currentConstraintRoot;
    private List <String> zeroFixedVariablesRoot  ;
    private List <String> oneFixedVariablesRoot;
 
    public NodeHandler(UpperBoundedConstarint currentConstraint,   
                          List <String> zeroFixedVariables, List <String> oneFixedVariables){
        this.   currentConstraintRoot=  currentConstraint;
        zeroFixedVariablesRoot=zeroFixedVariables;
        oneFixedVariablesRoot=oneFixedVariables;
    }
    
    protected void main() throws IloException {
        if(getNremainingNodes64()> ZERO){
            
            if (null==getNodeData(ZERO)){
                //root of mip
                NodeAttachment data = new NodeAttachment (zeroFixedVariablesRoot , oneFixedVariablesRoot,  currentConstraintRoot/*, getObjValue(ZERO)*/);
                setNodeData(ZERO,data);                
            }
            
            NodeAttachment attachedData =(NodeAttachment)getNodeData(ZERO);
            
            
            
            if (isRectangular ( attachedData,! USE_STRICT_INEQUALITY)){
                //if original constraint is a strict inequality, then collecting rectangles can collect rectangle whose vertex is on the constraint
                //Otherwise, we cannot collect solutions that are exactly on the constraint
                attachedData.isRectangular=true;
                setNodeData(ZERO,attachedData); 
                
            }
        }
    }
    
  
    //* Checks if , with all free vars set at their worst possible values, constraint is still feasible
    private boolean isRectangular (NodeAttachment attachedData, boolean isStrict){
        return   attachedData.reducedConstraint.isGauranteedFeasible(isStrict);
    }
     
}
