/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.commonDatatypes;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.ONE;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import java.util.*;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class TreeNode extends Rectangle{
    
    public static int NODE_ID = ZERO;
    
    public int myId =-ONE;
    
    
    //list of infeasible rectangles that are compatible with this rectangle
    public Map <Double, List<Rectangle>>  myCompatibleRectangles = null;  
    
    public TreeNode (List <String> zeroFixedVariables , List <String> oneFixedVariables , Map <Double, List<Rectangle>>  myCompatibleRectangles){
        super( zeroFixedVariables ,  oneFixedVariables) ;
        this.myCompatibleRectangles = myCompatibleRectangles;
        myId =NODE_ID++;
    }
    
    public TreeNode(){
        super( new ArrayList <String>() , new ArrayList <String>()) ;
         myId =NODE_ID++;
    }
      public String toString (){
          return "Node id " + myId + " " + super.toString() + " infeas map size " + myCompatibleRectangles.size();
      }
}
