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
 
 * 
 */
public class CB_NodeAttachment {
      
    public List <String> zeroFixedVariables = new ArrayList <String>();
    public List <String> oneFixedVariables = new ArrayList <String>();
     
    
    public Map <Double, List<Rectangle>>  myCompatibleRectangles = null; //new   TreeMap <Double, List<Rectangle>>  ();    
    
    
    //when we run out of collected rectangles, we switch to pure cplex
    public boolean startUsingCplexDefaults= false; 
    
    public CB_NodeAttachment(boolean useCplexDefault){
         startUsingCplexDefaults =useCplexDefault;
    }
     
    public CB_NodeAttachment (List <String> zeroFixedVariables ,List <String> oneFixedVariables,      Map <Double, List<Rectangle>>  myCompatibleRectangles          ){
        this.zeroFixedVariables.addAll(zeroFixedVariables);
        this.oneFixedVariables.addAll(oneFixedVariables);
        this.myCompatibleRectangles = myCompatibleRectangles;
        
    }
}
