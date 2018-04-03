/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex;
 
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.UpperBoundedConstarint;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.IncumbentCallback;
import java.util.List;

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
        //we reject every solution found by CPLEX;
        reject ();
         
        if (null==getNodeData()){
            //root of mip
            NodeAttachment data = new NodeAttachment (zeroFixedVariablesRoot , oneFixedVariablesRoot,     currentConstraintRoot/*, getObjValue()*/);
            setNodeData(data);                
        }

        NodeAttachment attachedData =(NodeAttachment)getNodeData();
 
        attachedData.wasSolutionRejected=true;
        setNodeData(attachedData);       
         
    }
    
    
}
