/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.analytic.solver;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.*;
import ca.mcmaster.infeasiblerectangles_v1.IR_Driver;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.LowerBoundConstraint;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.TreeNode;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.VariableCoefficientTuple;
import ca.mcmaster.infeasiblerectangles_v1.cplex.solver.BranchingUtils;
import ca.mcmaster.infeasiblerectangles_v1.cplex.solver.Solver;
import ilog.cplex.IloCplex.Status;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * 
 * solve best lp job first, until incumbent cuts off all jobs
 * 
 * job is branched on most shared var among compatible infeasible rectangles
 * 
 * a job with no infeasible compatible rectangles is either fully feasible or infeasible
 * 
 * if no shared vars among compatible infeasible rects, enumerated soln possible
 * 
 */
public class AnalyticSolver {
    
    public   Map<Double, List<TreeNode> > activeLeafs  = new TreeMap<Double,  List<TreeNode> >();
    
    public double incumbent = Double.MAX_VALUE;
    
    public   Map<Double, List<Rectangle> > solutionPool  = new TreeMap<Double,  List<Rectangle> >();
    
    private static Logger logger=Logger.getLogger(AnalyticSolver.class);    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+AnalyticSolver.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
        
        
    }
    
    public AnalyticSolver ( Map<Double, List<Rectangle> > infeasibleRectangle_Cumulative_Collection) {
        List<TreeNode>  nodes = new ArrayList<TreeNode> () ;
        nodes.add( new TreeNode()) ;
        nodes.get(ZERO).myCompatibleRectangles=       infeasibleRectangle_Cumulative_Collection;
        activeLeafs.put (/*any value*/ DOUBLE_ZERO, nodes ) ;
        
    }
    
    
    public Status getStatus (){
        Status status= Status.Infeasible;
        if (this.solutionPool.size()>ZERO){
            status = Status.Feasible;
            if (!activeLeafs.isEmpty() ){
                if ( Collections.min(this.activeLeafs.keySet())>=incumbent)      status = Status.Optimal;
            }else {
                status = Status.Optimal;
            }
        }
        return status;
    }
    
    public void solve () {
        
        while (this.activeLeafs.size()>ZERO && getBestBound()< incumbent){
            
            //printAllLeafs() ;
            
            logger.debug(" \n\nbest bound is "+ getBestBound() + " incumbent is " +incumbent + " numleafs is "+ getNumberOfLeafs(this.activeLeafs));
            
            //get next node
            double bestBound = getBestBound();
            List<TreeNode> bestNodes = this.activeLeafs.get( bestBound);
            TreeNode selectedNode =bestNodes.remove(ZERO);
            if (bestNodes.size()==ZERO) {
                this.activeLeafs.remove( bestBound);
            }else {
                this.activeLeafs.put(bestBound, bestNodes);
            }
            
            logger.debug("Selected node is "+ selectedNode);
            
            //printInfeasibleRectangles(selectedNode) ;
            
            //if best node selected is feasible or infeasible, add to soln pool and update incumbent, or discard
            if (selectedNode.myCompatibleRectangles.size() ==ZERO &&
                    isFullyFeasible(selectedNode.zeroFixedVariables, selectedNode.oneFixedVariables)) {
                addSolutionToPool(selectedNode) ;
                logger.debug ("this is a  solution node with lp relax "+ selectedNode.lpRelaxValueMinimization) ;
            }else if (selectedNode.myCompatibleRectangles.size() ==ZERO &&
                    !isFullyFeasible(selectedNode.zeroFixedVariables, selectedNode.oneFixedVariables)) {
                //discard infeasible node
                logger.debug ("discard node "+ selectedNode) ;
            }else {
                //find branching var
                VariableCoefficientTuple branchingVar = this.getBestBranchingVar(selectedNode);
                
                if (branchingVar.coeff==ONE) {
                    //we can get enumerated soln for this node
                    List<Rectangle> infeasibleRects = new ArrayList<Rectangle> () ;
                    for (List<Rectangle> rects:selectedNode.myCompatibleRectangles.values()){
                        infeasibleRects.addAll(rects) ;
                    }
                    
                    Enumerator enumerator = new Enumerator (  selectedNode.zeroFixedVariables , selectedNode.oneFixedVariables);
                    Rectangle soln = enumerator.getEnumeratedSolution(infeasibleRects);
                    addSolutionToPool(soln);
                    logger.debug ("found solution by enumeration "+ soln) ;
                }else {
                    //create two child nodes, each with its own list of compatible infeasible rects
                    TreeNode zeroChild = null;
                    TreeNode oneChild = null;
                    
                    //get the descendant rectangles for each child
                    BranchingUtils utils = new BranchingUtils( );                     
                    logger.warn("start split") ;
                    logger.warn("Size before split "+ getNumberOfRects(selectedNode.myCompatibleRectangles));
                    utils.split(selectedNode.myCompatibleRectangles ,  branchingVar.varName,
                            selectedNode.zeroFixedVariables,
                            selectedNode.oneFixedVariables);
                    logger.warn("Size after 0 split "+ getNumberOfRects(utils.rectangle_Compatible_With_Zero_Side));
                    logger.warn("Size after 1 split "+ getNumberOfRects(utils.rectangle_Compatible_With_One_Side));
                    logger.warn("end  split") ;
                     
                    List <String> newZeroFixedVariables = new ArrayList<String> () ;
                    List <String>  newOneFixedVariables = new ArrayList<String> () ;
                    newZeroFixedVariables.addAll(selectedNode.zeroFixedVariables) ;
                    newZeroFixedVariables.add(branchingVar.varName) ;
                    newOneFixedVariables.add(branchingVar.varName );
                    newOneFixedVariables.addAll(selectedNode.oneFixedVariables) ;

                    logger.warn("branch on this var " +branchingVar.varName + " having refcount " + branchingVar.coeff) ;
                     
                    zeroChild=new TreeNode (  newZeroFixedVariables ,  selectedNode.oneFixedVariables ,   utils.rectangle_Compatible_With_Zero_Side);
                    oneChild= new TreeNode ( selectedNode.zeroFixedVariables  ,  newOneFixedVariables , utils.rectangle_Compatible_With_One_Side) ;

                    //add these two nodes back into active leafs
                    this.addActiveLeaf(zeroChild);
                    this.addActiveLeaf(oneChild);
                    
                } //branch var coeff ==1
            } //selectedNode.myCompatibleRectangles.size() ==ZERO &&             
        }//end while
        
        Status status =getStatus ();
        if (status.equals(Status.Infeasible) ) {
            System.out.println("MIP is infeasible") ;            
        }else {
            System.out.println("MIP is optimal " + incumbent + " \nprinting solution pool\n") ;            
            for (List<Rectangle> solns : this.solutionPool.values()) {
                for (Rectangle soln : solns){
                    System.out.println(soln) ;    
                }                
            }
        }
        
    }
    
    public double getBestBound () {
        return Collections.min(this.activeLeafs.keySet()) ;
    }
    
    //a node with no compatible infeasibl erects under it is either fully feasible or fullt infeasible
    public boolean isFullyFeasible (List <String> zeroFixedVariables, List <String> oneFixedVariables) {
        boolean result = true;
        for (LowerBoundConstraint lbc : IR_Driver.mipConstraintList){
            LowerBoundConstraint reduced = lbc.getReducedConstraint(zeroFixedVariables, oneFixedVariables);
            if ( !reduced.isGauranteedFeasible(false)){
                //
                result = false;
                break;
            }         
        }
        return result ;
    }
     
    

    
    private VariableCoefficientTuple getBestBranchingVar (TreeNode node) {
        BranchingUtils utils = new BranchingUtils( );
         
        logger.warn ("getBestChoiceBranchingVariable start") ;
        VariableCoefficientTuple bestBranchVar = utils.getBestChoiceBranchingVariable ( node.myCompatibleRectangles,    node.zeroFixedVariables, node.oneFixedVariables); 
        if (bestBranchVar.varName!=null) {
            logger.warn (" Best Choice Branching Variable  is "+ bestBranchVar.varName) ;
        }else {
            logger.error (" Could not find Branching Variable    ") ;
        }
        return bestBranchVar;
    }
    
    private void addSolutionToPool(Rectangle selectedNode) {
        incumbent = Math.min(incumbent, selectedNode.getLpRelaxValueMinimization()) ;
        List<Rectangle>  solnList = this.solutionPool.get( selectedNode.lpRelaxValueMinimization);
        if (solnList==null) solnList = new ArrayList<Rectangle> ();
        solnList.add(selectedNode);
        this.solutionPool.put(selectedNode.lpRelaxValueMinimization , solnList);
    }
    
    private void addActiveLeaf( TreeNode new_Node) {
        List<TreeNode>  nodeList = this.activeLeafs.get( new_Node.getLpRelaxValueMinimization());
        if (nodeList==null) nodeList = new ArrayList<TreeNode> ();
        nodeList.add(new_Node);
        this.activeLeafs.put( new_Node.lpRelaxValueMinimization , nodeList);
    }
    
    private static int getNumberOfRects ( Map<Double, List<Rectangle> > map2){
        int count = ZERO;
        for (List<Rectangle> rectlist : map2.values()){
            count +=rectlist.size();
        }
        return count;
    }
    private static int getNumberOfLeafs ( Map<Double, List<TreeNode> > map2){
        int count = ZERO;
        for (List<TreeNode> rectlist : map2.values()){
            count +=rectlist.size();
        }
        return count;
    }
    
    private void printAllLeafs () {
        logger.debug("");
        for (List<TreeNode> nodes : this.activeLeafs.values()) {
            for (TreeNode node: nodes){
                logger.debug(node);
            }
            
        }
        logger.debug("");
        
    }
    
    private void  printInfeasibleRectangles(TreeNode selectedNode) {
         logger.debug("");
         for (List<Rectangle> nodes : selectedNode.myCompatibleRectangles.values()) {
             for (Rectangle node : nodes ){
                 logger.debug(node);
             }
         }
         logger.debug("");
    }
    
}
