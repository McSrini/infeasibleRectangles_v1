/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.LOG_FOLDER;
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ONE; 
import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import static ca.mcmaster.infeasiblerectangles_v1.Parameters.USE_STRICT_INEQUALITY;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.UpperBoundedConstarint;
import ilog.concert.IloException;
import java.util.List;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
 

/**
 *
 * @author tamvadss
 */
public class NodeHandler extends IloCplex.NodeCallback {
         
      
    //the root of this tree has these
    private UpperBoundedConstarint currentConstraintRoot;
    private List <String> zeroFixedVariablesRoot  ;
    private List <String> oneFixedVariablesRoot;
    
     //note down vars in the model, key by name
    //private Map<String, IloNumVar> modelVars;
    private static Logger logger=Logger.getLogger(NodeHandler.class);
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+NodeHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
        
        
    }
    
 
 
    public NodeHandler(UpperBoundedConstarint currentConstraint,   
                          List <String> zeroFixedVariables, List <String> oneFixedVariables ){
        this.   currentConstraintRoot=  currentConstraint;
        zeroFixedVariablesRoot=zeroFixedVariables;
        oneFixedVariablesRoot=oneFixedVariables;
        //this.modelVars= modelVars;
    }
    
    protected void main() throws IloException {
        if(getNremainingNodes64()> ZERO){
            
            if (null==getNodeData(ZERO)){
                //root of mip
                NodeAttachment data = new NodeAttachment (zeroFixedVariablesRoot , oneFixedVariablesRoot,  currentConstraintRoot/*, getObjValue(ZERO)*/);
                setNodeData(ZERO,data);                
            }
            
            NodeAttachment attachedData =(NodeAttachment)getNodeData(ZERO);
            
           
            
            if (isRectangular ( attachedData, !USE_STRICT_INEQUALITY)){
                //if original constraint is a strict inequality, then collecting rectangles can collect rectangle whose vertex is on the constraint
                //Otherwise, we cannot collect solutions that are exactly on the constraint
                attachedData.isRectangular=true;
                setNodeData(ZERO,attachedData); 
                logger.debug ("\nNode marked as rectangle ") ;
                 printNode(  attachedData);
            }else {
                //logger.debug ("Node NOT marked as rectangle ") ;
            }
        }
    }
    
  
    //* Checks if , with all free vars set at their worst possible values, constraint is still feasible
    private boolean isRectangular (NodeAttachment attachedData, boolean isStrict ) throws IloException{
        
        return     attachedData.reducedConstraint.isGauranteedFeasible(isStrict) ;
       
    }
     
    private void printNode(NodeAttachment attachedData){
         String  zerovars= "\nZero vars are ";
        for (String str : attachedData.zeroFixedVariables){
            zerovars +=(str+",") ;
        }
        logger.debug(zerovars) ;
        zerovars= "One vars are ";
         for (String str : attachedData.oneFixedVariables){
            zerovars +=(str+",") ;
        }
          logger.debug(zerovars) ;
    }
    
}
