/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex.solver;

import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

/**
 *
 * @author tamvadss
 * 
 * tagged to every node in the solution tree
 * 
 * has list of branching vars in priority order 0 being highest priority
 * 
 * has list of descendent infeasible rectangles by their level below root. 
 * When branching varible suggestions are exhuasted, these should be refreshed and used to find branching vars from here on downward
 * 
 * if we cannot find a recommendation for branching var , take cplex defualt
 * 
 */
public class CB_NodeAttachment {
      
    public List <String> zeroFixedVariables = new ArrayList <String>();
    public List <String> oneFixedVariables = new ArrayList <String>();
    
    public List <String> branchingVariablesInPriorityOrder = new ArrayList <String>();
    
     
    public CB_NodeAttachment (List <String> zeroFixedVariables ,List <String> oneFixedVariables, List <String> branchingVariablesInPriorityOrder){
        this.zeroFixedVariables.addAll(zeroFixedVariables);
        this.oneFixedVariables.addAll(oneFixedVariables);
        this.branchingVariablesInPriorityOrder.addAll(branchingVariablesInPriorityOrder);
    }
}
