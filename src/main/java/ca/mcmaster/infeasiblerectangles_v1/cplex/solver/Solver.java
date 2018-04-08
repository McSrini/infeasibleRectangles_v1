/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex.solver;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.LOG_FOLDER;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ONE;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.MIP_EMHPASIS_FOR_COLLECTION;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.MIP_EMHPASIS_FOR_SOLUTION;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.MIP_FILENAME;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.MIP_GAP_PERCENT;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.NODEFILE_TO_DISK;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.TIME_LIMIT_PER_CONSRAINT_FOR_INFEASIBLE_RECTANGLE_COLLECTION_SECONDS;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.USE_MIP_GAP;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.USE_MULTITHREADING_WITH_THIS_MANY_THREADS;
import ca.mcmaster.infeasiblerectangles_v1.cplex.BranchHandler;
import ca.mcmaster.infeasiblerectangles_v1.cplex.IncumbentHandler;
import ca.mcmaster.infeasiblerectangles_v1.cplex.NodeHandler;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class Solver {
    //
    public IloCplex cplex; 
    
    public  CB_BranchHandler branchHandler = null;
    
        
    private static Logger logger=Logger.getLogger(Solver.class);
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+Solver.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
        
        
    }
    
  
    public Solver () throws IloException{
        
        //import mip into ilocplex
        cplex = new IloCplex ();
        cplex.importModel(MIP_FILENAME);
        
        //
        if (!cplex.getObjective().getSense().equals(IloObjectiveSense.Minimize)){
            System.err.println("Implementaion is only for minimization MIPs.") ;
            exit(ZERO);
        } 
        
        //get the vars
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        IloNumVar[] variables  =lpMatrix.getNumVars();
        Map<String, IloNumVar> modelVars = new HashMap <String, IloNumVar>();
        for (IloNumVar var: variables){
            modelVars.put(var.getName(),var );
        }
        
        branchHandler = new CB_BranchHandler(modelVars) ;
        cplex.use(branchHandler) ;
        //cplex.use (new CB_Nodehandler()) ;
        
        cplex.setParam(IloCplex.Param.MIP.Strategy.File, NODEFILE_TO_DISK);
        if (USE_MIP_GAP) cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, MIP_GAP_PERCENT);
        //set solution params
        cplex.setParam(  IloCplex.IntParam.HeurFreq , -ONE);
        cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses,ZERO);        
        cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);
        cplex.setParam(IloCplex.Param.Emphasis.MIP, MIP_EMHPASIS_FOR_SOLUTION);
        //cplex.setParam(  IloCplex.IntParam.HeurFreq , -ONE);
        //cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses,ZERO);        
        //cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);
        //emphasis  optimality
        //cplex.setParam(IloCplex.Param.Emphasis.MIP, MIP_EMHPASIS_FOR_COLLECTION);
        //cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT_FOR_SOLUTIONNDS);
        //cplex.setParam(IloCplex.Param.Threads, USE_MULTITHREADING_WITH_THIS_MANY_THREADS); 
        
    }
    
    public void solve (boolean isVanilla) throws IloException {
        EmptyBranchHandler ebh = new EmptyBranchHandler();
        if (isVanilla) cplex.use(ebh) ;
        
        cplex.solve();
        
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        IloNumVar[] variables  =lpMatrix.getNumVars();
        
        for (IloNumVar var : variables) {
            System.out.println(" var is " + var.getName() + " has value " + cplex.getValue(var)) ;
        }
        
        
        if ((isVanilla)) System.out.println("vanilla solve made branches "+ ebh.numBranches + " and has optimal " + cplex.getObjValue());
    }
}
