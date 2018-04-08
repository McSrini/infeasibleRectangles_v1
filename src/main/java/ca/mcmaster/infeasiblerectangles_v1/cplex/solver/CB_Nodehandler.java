/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex.solver;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.ONE;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 */
public class CB_Nodehandler extends IloCplex.NodeCallback {
     

     
    protected void main() throws IloException {
        if(getNremainingNodes64()> ZERO){
            
            if (null==getNodeData(ZERO)) {
                //root, nothing to select
            }else if (((CB_NodeAttachment)getNodeData(ZERO)).startUsingCplexDefaults) {
                //do nothing
            }else {
                
                //we always direct cplex to the node having the best reamining LP relax infeasible rectangle that we collected
                //this is like strict-best-first, in reverse . 
                 
                long selectedIndex= ZERO;
                double bestLPRelax = Double.MAX_VALUE;
/*
                for (long index = ZERO; index<getNremainingNodes64(); index ++ ) {
                    double lpRelaxAtThisNode = ((CB_NodeAttachment)getNodeData(index))  .smallest_LPRelax_among_DescendantInfeasibleRectangles;
                    if ( bestLPRelax> lpRelaxAtThisNode ) {
                        bestLPRelax=lpRelaxAtThisNode;
                        selectedIndex= index;
                    }
                }
*/
                //solve this node next
                selectNode( selectedIndex);
         
            }
            
        }
    }
    
}
