/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex.solver;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.LOG_FOLDER;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ONE;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.TWO;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import ca.mcmaster.infeasiblerectangles_v1.IR_Driver;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * branch on variable from node attachment if available, else use cplex default
 * 
 */
public class CB_BranchHandler extends IloCplex.BranchCallback {
    
    public long customBranchingDecisions = ZERO;
    public long cplexDefualtBranchDecisions = ZERO;
        
   
    //note down vars in the model, key by name
    private Map<String, IloNumVar> modelVars;
     
    
    private static Logger logger=Logger.getLogger(CB_BranchHandler.class);
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+CB_BranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
        
        
    }
     
    public  CB_BranchHandler(Map<String, IloNumVar> modelVars){
         this.modelVars=modelVars;
    }
        
    //public  CB_BranchHandler(Map<String, IloNumVar> modelVars,   List <String> _zeroFixedVariables, List <String> _oneFixedVariables){
        //this. modelVars= modelVars;
        //zeroFixedVariables=zeroFixedVariables;
        //oneFixedVariables=oneFixedVariables;
         
    //}
 
    protected void main() throws IloException {
        if ( getNbranches()> ZERO                                      ){  
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            if (null==getNodeData()){
                //root of mip                                
                List <String> branchingOrder = getBranchingOrder (new ArrayList<String>(), new ArrayList<String>());                 
                CB_NodeAttachment data = new CB_NodeAttachment (  new ArrayList<String>(), new ArrayList<String>(),    branchingOrder );
                setNodeData(data);                
            } 
            
            CB_NodeAttachment nodeData = (CB_NodeAttachment) getNodeData();
            
            //if node data has no branching variable recommendations, try to refresh those 
            if (nodeData.branchingVariablesInPriorityOrder.size()==ZERO){
                List <String> branchingOrder =getBranchingOrder( nodeData.zeroFixedVariables,  nodeData.oneFixedVariables );
                nodeData.branchingVariablesInPriorityOrder.addAll(branchingOrder);
            }
            
            //if we have bracnhing suggestion, use it
            //else use cplex default
            //send forward node attachment in any case
            String branchingVar = nodeData.branchingVariablesInPriorityOrder.size()>ZERO ? nodeData.branchingVariablesInPriorityOrder.get(ZERO) : null;
             
            branch ( branchingVar,    nodeData);
            
        }//end else if branches >0
    }
    
    private List <String> getBranchingOrder(List<String> zeroFixes,  List<String> oneFixes ){
        
        BranchingVariableFinder branchingVariableFinder = new BranchingVariableFinder (IR_Driver.infeasibleRectanglesCumulative, 
                                                                                               new ArrayList<String> (this.modelVars.keySet())) ;
        
        return  branchingVariableFinder.getBranchingVariablesInPriorityOrder(zeroFixes, oneFixes);
    }
    
    private void branch (String branchingVar,   CB_NodeAttachment nodeData  ) throws IloException{
        
        // branches about to be created
        IloNumVar[][] vars = new IloNumVar[TWO][] ;
        double[ ][] bounds = new double[TWO ][];
        IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
        
        if (null==branchingVar){
                //use cplex's branching var decision
                //we assume only 1 var branch, since we use traditional branch and bound
                getBranches( vars, bounds,  dirs) ;
                
                cplexDefualtBranchDecisions ++;
                
        }else {
            //get var with given name, and create up and down branch conditions
            vars[ZERO] = new IloNumVar[ONE];
            vars[ZERO][ZERO]= this.modelVars.get(branchingVar );
            bounds[ZERO]=new double[ONE ];
            bounds[ZERO][ZERO]=ZERO;
            dirs[ZERO]= new IloCplex.BranchDirection[ONE];
            dirs[ZERO][ZERO]=IloCplex.BranchDirection.Down;
            
            vars[ONE] = new IloNumVar[ONE];
            vars[ONE][ZERO]= this.modelVars.get(branchingVar );
            bounds[ONE]=new double[ONE ];
            bounds[ONE][ZERO]=ONE;
            dirs[ONE]= new IloCplex.BranchDirection[ONE];
            dirs[ONE][ZERO]=IloCplex.BranchDirection.Up;
            
            customBranchingDecisions ++;
        }
               
        //debug print
        logger.debug("\n\nBranching on " +vars[ZERO][ZERO].getName());
        for (  String str :nodeData.zeroFixedVariables ){
                logger.debug("zero var are " +str);
            }
            for (  String str :nodeData.oneFixedVariables ){
                logger.debug("one var are " +str);
        }
              
        //debug print
        
        for (int childNum = ZERO ;childNum<TWO;  childNum++) {  
            String branchingVarName = vars[childNum][ZERO].getName();
            boolean isZeroFix = Math.round (bounds[childNum][ZERO]) == ZERO; // zero bound indicates binary var is fixed at 0
            

            List <String> zeroFixedVariables = new ArrayList<String> () ;
            List <String>  oneFixedVariables = new ArrayList<String> () ;
            
            if (isZeroFix){
                zeroFixedVariables.add(branchingVarName);
                 
            }else{
                oneFixedVariables.add(branchingVarName);
                 
            }

            zeroFixedVariables.addAll(nodeData.zeroFixedVariables  );
            oneFixedVariables.addAll(nodeData.oneFixedVariables );
            
            int size = nodeData.branchingVariablesInPriorityOrder.size();
            List<String>  branchingVariableRecommendationsForChildNode = new ArrayList<String> ();
            if (size>ONE)  branchingVariableRecommendationsForChildNode = nodeData.branchingVariablesInPriorityOrder.subList(ONE, size);
            CB_NodeAttachment thisChild  =  new CB_NodeAttachment (zeroFixedVariables, oneFixedVariables  , branchingVariableRecommendationsForChildNode); 
            makeBranch( vars[childNum],  bounds[childNum],dirs[childNum], getObjValue(), thisChild);
        }
        
    }
    
    
}
