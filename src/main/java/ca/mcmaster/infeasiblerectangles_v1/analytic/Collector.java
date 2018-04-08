/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.analytic;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.LOG_FOLDER;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ONE;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.USE_STRICT_INEQUALITY;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.UpperBoundedConstarint;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.VariableCoefficientTuple;
import ca.mcmaster.infeasiblerectangles_v1.cplex.BranchHandler;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * collect feasible rectangles using analytical method
 * 
 * maximization mip with 1 UB constraint
 *  
 * first , find the origin, if origin itself is infeasible discard this node
 * 
 * grow lowest coeff var in constr first, till no more growth possible. Record count of free vars
 * if freevar count == 0 , node is feasible with its current branching conditions
 * if freevarcount == all free vars,, node is feasible with its current branching conditions
 * 
 * branch on fixed vars to create a feasible node, and create other nodes which need further decomposition. Branch on highest coeff var in constraint first
 * 
 * reapeat collection procedure on best remaining LP relax node, lp relax being minimization lp relax ( i..e lp relax at origin).
 * 
 */
public class Collector {
    
    public   Map<Double, List<Rectangle>  > collectedFeasibleRectangles = new TreeMap <Double, List<Rectangle>>  ();    
    
    private UpperBoundedConstarint ubc ;
    private    Map<Double, List<Rectangle>  > pendingJobs = new TreeMap <Double, List<Rectangle>>  ();    
    
    private static Logger logger=Logger.getLogger(Collector.class);
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+Collector.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
        
        
    }
    
    public Collector (UpperBoundedConstarint ubc){
        this.ubc = ubc;
        Rectangle mip = new Rectangle (new ArrayList<String> (),new ArrayList<String> ()) ;
        List<Rectangle> jobList = new ArrayList<Rectangle>();
        jobList.add(mip);
        pendingJobs.put(mip.getLpRelaxValueMinimization(), jobList);
        logger.debug( "constraint  for collection is " +ubc );
    }
    
    public void collect(){
        //remove the best pending node and decompose it, until pendin glist is empty
        while (!this.pendingJobs.isEmpty()) {
            
           //printAllJobs();
           
           double bestLPRelax = Collections.min( pendingJobs.keySet());
           List<Rectangle> bestLPJobs = pendingJobs.get(bestLPRelax);
           Rectangle job = bestLPJobs.remove(ZERO);
           if (bestLPJobs.size()==ZERO) {
               pendingJobs.remove(bestLPRelax);
           }else {
               pendingJobs.put(bestLPRelax, bestLPJobs);
           }
           decompose(job);
           
        }
    }
    
    private void printAllJobs() {
        for (List<Rectangle> jobList : this.pendingJobs.values()){
            for (Rectangle  job: jobList){
                logger.debug(job);
            }
        }
    }
    
    //decompose a leaf node and add feasible node found into collection, and more jobs into joblist
    private void decompose (Rectangle leafNode) {
        
        logger.debug("decomposing job "+ leafNode);
        
        //first find the origin
        //
        //find the redcued constraint
        //there are better ways of finding the reduced constraint, but thius is simple 
        UpperBoundedConstarint reducedConstraint = ubc.getReducedConstraint(leafNode.zeroFixedVariables, leafNode.oneFixedVariables);
        //here is the list of values of vars in the reduced constraint , fixed at  0 or 1 at the origin. 
        List<Boolean> isVariableFixedAtZeroAtOrigin = new ArrayList<Boolean>();   
        
        //if reduced constraint is trivially feasible or unfeasible, no need for dempostion
        if (reducedConstraint.isTrivially_Feasible( !USE_STRICT_INEQUALITY) || reducedConstraint.isGauranteedFeasible(!USE_STRICT_INEQUALITY)) {
            this.addLeafToFeasibleCollection(leafNode);
            logger.debug( "Collected whole feasible rect "+leafNode);
        }else if (reducedConstraint.isTrivially_Infeasible( !USE_STRICT_INEQUALITY) || reducedConstraint.isGauranteed_INFeasible(!USE_STRICT_INEQUALITY) ){
            //discard
            logger.debug("discard infeasible rect " + leafNode);
        }else {
            //must decompose
            //
            //find the origin for this constraint
            double constraintValueAtOrigin = ZERO;
            for (VariableCoefficientTuple tuple: reducedConstraint.sortedConstraintExpr){

                //note that reduced constraint is only composed of free variables

                //we choose its value to minimize origin value , since we want the origin
                if (tuple.coeff<ZERO ) {
                    constraintValueAtOrigin+=tuple.coeff;
                    isVariableFixedAtZeroAtOrigin.add( false);
                } else {
                    isVariableFixedAtZeroAtOrigin.add( true);
                    //no contribution to origin value
                }            
            }
            
            //for each variable in the constraint, starting with the lowest coeff, see if flipping its value from its origin value will render constraint infeasible
            //continue till we hit infeasibility
            //delta is how much we move from the origin by flipping this variable 
            double delta =ZERO;
            int countOfVarsWhichCanBeFree = ZERO ;
            ///logger.debug ("reduced constr is " +reducedConstraint.toString()) ;
            for (VariableCoefficientTuple tuple: reducedConstraint.sortedConstraintExpr){
                //System.out.println("tuple "+ tuple.varName);
                delta += Math.abs(tuple.coeff);
                boolean isConstraintViolated = !USE_STRICT_INEQUALITY ? 
                        (delta +constraintValueAtOrigin>= reducedConstraint.upperBound): 
                        (delta +constraintValueAtOrigin> reducedConstraint.upperBound);
                if ( isConstraintViolated) {
                    //growth no longer possible
                    break;
                } else {
                    countOfVarsWhichCanBeFree ++;
                    logger.debug("Free var " + tuple.varName) ;
                    //System.out.println("tuple added "+ tuple.var.name);
                }
            }
            
            //now we know the vars which can be free. Note that all vars cannot be free, because entire rectangle feasible has been taken care of
            Rectangle feasibleLeaf = createFeasibleNode (leafNode, isVariableFixedAtZeroAtOrigin,countOfVarsWhichCanBeFree ,reducedConstraint) ;
            this.addLeafToFeasibleCollection(feasibleLeaf );
            
            List<Rectangle> newJobs = null;
            if ( countOfVarsWhichCanBeFree!=ZERO) {
                newJobs = createMoreNodesForDecompostion (leafNode, isVariableFixedAtZeroAtOrigin,countOfVarsWhichCanBeFree ,reducedConstraint) ;
                this.addPendingJobs(newJobs);  
            }
                        
            logger.debug( "Collected feasible rect "+feasibleLeaf);
            
        }//end if-else must decompose        
        
    }//end method decompose
    
    private List<Rectangle> createMoreNodesForDecompostion (Rectangle leafNode, List<Boolean> isVariableFixedAtZeroAtOrigin,
                                                          int countOfVarsWhichCanBeFree,  UpperBoundedConstarint reducedConstraint ) {
        
        List<Rectangle> newJobs = new ArrayList<Rectangle>();
        
        //starting with the highest coeff var in the reduced constraint, flip var value from origin, and for all higher coeff vars retain their value at origin
        //# of jobs created = (# of freevars in reduced constr - countOfVarsWhichCanBeFree)
        for (int jobIndex = ZERO; jobIndex< reducedConstraint.sortedConstraintExpr.size()- countOfVarsWhichCanBeFree;jobIndex++){
            Rectangle  newJob = new Rectangle(leafNode.zeroFixedVariables, leafNode.oneFixedVariables) ;
            
            //add flipped  branching condition for jth largest var
            int size = reducedConstraint.sortedConstraintExpr.size();
            if (isVariableFixedAtZeroAtOrigin.get(size-ONE-jobIndex)){
                newJob.oneFixedVariables.add(  reducedConstraint.sortedConstraintExpr.get(size-ONE-jobIndex).varName);
            }else {
                newJob.zeroFixedVariables.add(  reducedConstraint.sortedConstraintExpr.get(size-ONE-jobIndex).varName);
            }
            
            //for all the other higher coeff vars, add branching conditions by using their value at origin
            int numberOfHigherCoeffVars = jobIndex;
            for (; numberOfHigherCoeffVars >ZERO; numberOfHigherCoeffVars--){
                if (isVariableFixedAtZeroAtOrigin.get(size-numberOfHigherCoeffVars)) {
                    newJob.zeroFixedVariables.add(reducedConstraint.sortedConstraintExpr.get(size-numberOfHigherCoeffVars).varName);
                }else {
                    newJob.oneFixedVariables.add( reducedConstraint.sortedConstraintExpr.get(size-numberOfHigherCoeffVars).varName);
                }
            }
                    
            newJobs.add(newJob );
        }
        
        return newJobs ;
    }
    
    private Rectangle createFeasibleNode (Rectangle leafNode, List<Boolean> isVariableFixedAtZeroAtOrigin,int countOfVarsWhichCanBeFree,   
                                          UpperBoundedConstarint reducedConstraint  ) {
        //create a branch using vars which are not free, and fix their values to their vale at origin
        Rectangle result = new Rectangle (leafNode.zeroFixedVariables, leafNode.oneFixedVariables) ;
        for (int index = countOfVarsWhichCanBeFree; index < isVariableFixedAtZeroAtOrigin.size(); index ++) {
            //get var value at origin, and fix it at that value
            if (isVariableFixedAtZeroAtOrigin.get(index)){
                result.zeroFixedVariables.add(  reducedConstraint.sortedConstraintExpr.get(index).varName);
            }else {
                result.oneFixedVariables.add(  reducedConstraint.sortedConstraintExpr.get(index).varName);
            }
        }
        return result ;
    }
    
    private void addLeafToFeasibleCollection (Rectangle leafNode){
        List<Rectangle> rects= collectedFeasibleRectangles.get( leafNode.getLpRelaxValueMinimization());
        if (rects==null) rects =new ArrayList<Rectangle> ();
        rects.add(leafNode) ;
        collectedFeasibleRectangles.put( leafNode.lpRelaxValueMinimization, rects);
    }
    
    private void addPendingJobs (List<Rectangle> jobs) {
        for (Rectangle job : jobs){
            addPendingJob(job);
        }
    }
    
    private void addPendingJob (Rectangle job) {
        List<Rectangle> rects= this.pendingJobs.get(job.getLpRelaxValueMinimization());
        if (rects==null) rects =new ArrayList<Rectangle> ();
        rects.add(job) ;
        this.pendingJobs.put( job.lpRelaxValueMinimization, rects);
    }
    
}
