/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex.solver;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.ONE;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import ca.mcmaster.infeasiblerectangles_v1.IR_Driver;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.VariableCoefficientTuple;
import ilog.concert.IloNumVar;
import java.util.*;
import java.util.Map;

/**
 *
 * @author tamvadss
 * 
 * given a node, uses map of infeasible rectangles to first find descendant infeasible rectangles, and then finds branching
 * variables in priority order using these descendants
 * 
 */
public class BranchingVariableFinder {
    private   Map<Integer, List<Rectangle> > infeasibleRectanglesCumulative;
    private List<String > modelVars;
 
    public BranchingVariableFinder (Map<Integer, List<Rectangle> > infeasibleRectanglesCumulative,   List<String > modelVars){
        this. infeasibleRectanglesCumulative= infeasibleRectanglesCumulative;
        this.modelVars=modelVars;
    }
      
    public List <String> getBranchingVariablesInPriorityOrder(List <String> zeroFixedVariables , List <String> oneFixedVariables ){
                
        //desecndant rects at the closest level
        Map<Integer, List<Rectangle> > descendentRectangles  = getDescendantInfeasibleRectangles(  zeroFixedVariables ,  oneFixedVariables );
        
        //count of how many times each var occurs in descendant rectangles, index same as modelvars
        /*List<Integer> variableRefcount =new ArrayList <Integer>();
        //initialize
        for (int index=ZERO; index < modelVars.size(); index ++) {
             variableRefcount.add( ZERO);
        }*/
          
        List <String> branchingVariablesInPriorityOrder= new ArrayList <String>();
        if (descendentRectangles.size()>ZERO){
            
            int depth = Collections.min( descendentRectangles.keySet()); //only 1 element will be there anyway
            
            VariableRefcounter refCounter = new VariableRefcounter (descendentRectangles.get(depth), modelVars);
            refCounter.getVarRefcounts(branchingVariablesInPriorityOrder);
        }
        
        //find the var in the descendant rectangles having highest frequency
        //in case of tie -  break in favour of var having higher coeff in objective
        /*if (descendentRectangles.size()>ZERO){
            for (int index=ZERO; index < modelVars.size(); index ++) {
                String var = modelVars.get(index);
                //find refcount of this var
                int refCount = getVarRefCount ( var, descendentRectangles);                
                variableRefcount.set(index, refCount);
            }
            //add the largest reamining refcount var to priority order. In case of tie, break in favour of var with higher objective coeff
            while (true){
                
                int largestValue = Collections.max(variableRefcount);
                if (largestValue==ZERO) break;
                List <String> varList =getvariablesWithRefcount( largestValue, variableRefcount) ;
                branchingVariablesInPriorityOrder.addAll(varList )        ;                                
            }
        }       */ 
        
        return branchingVariablesInPriorityOrder;
    }
    
    /*
    //gets variables with specified refcount and removes them from list by setting their refcount to 0
    private List<String> getvariablesWithRefcount( int largestValue,  List<Integer> variableRefcount){
         List <String> varList = new ArrayList <String> ();
         List <Integer> positions = new ArrayList <Integer> ();
         
         for (int index =ZERO; index < variableRefcount.size(); index ++) {
             if (variableRefcount.get(index).equals(largestValue )){
                 positions.add(index);
                 varList.add(modelVars.get(index));
             }             
         }
         
         //remove these items from reckoning
         for (int position : positions){
             variableRefcount.set( position, ZERO)    ;
         }
         
         //
         //sort ietms in varList , highest object coeff first
         
         return sortByObjectiveCoefficientMagnitude(varList);
    }*/
    /*
    private double getObjectiveCoeffMagnitude (String var){
        double result = -ONE;
        for (VariableCoefficientTuple tuple : IR_Driver.objective.objectiveExpr){
            if ( tuple.varName.equals(var)) {
                result= Math.abs(tuple.coeff);
                break;
            }
        }
        return result;
    }*/
    /*
    private  List <String> sortByObjectiveCoefficientMagnitude ( List <String> varList ){
         List <String> result = new ArrayList <String> ();
         
         while (true) {
             if (varList.size()==ZERO) break;
             int indexOfLargest = -ONE;
             double maxCoeff = ZERO;
             for (int index = ZERO; index < varList.size(); index ++){
                 String var = varList.get(index);
                 if (getObjectiveCoeffMagnitude(var) >maxCoeff){
                     maxCoeff=getObjectiveCoeffMagnitude(var);
                     indexOfLargest= index;
                 }
             }
             //add thsi largest var to result
             result.add(varList.get( indexOfLargest));
             varList.remove( indexOfLargest);
         }
         return result;
    }*/
    
    /*
    //for a given var, find how many rectangles have it in their zero or one variable fixes
    private int getVarRefCount ( String var,  Map<Integer, List<Rectangle> > descendentRectangles){
        int count = ZERO;
        for ( List<Rectangle> rects : descendentRectangles.values()) {
            count +=getVarRefCount (  var,   rects);
            //there will be only 1 rect list
            break; 
        }
        
        return count;
    }
    
    private int getVarRefCount ( String var,    List<Rectangle> rectangles){
         int count = ZERO;
         for (Rectangle rect :  rectangles){
             if (rect.zeroFixedVariables.contains(var) || rect.oneFixedVariables.contains(var)) count ++;
         }
         return count;
    }*/
      
    private Map<Integer, List<Rectangle> > getDescendantInfeasibleRectangles(List <String> zeroFixedVariables , List <String> oneFixedVariables ){
        Map<Integer, List<Rectangle> > result = new TreeMap<Integer, List<Rectangle> >();
        
        int myDepth = zeroFixedVariables.size()+oneFixedVariables.size();
        for (int depth : infeasibleRectanglesCumulative.keySet()){
            if (depth <= myDepth) continue;
              
            //get rectangles at this depth
            List<Rectangle> rectanglesAtThisDepth = infeasibleRectanglesCumulative.get(depth);
            rectanglesAtThisDepth= (rectanglesAtThisDepth==null? new ArrayList <Rectangle> (): rectanglesAtThisDepth);
            List<Rectangle> descendantRectanglesAtThisDepth = new ArrayList<Rectangle> ();
            for (Rectangle rect: rectanglesAtThisDepth){
                if (isRectangleDescendant (zeroFixedVariables ,  oneFixedVariables,  rect)){
                    descendantRectanglesAtThisDepth.add(rect);
                }
            }
            if (descendantRectanglesAtThisDepth.size()>ZERO) {
                result.put (depth,descendantRectanglesAtThisDepth ) ;
                //we only need the closest rectangles
                
                break;
            }
              
        }
          
        return result;
    }
      
    private boolean isRectangleDescendant (List <String> myZeroFixedVariables , List <String> myOneFixedVariables, Rectangle rect) {
          
        boolean isDescendant = true ;
          
        for (String zeroVar : myZeroFixedVariables) {
            if (! rect.zeroFixedVariables.contains(zeroVar)) {
                isDescendant= false;
                break;
            }
        }
        
        for (String oneVar : myOneFixedVariables ) {
            if (false== isDescendant)  break;
            if (! rect .oneFixedVariables.contains(oneVar)) {
                isDescendant= false;
                break;
            }
        }
        
        return isDescendant && (myZeroFixedVariables.size()+myOneFixedVariables.size() < rect.oneFixedVariables.size() + rect.zeroFixedVariables.size()) ;
          
    }
      
}
