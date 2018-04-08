/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.analytic;

import ca.mcmaster.infeasiblerectangles_v1.commonDatatypes.Rectangle;
import static java.lang.System.exit;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class Tester {
    private Map<Double, List<Rectangle>  > collectedFeasibleRectangles ;
    public Tester (Map<Double, List<Rectangle>  > collectedFeasibleRectangles ){
        this.collectedFeasibleRectangles = collectedFeasibleRectangles;
        //
        for (int index7=0 ; index7<2; index7++ ){
            for (int index1=0 ; index1<2; index1++ ){
                for (int index2=0 ; index2<2; index2++ ){
                    for (int index3=0 ; index3<2; index3++ ){

                        for (int index4=0 ; index4<2; index4++ ){
                            for (int index5=0 ; index5<2; index5++ ){
                                for (int index6=0 ; index6<2; index6++ ){
                                    
                                    //knapsacksmall
                                    //int val = 0 + 30*index1 + 50*index2 + 40*index3+ 10*index4 + 40*index5 + 30*index6+ 10*index7;
                                    //int UB = 99.99
                                    
                                    //knapsack two
                                    //c0: 30 x1 + 50 x2 + 40 x3 + 10 x4 + 40 x5 + 30 x6 + 10 x7  <= 100  
                                    //int val = 0 + 30*index1 + 50*index2 + 40*index3+ 10*index4 + 40*index5 + 30*index6+ 10*index7;
                                    //int UB =100;
                                    
                                    int val = index1 +  index2 + index3+ index4 + index5 + index6+ index7;
                                    int UB = 3;
                                    // c1:    -x1 -  x2  -   x3 -  x4  -  x5 -  x6 -  x7  >= -3  
                                    
                                    if (val >UB){
                                        
                                        System.out.println ("x1="+ index1 + ","+
                                                            "x2="+ index2 + ","+
                                                            "x3="+ index3 + ","+
                                                            "x4="+ index4 + ","+
                                                            "x5="+ index5 + ","+
                                                            "x6="+ index6 + ","+
                                                            "x7="+ index7 ) ;
                                        
                                        //must fit into some rect
                                        if (!isCompatible( index1,  index2,  index3,  index4, index5,    index6,   index7)) {
                                            System.err.println("****** no compatible rectangle *** ");
                                            exit(1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

               
    }
    
    private boolean isCompatible(int index1, int index2, int index3, int index4, int index5, int  index6, int index7) {
        boolean result = false;
        for ( List<Rectangle> rects : collectedFeasibleRectangles.values()) {
            for (Rectangle rect : rects){
                if (index1==0) {
                    //x1 cannot be 1 in this rect
                    if (rect.oneFixedVariables.contains("x1"))  continue; //next rect
                }else {
                    if (rect.zeroFixedVariables.contains("x1"))  continue; //next rect
                }
                if (index2==0) {
                    //x2 cannot be 1 in this rect
                    if (rect.oneFixedVariables.contains("x2"))  continue; //next rect
                }else {
                    if (rect.zeroFixedVariables.contains("x2"))  continue; //next rect
                }
                if (index3==0) {
                    //x3 cannot be 1 in this rect
                    if (rect.oneFixedVariables.contains("x3"))  continue; //next rect
                }else {
                    if (rect.zeroFixedVariables.contains("x3"))  continue; //next rect
                }
                if (index4==0) {
                    //x4 cannot be 1 in this rect
                    if (rect.oneFixedVariables.contains("x4"))  continue; //next rect
                }else {
                    if (rect.zeroFixedVariables.contains("x4"))  continue; //next rect
                }
                if (index5==0) {
                    //x5 cannot be 1 in this rect
                    if (rect.oneFixedVariables.contains("x5"))  continue; //next rect
                }else {
                    if (rect.zeroFixedVariables.contains("x5"))  continue; //next rect
                }
                if (index6==0) {
                    //x6 cannot be 1 in this rect
                    if (rect.oneFixedVariables.contains("x6"))  continue; //next rect
                }else {
                    if (rect.zeroFixedVariables.contains("x6"))  continue; //next rect
                }
                if (index7==0) {
                    //x7 cannot be 1 in this rect
                    if (rect.oneFixedVariables.contains("x7"))  continue; //next rect
                }else {
                    if (rect.zeroFixedVariables.contains("x7"))  continue; //next rect
                }
                
                //all vars pass this rect
                return true;
            }
        }
        return result;
    }
}
