/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.*;
import ca.mcmaster.infeasiblerectangles_v1.IR_Driver;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.MIP_FILENAME;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.USE_STRICT_INEQUALITY;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.*;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle; 
import static java.lang.System.exit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class BranchHandler  extends IloCplex.BranchCallback {
    
       
    //the root of this tree has these
    //
    //the constraint that is bothering this root
    private UpperBoundedConstarint currentConstraintRoot;
   
    //var fixing at root of this tree
    private List <String> zeroFixedVariablesRoot  ;
    private List <String> oneFixedVariablesRoot;
    
    //note down vars in the model, key by name
    private Map<String, IloNumVar> modelVars;
    
    private static Logger logger=Logger.getLogger(BranchHandler.class);
    
    static {
        logger.setLevel(Level.ERROR);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+BranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
        
        
    }
    
    
    public  BranchHandler(Map<String, IloNumVar> modelVars, UpperBoundedConstarint currentConstraint,   
                           List <String> zeroFixedVariables, List <String> oneFixedVariables){
        this. modelVars= modelVars;
        this.currentConstraintRoot =  currentConstraint;
        zeroFixedVariablesRoot=zeroFixedVariables;
        oneFixedVariablesRoot=oneFixedVariables;
         
    }
       
    
    protected void main() throws IloException {
        //
        //if rectangular region, collect and prune
        //else if solution rejected, branch on var with lowest coeff in reduced constraint
        // always create and attach a new attachment into each child
        
        if ( getNbranches()> ZERO ){  
            
            //double lpRelax = getObjValue() ;
            
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            if (null==getNodeData()){
                //root of mip
                NodeAttachment data = new NodeAttachment (zeroFixedVariablesRoot , oneFixedVariablesRoot,   currentConstraintRoot );
                setNodeData(data);                
            } 
            
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            
            //debug
            /*
            String zeroFixedvars = "";
            String oneFixedVars = "";
            for (String var : nodeData.zeroFixedVariables){
                zeroFixedvars+=var;
            }
            for (String var : nodeData.oneFixedVariables){
                oneFixedVars+=var;
            }
            logger.error( "This node zero fixed vars " +zeroFixedvars);
            logger.error( "This node one  fixed vars " +oneFixedVars);
            
            
                   */          
            
            //end debug
                
            if (nodeData.isRectangular){
                 //collect and prune
                    Rectangle rectangle = new Rectangle (nodeData.zeroFixedVariables, nodeData.oneFixedVariables);      

                    double lp   = rectangle.getLpRelaxValueMinimization();
                    List<Rectangle> rectHavingThisDepth = IR_Driver.rectangleBuffer.get(lp);
                    if (rectHavingThisDepth==null) rectHavingThisDepth = new ArrayList<Rectangle>();
                    rectHavingThisDepth.add(rectangle);
                    IR_Driver.rectangleBuffer.put (lp, rectHavingThisDepth);

                    //discard this leaf node - we have collected the rectangle
                    prune() ; 
            } else if (nodeData.wasSolutionRejected){
                
                Rectangle solutionPoint = new Rectangle (getFixedVarsAtSolutionPoint(ZERO),    getFixedVarsAtSolutionPoint(ONE));   
                
                
                
                
                //if some free vars left, we branch on one of them, else
                //we collect the point solution that was rejected and prune this nude ( pruning not needed I think)
                
                //decide bracnhing var - it will the highest free coeff remaining in the constraint
                int reducedConstraintSize = nodeData.reducedConstraint.sortedConstraintExpr.size();
                
                //if there is any free variable, then branch on the best such var
                if (reducedConstraintSize > ZERO) {
                    String branchingVar = nodeData.reducedConstraint.sortedConstraintExpr.get(reducedConstraintSize-ONE).varName;                
                    //branch on var chosen above and propogate node data to child nodes
                    branch ( branchingVar,   nodeData);
                } else {
                    //all vars fixed already, can happen if there is only 1 solution and it at a triangle corner
                    //collect point solution unless it is on original constrain plane and strict is ON
                    //collect non-rect solutions as points

                    boolean solutionLiesExactlyOnOriginalConstraintPlane = 
                                doesSolutionLieExactlyOnOriginalConstraintPlane(solutionPoint.zeroFixedVariables, solutionPoint.oneFixedVariables);

                    if (!USE_STRICT_INEQUALITY && solutionLiesExactlyOnOriginalConstraintPlane) {
                        //do not collect    
                        logger.error("Solution point rejected "+ solutionPoint) ;                    
                    }else {
                        logger.error("Solution point collected "+ solutionPoint) ;
                        //collect the point
                        double lp   = solutionPoint.getLpRelaxValueMinimization();
                        List<Rectangle> rectHavingThisDepth = IR_Driver.rectangleBuffer.get(lp);
                        if (rectHavingThisDepth==null) rectHavingThisDepth = new ArrayList<Rectangle>();
                        rectHavingThisDepth.add(solutionPoint);
                        IR_Driver.rectangleBuffer.put (lp, rectHavingThisDepth);
                    }   
                    
                    prune();
                }
                
            } else {
                //branch on default var and propogate node data to child nodes
                branch ( null,    nodeData);
            }
        }//end else if branches >0
            
    }//emd main
    
       
    private boolean doesSolutionLieExactlyOnOriginalConstraintPlane(List <String> zeroFixedVariables , List <String> oneFixedVariables ){
        UpperBoundedConstarint reducedConstarint =  currentConstraintRoot.getReducedConstraint(zeroFixedVariables ,  oneFixedVariables  );
        return reducedConstarint.upperBound==ZERO;
    }
    
    private List<String> getFixedVarsAtSolutionPoint(int fixed) throws IloException {
        List<String> result = new ArrayList<String> ();
        for (IloNumVar var : this.modelVars.values()){
            if (Math.round(getValue(var)) ==fixed) {
                result.add(var.getName());
            }
        }
        return result;
    }
    
    private void branch (String branchingVar,  NodeAttachment nodeData) throws IloException{
        
        // branches about to be created
        IloNumVar[][] vars = new IloNumVar[TWO][] ;
        double[ ][] bounds = new double[TWO ][];
        IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
        
        if (null==branchingVar){
                //use cplex's branching var decision
                //we assume only 1 var branch, since we use traditional branch and bound
                getBranches( vars, bounds,  dirs) ;
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
           
            UpperBoundedConstarint childConstraint =null;
            if (isZeroFix){
                zeroFixedVariables.add(branchingVarName);
                childConstraint = nodeData.reducedConstraint.getReducedConstraint(zeroFixedVariables, new ArrayList<String>() );
            }else{
                oneFixedVariables.add(branchingVarName);
                childConstraint = nodeData.reducedConstraint.getReducedConstraint(new ArrayList<String>() , oneFixedVariables);
            }

            zeroFixedVariables.addAll(nodeData.zeroFixedVariables  );
            oneFixedVariables.addAll(nodeData.oneFixedVariables );
            
            NodeAttachment thisChild  =  new NodeAttachment (zeroFixedVariables, oneFixedVariables  , childConstraint); 
            makeBranch( vars[childNum],  bounds[childNum],dirs[childNum], getObjValue(), thisChild);
        }
        
    }
    
}
