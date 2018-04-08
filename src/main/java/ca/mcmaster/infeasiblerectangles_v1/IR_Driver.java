/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.*;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.MIP_FILENAME;
import ca.mcmaster.infeasiblerectangles_v1.analytic.Collector;
import ca.mcmaster.infeasiblerectangles_v1.analytic.Tester;
import ca.mcmaster.infeasiblerectangles_v1.analytic.solver.AnalyticSolver;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.*;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle; 
import ca.mcmaster.infeasiblerectangles_v1.cplex.MIP_Creator; 
import ca.mcmaster.infeasiblerectangles_v1.cplex.solver.Solver;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloObjectiveSense;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;
import java.io.File;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.*;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class IR_Driver {
        
    //constraints in this mip
    public static List<LowerBoundConstraint> mipConstraintList ;
    public static Objective objective ;
    
    //rectangles collected by the branch callback for 1 constraint 
    public static Map<Double, List<Rectangle>  > rectangleBuffer = new TreeMap <Double, List<Rectangle>>  ();    
    //cumulative with all rects collected
    public static Map<Double, List<Rectangle> > infeasibleRectangle_Cumulative_Collection  = new TreeMap<Double,  List<Rectangle> >();
      
    private static Logger logger=Logger.getLogger(IR_Driver.class);
    
    
    
    
    public static void main(String[] args) throws Exception {
                
        if (! isLogFolderEmpty()) {
            System.err.println("\n\n\nClear the log folder before starting " + LOG_FOLDER);
            //exit(ONE);
        }
            
        logger=Logger.getLogger(IR_Driver.class);
        logger.setLevel(Level.DEBUG);
        
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+IR_Driver.class.getSimpleName()+ LOG_FILE_EXTENSION);
           
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
        
        logger.debug ("Start  solve " +MIP_FILENAME ) ;
        
        //assemble constraints in model
        IloCplex mip =  new IloCplex();
        mip.importModel(MIP_FILENAME);
        mipConstraintList= getConstraints(mip);
        objective= getObjective(mip);
        
        System.out.println ("Starting rectangle collection ... " + MIP_FILENAME) ;
        //solve for each constraint, i.e. collect rectangles for each constraint into infeasibleRectanglesByConstraint
        
        /*for (int index = ZERO ; index <mipConstraintList.size() && ! isHaltFilePresent (); index ++ ) {
            MIP_Creator  mipWithOneConstraint = 
                    new MIP_Creator (   mipConstraintList.get(index)   );
           
            
            //solve this mip to collect all its rectangles
            mipWithOneConstraint.cplex.solve();
            //move all the rectangles into the variable infeasibleRectanglesByConstraint
            if (rectangleBuffer.size()>ZERO) {
                
                logger.debug ("for constraint " + index + 
                    " having name " +mipConstraintList.get(index).name + 
                    " there are this many rects " + getNumberOfRects(rectangleBuffer)  );
                
                
                
                for (double lp : rectangleBuffer.keySet()){
                    List<Rectangle> rectanglesAtDepth = rectangleBuffer.get(lp);
                    logger.debug ("lp is " + lp );
                    if (rectanglesAtDepth!=null){
                        for (Rectangle rect : rectanglesAtDepth)   {
                            logger.debug (rect) ;
                            logger.debug ("lp relax value for minimization is " +rect.lpRelaxValueMinimization + " check lp is "+ lp) ;
                        }
                    }
                } 
                
                saveRectanglesToMap(  ) ;
                //clear rectanle collection before solving fro next constraint
                rectangleBuffer.clear();
            }            
        }*/
        
        for (int index = ZERO ; index <mipConstraintList.size() && ! isHaltFilePresent (); index ++ ) {
            
            UpperBoundedConstarint ubc = new UpperBoundedConstarint( mipConstraintList.get(index)) ;
            Collector collector = new Collector (ubc);
            collector.collect();
            
            //System.out.println("test results for constraint "+ index) ;
            //Tester tester = new Tester (collector.collectedFeasibleRectangles);
            
            
            rectangleBuffer=collector.collectedFeasibleRectangles;
            
            if (rectangleBuffer.size()>ZERO) {
                
                logger.debug ("for constraint " + index + 
                    " having name " +mipConstraintList.get(index).name + 
                    " there are this many rects " + getNumberOfRects(rectangleBuffer)  );
                
                /*
                for (double lp : rectangleBuffer.keySet()){
                    List<Rectangle> rectanglesAtDepth = rectangleBuffer.get(lp);
                    logger.debug ("lp is " + lp );
                    if (rectanglesAtDepth!=null){
                        for (Rectangle rect : rectanglesAtDepth)   {
                            logger.debug (rect) ;
                            logger.debug ("lp relax value for minimization is " +rect.lpRelaxValueMinimization  ) ;
                        }
                    }
                } 
                */
                
                saveRectanglesToMap(  ) ;
                //clear rectanle collection before solving fro next constraint
                rectangleBuffer.clear();
            }  
        }
   
        logger.debug ("Starting solve ... " ) ;
        AnalyticSolver solver = new AnalyticSolver( infeasibleRectangle_Cumulative_Collection);
        solver.solve();
        
        
        //test knapsack 3 with equality constrainst , maybe okay becase no longer creating branches when no vars free to move
       
        /*Solver solver = new Solver();
        solver.solve(false);
        logger.debug ("Status is " + solver.cplex.getStatus()) ;
        if (solver.cplex.getStatus().equals(Status.Optimal)) {
            logger.debug ("Optimum is " + solver.cplex.getObjValue()) ;
        }
        logger.debug ("Custom branch count is "+ solver.branchHandler.customBranchingDecisions);
        logger.debug ("Cplex default branch count is "+ solver.branchHandler.cplexDefualtBranchDecisions);
        */
        logger.debug ("Start vanilla solve ") ;
        //(new Solver()) .solve(true );
        logger.debug ("Completed vanilla solve ") ;
        
        
    }//end main
    
    private static void saveRectanglesToMap (   ){
        for (double lp : rectangleBuffer .keySet()){
            List<Rectangle> new_rectanglesAtDepth = rectangleBuffer .get(lp);             
            if (new_rectanglesAtDepth!=null){
                 List<Rectangle> existingRectanglesAtDepth =  infeasibleRectangle_Cumulative_Collection.get(lp);         
                 if (existingRectanglesAtDepth==null) existingRectanglesAtDepth= new ArrayList<Rectangle>();
                 existingRectanglesAtDepth.addAll(new_rectanglesAtDepth) ;
                 infeasibleRectangle_Cumulative_Collection.put(lp, existingRectanglesAtDepth);         
            }
        }
        logger.debug ("infeasibleRectangle_Collection_Cumulative total rects "+getNumberOfRects( infeasibleRectangle_Cumulative_Collection) );
    }
    
    private static int getNumberOfRects ( Map<Double, List<Rectangle> > map2){
        int count = ZERO;
        for (List<Rectangle> rectlist : map2.values()){
            count +=rectlist.size();
        }
        return count;
    }
 
         
    private static boolean isLogFolderEmpty() {
        File dir = new File (LOG_FOLDER );
        return (dir.isDirectory() && dir.list().length==ZERO);
    }
    
    //minimization objective
    private static Objective getObjective (IloCplex cplex) throws IloException {
        
        List<VariableCoefficientTuple>   objectiveExpr = new ArrayList<VariableCoefficientTuple>   ();
        
        IloObjective  obj = cplex.getObjective();
        boolean isMaximization = obj.getSense().equals(IloObjectiveSense.Maximize);
        
        IloLinearNumExpr expr = (IloLinearNumExpr) obj.getExpr();
                 
        IloLinearNumExprIterator iter = expr.linearIterator();
        while (iter.hasNext()) {
           IloNumVar var = iter.nextNumVar();
           double val = iter.getValue();
           
           //convert  maximization to minimization 
           VariableCoefficientTuple tuple = new VariableCoefficientTuple (var.getName(), !isMaximization ? val : -val);
           //logger.debug ("Obj " + tuple.coeff + "*" + tuple.varName) ;
           objectiveExpr.add(tuple );
        }
        
        return new Objective (objectiveExpr) ;
        
         
    }
     
    //get all constraints as lower bounds
    private static List<LowerBoundConstraint> getConstraints(IloCplex cplex) throws IloException{
        
        List<LowerBoundConstraint> result = new ArrayList<LowerBoundConstraint>();
        
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        for (IloRange rangeConstraint : lpMatrix.getRanges()){            
             
            boolean isUpperBound = Math.abs(rangeConstraint.getUB())<Double.MAX_VALUE ;
            boolean isLowerBound = Math.abs(rangeConstraint.getLB())<Double.MAX_VALUE ;
            boolean isEquality = rangeConstraint.getUB()==rangeConstraint.getLB();
            boolean isRange = isUpperBound && isLowerBound && !isEquality;
            boolean isUpperBoundOnly =  isUpperBound && !isLowerBound  ;
            boolean isLowerBoundOnly =!isUpperBound && isLowerBound ;
            //equality constraints will be converted into 2 UB constraints - not handled right now
                        
            LowerBoundConstraint lbc = null;
            if ( isUpperBoundOnly || isLowerBoundOnly ) {
                
                //convert upper bound to lower bound  
                double pseudoBound = isUpperBound? -rangeConstraint.getUB(): rangeConstraint.getLB();
                IloLinearNumExprIterator constraintIterator =    ((IloLinearNumExpr) rangeConstraint.getExpr()).linearIterator();
                //this will be our representation of this constarint
                List<VariableCoefficientTuple>   constraintExpr = new ArrayList<VariableCoefficientTuple> ();
                while (constraintIterator.hasNext()) {
                    String varName = constraintIterator.nextNumVar().getName();
                    Double coeff =  constraintIterator.getValue();
                    constraintExpr.add(new VariableCoefficientTuple(varName, isUpperBound? -coeff: coeff));
                }
                //here is the constraint, in our format
                lbc  = new LowerBoundConstraint (rangeConstraint.getName(),   constraintExpr,   pseudoBound ) ;
                //add it to our list of constraints
                result.add(lbc);
                //logger.debug(lbc);
                
            }    else     if (isEquality) {
                     
                //we will add two constraints , one LB and one UB           
                IloLinearNumExprIterator constraintIterator =    ((IloLinearNumExpr) rangeConstraint.getExpr()).linearIterator();
                //this will be our representation of this constarint
                List<VariableCoefficientTuple>   constraintExprUB = new ArrayList<VariableCoefficientTuple> ();
                List<VariableCoefficientTuple>   constraintExprLB = new ArrayList<VariableCoefficientTuple> ();
                while (constraintIterator.hasNext()) {
                    String varName = constraintIterator.nextNumVar().getName();
                    Double coeff =  constraintIterator.getValue();
                    constraintExprLB.add(new VariableCoefficientTuple(varName, coeff));
                    constraintExprUB.add(new VariableCoefficientTuple(varName,   -coeff));
                }
                                
                //here is the LB constraint, in our format
                lbc  = new LowerBoundConstraint (rangeConstraint.getName()+NAME_FOR_EQUALITY_CONSTRAINT_LOWER_BOUND_PORTION,
                                                 constraintExprLB,   rangeConstraint.getLB() ) ;
                //add it to our list of constraints
                result.add(lbc);  
                //logger.debug(lbc);
                //second constraint which is UB
                lbc  = new LowerBoundConstraint (rangeConstraint.getName()+NAME_FOR_EQUALITY_CONSTRAINT_UPPER_BOUND_PORTION,
                                                 constraintExprUB,  - rangeConstraint.getUB() ) ;
                //add it to our list of constraints
                result.add(lbc); 
                //logger.debug(lbc);
                          
            } else if (isUpperBound && isLowerBound && !isEquality) {
                System.err.println("Range constraints not allowed -LATER ");
                exit(ONE);
                // such constraints are not read by cplex.import, it seeems
                /*
                //range constraint, create 2 constraints
                IloLinearNumExprIterator constraintIterator =    ((IloLinearNumExpr) rangeConstraint.getExpr()).linearIterator();
                //this will be our representation of this constarint
                List<VariableCoefficientTuple>   constraintExpr_UB = new ArrayList<VariableCoefficientTuple> ();
                List<VariableCoefficientTuple>   constraintExpr_LB = new ArrayList<VariableCoefficientTuple> ();
                while (constraintIterator.hasNext()) {
                    String varName = constraintIterator.nextNumVar().getName();
                    Double coeff =  constraintIterator.getValue();
                    constraintExpr_UB.add(new VariableCoefficientTuple(varName,   coeff ));
                    constraintExpr_LB.add(new VariableCoefficientTuple(varName,  - coeff ));
                }
                ubc  = new UpperBoundConstraint (rangeConstraint.getName()+"U",   constraintExpr_UB,    rangeConstraint.getUB()  ) ;
                //add it to our list of constraints
                result.add(ubc);
                //logger.debug(ubc);
                
                //now add the LB constraint
                ubc  = new UpperBoundConstraint (rangeConstraint.getName()+"L",   constraintExpr_LB,   - rangeConstraint.getLB()  ) ;
                //add it to our list of constraints
                result.add(ubc);
                //logger.debug(ubc);
                */  
            }
                   
        }//end for
        
        return result;
        
    }//end method
    
    private static boolean isHaltFilePresent (){
        File file = new File( HALT_FILE);
         
        return file.exists();
    }
    
}
