/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex.solver;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.DOUBLE_ZERO;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ONE;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import ca.mcmaster.infeasiblerectangles_v1.IR_Driver;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.MAX_RECTS_FOR_CALCULATING_BEST_BRANCHING_VAR;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.VariableCoefficientTuple;
import java.util.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class BranchingUtils {
    
    public Map<Double, List<Rectangle>  > rectangle_Compatible_With_Zero_Side = new TreeMap <Double, List<Rectangle>>  ();    
    public Map<Double, List<Rectangle>  > rectangle_Compatible_With_One_Side = new TreeMap <Double, List<Rectangle>>  ();    
    
    public    BranchingUtils (){
        
    }
    
    public void split (Map<Double, List<Rectangle>  > parent_rectangle_Collection, String branchingVar , 
            List <String> nodeZeroFixedVariables, List <String> nodeOneFixedVariables) {
        
        //by this branching, is one of the sides exactly matching an infeasible rect ?
        boolean isZeroSideInfeasible = false;
        boolean isOneSideInfeasible = false;
        
        for (Map.Entry<Double, List<Rectangle>>   entry : parent_rectangle_Collection.entrySet()) {
            List<Rectangle> zeroRects = new ArrayList<Rectangle>();
            List<Rectangle> oneRects = new ArrayList<Rectangle>();
            for (Rectangle rect : entry.getValue()) {
                if (! rect.oneFixedVariables.contains(branchingVar) && !isZeroSideInfeasible)  {
                    SubSetEnum isSubset = isRectSubSetOFBranchingConsditions(true, branchingVar  ,   rect,
                                                        nodeZeroFixedVariables,   nodeOneFixedVariables) ;
                    if (isSubset.equals(SubSetEnum.NOT_SUBSET)) {
                        zeroRects.add(rect);
                    } else  isZeroSideInfeasible=true;
                    /* if (isSubset.equals(SubSetEnum.PERFECT_MATCH)) {
                        isZeroSideInfeasible=true;
                    }*/
                    
                }
                if (! rect.zeroFixedVariables.contains(branchingVar) && !isOneSideInfeasible) {
                    SubSetEnum isSubset = isRectSubSetOFBranchingConsditions(false, branchingVar  ,   rect,
                                                        nodeZeroFixedVariables,   nodeOneFixedVariables) ;
                    if (isSubset.equals(SubSetEnum.NOT_SUBSET)) {
                        oneRects.add(rect);
                    }else  isOneSideInfeasible=true;
                    /*if (isSubset.equals(SubSetEnum.PERFECT_MATCH)){
                        isOneSideInfeasible=true;
                    }  */                   
                }                 
            }
            if (zeroRects.size()>ZERO && !isZeroSideInfeasible) rectangle_Compatible_With_Zero_Side.put( entry.getKey(), zeroRects );
            if (oneRects.size()>ZERO && !isOneSideInfeasible) rectangle_Compatible_With_One_Side.put (entry.getKey(), oneRects);
        }  
        
        if (isZeroSideInfeasible) rectangle_Compatible_With_Zero_Side.clear();
        if (isOneSideInfeasible)  rectangle_Compatible_With_One_Side.clear();
            
    }
    
    //is this rectangle  rendered infeasible by this node branching on this var in this direction
    //if so, then do not include this infeasible rectangle as an infeasible rectangle under this node
    public SubSetEnum isRectSubSetOFBranchingConsditions(boolean isZeroDirection, String branching_var, Rectangle rect,
                                                      List <String> nodeZeroFixedVariables, List <String> nodeOneFixedVariables) {
        SubSetEnum result = SubSetEnum.SUBSET;
        
        List <String> nodeZeroFixedVariablesAugmented = new ArrayList <String> ();
        nodeZeroFixedVariablesAugmented.addAll(nodeZeroFixedVariables);
        List <String> nodeOneFixedVariablesAugmented = new ArrayList <String> ();
        nodeOneFixedVariablesAugmented.addAll(nodeOneFixedVariables);
        if (isZeroDirection) {
            nodeZeroFixedVariablesAugmented.add(branching_var) ;
        }else {
            nodeOneFixedVariablesAugmented.add(branching_var);
        }
        
        //are all zero fixings for the node , plus this var if dir is down, a superset of rects's one fixings ?
        for (String var : rect.zeroFixedVariables) {
            if (!nodeZeroFixedVariablesAugmented.contains(var)  ) {
                result = SubSetEnum.NOT_SUBSET;
                break;
            }
        }
        
        //are all one fixings for the node , plus this var if dir is up, a superset of rects's one fixings?
        for (String var : rect.oneFixedVariables) {
            if (SubSetEnum.NOT_SUBSET.equals(result)) break;
            if (!nodeOneFixedVariablesAugmented.contains(var)  ) {
                result = SubSetEnum.NOT_SUBSET;
                break;
            }
        }
        
        //if  subset then check if perfect match
        /*if (SubSetEnum.SUBSET.equals(result)) {
            if (rect.oneFixedVariables.size() ==nodeOneFixedVariablesAugmented.size() &&
                rect.zeroFixedVariables.size()==nodeZeroFixedVariablesAugmented.size()     ) {
                result = SubSetEnum.PERFECT_MATCH;
            }
        }*/
        
        return result;        
    }
 
    //find the var having highest refcount
    public VariableCoefficientTuple getBestChoiceBranchingVariable (  Map<Double, List<Rectangle>  > rectangle_Collection,  
                                                    List <String> zeroFixedVariables, List <String> oneFixedVariables){  
        String bestVar = null;
        int highestRefCount = ZERO;
        
        List<Rectangle> rectanglesToConsiderForBranchingVarCalculation = getRectanglesToConsiderForBranchingVarCalculation(rectangle_Collection);
        
        //collect refcounts into this map
        Map<String, Integer> varRefCountMap = new HashMap<String, Integer> () ;
        
        for (Rectangle rect: rectanglesToConsiderForBranchingVarCalculation){
             List<String> variablesUsedForBranchingInThisRectangle = getVariablesUsedForBranchingInThisRectangle (rect, zeroFixedVariables,   oneFixedVariables);              
             if (variablesUsedForBranchingInThisRectangle.size()>ZERO) updateRefCounts(varRefCountMap, variablesUsedForBranchingInThisRectangle) ;
        }
        
        //pick the highest refcount var , in case of tie best objective coeff wins
        if (varRefCountMap.size()>ZERO){
            highestRefCount= Collections.max(varRefCountMap.values());
        } else {
            System.out.println("ERROR");
        }
        List<String> candidateVars = new ArrayList<String> ();
        for (Entry<String, Integer> entry : varRefCountMap.entrySet()){
            String thisVar = entry.getKey();
            Integer thisCount = entry.getValue();
            if (thisCount==highestRefCount) candidateVars.add(thisVar) ;
        }
        if (candidateVars.size()==ONE) {
            bestVar=candidateVars.get(ZERO);
        }else if (candidateVars.size()>ONE) {
            bestVar =getVarWithHighestObjectiveCoeff(candidateVars) ;
        } else {
            //default to null
        }
               
        return new VariableCoefficientTuple (bestVar, highestRefCount) ;
    }
    
 
    
    private List<Rectangle> getRectanglesToConsiderForBranchingVarCalculation (Map<Double, List<Rectangle>  > rectangle_Collection) {
        
        List<Rectangle> rectanglesToConsider = new ArrayList<Rectangle> ();
         
        //add to result list until size exceeded
        for (List<Rectangle> rectList : rectangle_Collection.values()) {
            for (Rectangle rect : rectList) {
                if (rectanglesToConsider.size()<MAX_RECTS_FOR_CALCULATING_BEST_BRANCHING_VAR) {
                    rectanglesToConsider.add( rect);
                } else {
                    break;
                }
            }      
            if (rectanglesToConsider.size()>=MAX_RECTS_FOR_CALCULATING_BEST_BRANCHING_VAR)  break;
        }
 
        return rectanglesToConsider;
    }
        
    
    private void updateRefCounts(Map<String, Integer>  varRefCountMap, List<String>  variablesUsedForBranchingInThisRectangle){
        for (String var : variablesUsedForBranchingInThisRectangle) {
            int currentCount = varRefCountMap.get(var) == null ? ZERO: varRefCountMap.get(var);
            varRefCountMap.put(var, ONE+currentCount);
        }
    }
    
    //get vars used , consider only free vars
    private  List<String> getVariablesUsedForBranchingInThisRectangle(Rectangle rect, List<String> zeroFixedVariables,  List<String>  oneFixedVariables){
        List<String> variablesUsedForBranchingInThisRectangle = new ArrayList<String> ();
        
        for (String var : rect.zeroFixedVariables) {
            if (!zeroFixedVariables.contains(var) && !oneFixedVariables.contains(var)) variablesUsedForBranchingInThisRectangle.add(var );
        }
        for (String var : rect.oneFixedVariables){
            if (!variablesUsedForBranchingInThisRectangle.contains(var)) {
                 if (!zeroFixedVariables.contains(var) && !oneFixedVariables.contains(var)) {
                     variablesUsedForBranchingInThisRectangle.add(var);
                 }
            }
        }
        
        if (variablesUsedForBranchingInThisRectangle.size()==ZERO)  {
            System.out.print("ERROR") ;
        }
        return variablesUsedForBranchingInThisRectangle;
    }
    
    private String getVarWithHighestObjectiveCoeff(List<String> candidateVars){
        String bestVar = null;
        Double bestValSoFar = DOUBLE_ZERO;
         
        for (String  var: candidateVars){
            double thisVal = IR_Driver.objective.getObjectiveCoeff(var);
            if ( Math.abs( thisVal) > Math.abs(bestValSoFar)) {
                bestValSoFar=thisVal;
                bestVar= var;
            }
        }
        
        
        return bestVar;
    }
    
 
    
}
