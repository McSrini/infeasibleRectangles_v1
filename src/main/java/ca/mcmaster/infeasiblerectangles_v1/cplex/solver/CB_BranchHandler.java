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
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
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
    
    //these two for statistics
    public long customBranchingDecisions = ZERO;
    public long cplexDefualtBranchDecisions = ZERO;
    
    //note down vars in the model, key by name
    private Map<String, IloNumVar> modelVars; 
    private static Logger logger=Logger.getLogger(CB_BranchHandler.class);
    
    static {
        logger.setLevel(Level.WARN);
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
 
    protected void main() throws IloException {
        if ( getNbranches()> ZERO                                      ){  
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            if (null==getNodeData()){
                //root of mip                                                              
                CB_NodeAttachment data = new CB_NodeAttachment (  new ArrayList<String>(), new ArrayList<String>() ,
                          IR_Driver.infeasibleRectangle_Cumulative_Collection);
                setNodeData(data);                
            } 
            
            CB_NodeAttachment nodeData = (CB_NodeAttachment) getNodeData();
            
            //if node data has no branching variable recommendations, try to refresh those 
            /*if (nodeData.branchingVariablesInPriorityOrder.size()==ZERO){
                logger.debug("calculating best branching order ... " );
                List <String> branchingOrder =getBranchingOrder( nodeData.zeroFixedVariables,  nodeData.oneFixedVariables );
                nodeData.branchingVariablesInPriorityOrder.addAll(branchingOrder);
                if (branchingOrder.size()>ZERO) {
                    logger.info(" branching order best var is "+ branchingOrder.get(ZERO) + " and list size "+branchingOrder.size());
                } else {
                    logger.info("no branching recommendation, will use cplex default. There should not be many such lines in the log !") ;
                }
            }*/
            
            BranchingUtils utils = new BranchingUtils( );
            String branchingVar =  null;
            if ( !nodeData.startUsingCplexDefaults ) {
                logger.warn ("getBestChoiceBranchingVariable start") ;
                String bestBranchVar = utils.getBestChoiceBranchingVariable (  nodeData.myCompatibleRectangles, 
                                          nodeData.zeroFixedVariables, nodeData.oneFixedVariables).varName; 
                if (bestBranchVar!=null) {
                    logger.warn (" Best Choice Branching Variable  is "+ bestBranchVar) ;
                }else {
                    logger.error (" Could not find Branching Variable    ") ;
                }
            }
             
                                        
            
             
            branch ( branchingVar,    nodeData);
            
        }//end else if branches >0
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
              
        //get the descendant rectangles for each child
        BranchingUtils utils = new BranchingUtils( );
        if (!nodeData.startUsingCplexDefaults){
            logger.warn("start split") ;
            logger.warn("Size before split "+ getNumberOfRects(nodeData.myCompatibleRectangles));
            utils.split(nodeData.myCompatibleRectangles , vars[ZERO][ZERO].getName(), nodeData.zeroFixedVariables, nodeData.oneFixedVariables);
            logger.warn("Size after 0 split "+ getNumberOfRects(utils.rectangle_Compatible_With_Zero_Side));
            logger.warn("Size after 1 split "+ getNumberOfRects(utils.rectangle_Compatible_With_One_Side));
            logger.warn("end  split") ;
        }
        
        
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
             
            
            CB_NodeAttachment thisChild  =  new CB_NodeAttachment(true);                    
            if (!nodeData.startUsingCplexDefaults) {
                if (isZeroFix) {
                    if (utils.rectangle_Compatible_With_Zero_Side .size()>ZERO)    thisChild =  
                            new CB_NodeAttachment (zeroFixedVariables, oneFixedVariables  ,      utils.rectangle_Compatible_With_Zero_Side  );
                }else {
                    if (utils.rectangle_Compatible_With_One_Side  .size()>ZERO)    thisChild =  
                            new CB_NodeAttachment (zeroFixedVariables, oneFixedVariables  ,  utils.rectangle_Compatible_With_One_Side);
                }                
            } 
            makeBranch( vars[childNum],  bounds[childNum],dirs[childNum], getObjValue(), thisChild);
        }
        
    }
            
    private static int getNumberOfRects ( Map<Double, List<Rectangle> > map2){
        int count = ZERO;
        for (List<Rectangle> rectlist : map2.values()){
            count +=rectlist.size();
        }
        return count;
    }
 
}
