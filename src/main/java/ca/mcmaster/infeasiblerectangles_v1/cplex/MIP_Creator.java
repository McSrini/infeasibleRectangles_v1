/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.*;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.*;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.*;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloObjectiveSense;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.HashMap;
import java.util.*;
import java.util.Map;

/**
 *
 * @author tamvadss
 * 
 * creates CPLEX objects with upper bound constraints
 * 
 */
public class MIP_Creator {
      
    //this is the object we prepare for everyone else to use
    public IloCplex cplex; 
    
    public MIP_Creator (   LowerBoundConstraint lbc 
           /* List <String> zeroFixedVariables, List <String> oneFixedVariables     */    ) throws IloException {
        
        //import mip into ilocplex
        cplex = new IloCplex ();
        cplex.importModel(MIP_FILENAME);
        
        //convert to maximization
        if (cplex.getObjective().getSense().equals(IloObjectiveSense.Minimize)){
            IloObjective ilogObjective = cplex.getObjective();
            ilogObjective.setSense(IloObjectiveSense.Maximize);
            //objective expression is the same
            //IloNumExpr newExpr =ilogObjective. getExpr() ;
            //newExpr = cplex.prod(newExpr, -ONE);
            //ilogObjective.setExpr(newExpr);
        }else {
            System.err.println("Implementaion is only for minimization MIPs.") ;
            exit(ZERO);
        }
        
        //remove all constrs
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        IloRange[] constraints = lpMatrix.getRanges();
        for (IloRange constr: constraints ){
           // System.out.println (constr.getName() + " and "+lbc.name) ;
           boolean isNameMatching = constr.getName().equals(lbc.name);
           boolean isNameMatchingEqualityLower = lbc.name .equals(constr.getName()+NAME_FOR_EQUALITY_CONSTRAINT_LOWER_BOUND_PORTION);
           boolean isNameMatchingEqualityUpper = lbc.name .equals(constr.getName()+NAME_FOR_EQUALITY_CONSTRAINT_UPPER_BOUND_PORTION);
           if (!isNameMatching && !isNameMatchingEqualityLower && !isNameMatchingEqualityUpper) {
                cplex.delete(constr)   ;
           }else if (isNameMatching)  {
                //change constr direction
                boolean isUpperBound = Math.abs(constr.getUB())<Double.MAX_VALUE ;
           
                double currentLB = constr.getLB();
                double currentUB = constr.getUB();
                if (isUpperBound) {
                    constr.setLB(currentUB);
                    constr.setUB( Double.MAX_VALUE);
                }else {
                    constr.setUB(currentLB);
                    constr.setLB( -Double.MAX_VALUE);
                }
           }else if (isNameMatchingEqualityLower){
               //lower bound portion of equality constraint
               //switch sign to upper bound
               constr.setUB( constr.getLB());
               constr.setLB( -Double.MAX_VALUE);
           }else if (isNameMatchingEqualityUpper) {
               constr.setLB(constr.getUB());
               constr.setUB( Double.MAX_VALUE);
           }
        }
                        
        IloNumVar[] variables  =lpMatrix.getNumVars();
        Map<String, IloNumVar> modelVars = new HashMap <String, IloNumVar>();
        for (IloNumVar var: variables){
            modelVars.put(var.getName(),var );
        }
        
        //apply var bounds
       // merge (  variables , this.getLowerBounds(oneFixedVariables), this.getUpperBounds(zeroFixedVariables)  ) ;
        
         
        //prepare reduced constraint
     //   LowerBoundConstraint reducedConstraint = lbc.getReducedConstraint(zeroFixedVariables, oneFixedVariables);
        
        //add reduced or original constr into ilocplex using modelvars 
        //note LB constraint is converted into UB
      //  System.out.println ("add constr as UB "+ lbc.name) ;
      //  this.addConstraintAsUpperBound( modelVars , lbc) ;
        
        
        //attach 3 callbacks
        UpperBoundedConstarint ubc = new UpperBoundedConstarint( lbc);
        BranchHandler bh = new BranchHandler(modelVars, ubc, new ArrayList <String>(),  new ArrayList <String>()) ;
        NodeHandler nh = new NodeHandler(ubc , new ArrayList <String>(),  new ArrayList <String>());
        IncumbentHandler ih = new IncumbentHandler (ubc , new ArrayList <String>(),  new ArrayList <String>());
        cplex.use(bh) ;
        cplex.use(ih) ;
        cplex.use(nh) ;
                
        //set solution params
        cplex.setParam(  IloCplex.IntParam.HeurFreq , -ONE);
        cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses,ZERO);        
        cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);
        //emphasis  optimality
        cplex.setParam(IloCplex.Param.Emphasis.MIP, MIP_EMHPASIS_FOR_COLLECTION);
        cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT_PER_CONSRAINT_FOR_INFEASIBLE_RECTANGLE_COLLECTION_SECONDS);
        cplex.setParam(IloCplex.Param.Threads, USE_MULTITHREADING_WITH_THIS_MANY_THREADS); 
        //cplex.setParam(IloCplex.Param.MIP.Display, ZERO);
        
        //varify maximization mip with 1 constraint
        /*System.out.println("DEBUG "+cplex.getObjective().getSense());
        System.out.println(cplex.getObjective());
          lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
          constraints = lpMatrix.getRanges();
        for (IloRange constr: constraints ){
           System.out.println(constr.getExpr() );
           double debug_lb =constr.getLB();
           double debug_ub = constr.getUB();
           System.out.println(" LB " + debug_lb + "UB " +debug_ub )   ;
        }*/
               
    
    }
    
    /*
    //add constarint into cplex object
    //note we take a lbc and add it as ubc
    private void addConstraintAsUpperBound (   Map<String, IloNumVar> modelVars  ,LowerBoundConstraint lbc) throws IloException{
        IloNumExpr constarintExpression = cplex.numExpr();
        for (VariableCoefficientTuple tuple : lbc.sortedConstraintExpr){
            constarintExpression = cplex.sum(constarintExpression, cplex.prod(tuple.coeff, modelVars.get(tuple.varName) ));
        }
       IloRange  addedConstr =  cplex.addLe(constarintExpression,  lbc.lowerBound);    
         System.out.println ("debug constraint for cplex is  "+ constarintExpression + " bound is " + lbc.lowerBound) ;
          System.out.println(cplex.getObjective().getSense());
        System.out.println(cplex.getObjective());
         IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        IloRange[] constraints = lpMatrix.getRanges();
        for (IloRange constr: constraints ){
           System.out.println(constr.getExpr() );
           double debug_lb =constr.getLB();
           double debug_ub = constr.getUB();
           System.out.println(" LB " + debug_lb + "UB " +debug_ub )   ;
        }
    } 
    
    private  Map< String,Double > getLowerBounds ( List <String> oneFixedVariables){
        Map< String,Double > oneFixings = new HashMap< String,Double >();
        for (String str: oneFixedVariables){
            oneFixings.put( str, DOUBLE_ONE);
        }
        return oneFixings;
    }
        
    private  Map< String,Double > getUpperBounds ( List <String> zeroFixedVariables){
        Map< String,Double > zeroFixings = new HashMap< String,Double >();
        for (String str: zeroFixedVariables){
            zeroFixings.put( str, DOUBLE_ZERO);
        }
        return zeroFixings;
    }
    */
        
    /**
     * 
     * To the CPLEX object ,  apply all the bounds mentioned in attachment
     */
    /*
    private    void  merge (  IloNumVar[] variables ,  Map< String,Double > lowerBounds, Map< String,Double > upperBounds  ) throws IloException {


        for (int index = ZERO ; index <variables.length; index ++ ){

            IloNumVar thisVar = variables[index];
            updateVariableBounds(thisVar,lowerBounds, false );
            updateVariableBounds(thisVar,upperBounds, true );

        }       
    }*/
    
    /**
     * 
     *  Update variable bounds as specified    
     */
    
    /*
    private      void updateVariableBounds(IloNumVar var, Map< String,Double > newBounds, boolean isUpperBound   ) 
            throws IloException{

        String varName = var.getName();
        boolean isPresentInNewBounds = newBounds.containsKey(varName);

        if (isPresentInNewBounds) {
            double newBound =   newBounds.get(varName)  ;
            if (isUpperBound){
                if ( var.getUB() > newBound ){
                    //update the more restrictive upper bound
                    var.setUB( newBound );
                    //logger.info(" var " + varName + " set upper bound " + newBound ) ;
                }
            }else{
                if ( var.getLB() < newBound){
                    //update the more restrictive lower bound
                    var.setLB(newBound);
                    //logger.info(" var " + varName + " set lower bound " + newBound ) ;
                }
            }               
        }

    }  
  */
}
