/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex;
 
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.UpperBoundedConstarint;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class NodeAttachment {
     
    public List <String> zeroFixedVariables = new ArrayList <String>();
    public List <String> oneFixedVariables = new ArrayList <String>();
    
    //this is the constraint we are using right now, with some vars fixed by the branching conditions
    public UpperBoundedConstarint reducedConstraint ;
    
    public boolean isRectangular = false;
    
    //intger vertex , may or may not be a rectangle
    public boolean wasSolutionRejected = false;
    
     
    
    public NodeAttachment (List <String> zeroFixedVariables , List <String> oneFixedVariables, UpperBoundedConstarint reducedConstraint ) {
        this.reducedConstraint= reducedConstraint;
        this.zeroFixedVariables.addAll(zeroFixedVariables);
        this.oneFixedVariables.addAll(oneFixedVariables);
         
    }
    
       
}
