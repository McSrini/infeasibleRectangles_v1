/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblerectangles_v1.cplex.solver;

import static ca.mcmaster.infeasiblerectangles_v1.Constants.ZERO;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 */
public class EmptyBranchHandler extends IloCplex.BranchCallback {

    public long numBranches = ZERO;
    
    protected void main() throws IloException {
        if ( getNbranches()> ZERO ){  
            numBranches+= getNbranches();                                      
        }
    }
    
}
