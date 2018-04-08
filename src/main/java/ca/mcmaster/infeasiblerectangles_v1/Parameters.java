/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1;

/**
 *
 * @author tamvadss
 */
public class Parameters {
    
       //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackFourTest.lp";  ////x1 x7  x4  x2
       //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackSmall.lp";  ////x1 x7  x4  x2
       //public static final String MIP_FILENAME = "F:\\temporary files here\\protfold.mps";
       //public static final String MIP_FILENAME = "harp2.mps";
       //public static final String MIP_FILENAME = "cov1075.mps";
       public static final String MIP_FILENAME = "stp3d.mps";
       
       public static boolean USE_STRICT_INEQUALITY = false;
       
       public static int MIP_EMHPASIS_FOR_COLLECTION = 2; //push bound
       public static int MIP_EMHPASIS_FOR_SOLUTION = 3; //just  bound
       
       public static int TIME_LIMIT_PER_CONSRAINT_FOR_INFEASIBLE_RECTANGLE_COLLECTION_SECONDS = 30;
       public static int        USE_MULTITHREADING_WITH_THIS_MANY_THREADS=1;
       
       public static int NODEFILE_TO_DISK = 3;
       
       
       public static boolean USE_MIP_GAP= false;
       public static double MIP_GAP_PERCENT = 0.06;  //6%
       
       public static int MAX_RECTS_FOR_CALCULATING_BEST_BRANCHING_VAR = 10000;
        
}
