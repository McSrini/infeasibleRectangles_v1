/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex;
 
import static ca.mcmaster.infeasiblerectangles_v1.Constants.*;
import ca.mcmaster.infeasiblerectangles_v1.IR_Driver;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.UpperBoundedConstarint;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.IncumbentCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class IncumbentHandler extends IncumbentCallback{
    
   
    //the root of this tree has these
    private UpperBoundedConstarint currentConstraintRoot;
    private List <String> zeroFixedVariablesRoot  ;
    private List <String> oneFixedVariablesRoot;
    
   
    public IncumbentHandler(UpperBoundedConstarint currentConstraint, List <String> zeroFixedVariables, List <String> oneFixedVariables) {
        this.   currentConstraintRoot=  currentConstraint;
        zeroFixedVariablesRoot=zeroFixedVariables;
        oneFixedVariablesRoot=oneFixedVariables;
        
    }
    
    protected void main() throws IloException {
        
     
              
        if (null==getNodeData()){
            //root of mip
            NodeAttachment data = new NodeAttachment (zeroFixedVariablesRoot , oneFixedVariablesRoot,     currentConstraintRoot/*, getObjValue()*/);
            setNodeData(data);                
        }

        NodeAttachment attachedData =(NodeAttachment)getNodeData();
 
        attachedData.wasSolutionRejected=true;
        setNodeData(attachedData);         
        
        if (attachedData.isRectangular==false) {
            //print rejected non-rect solns
            
        }
        
        //we reject every solution found by CPLEX;
        reject ();
         
    }
 
        
 
    
}
