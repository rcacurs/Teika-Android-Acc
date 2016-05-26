package lv.edi.SmartWearProcessing;
import org.ejml.*;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.EigenDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.EigenOps;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

/**
 * Created by Richards on 01.12.2015..
 */
public class Calibration {
    /**
     * Function for fitting data to elipsoid for sensor data calibration.
     * @param inputData DenseMatrix64F n x 3 matrix, (n rows). each n is one measurement
     * @param outputOffset DenseMatrix64F column vector dim 3 (allocated before hand)
     * @param W_inverted DenseMatrix64F 3x3 matrix for scaling compensation
     */
    private static EigenDecomposition<DenseMatrix64F> eig;

    /*
    * This function initialized Calibration class. Must be called for funcionts - ellipsoidFitCalibration
    * to work
     */
    public static void init(){
       eig = DecompositionFactory.eig(3, true);
    }
    public static void ellipsoidFitCalibration(DenseMatrix64F inputData, DenseMatrix64F outputOffset, DenseMatrix64F W_inverted) throws IllegalArgumentException{
//        System.out.println("inputData "+inputData);
        DenseMatrix64F x = new DenseMatrix64F(inputData.numRows, 1); // preallocate for x, y, z
        DenseMatrix64F y = new DenseMatrix64F(inputData.numRows, 1);
        DenseMatrix64F z = new DenseMatrix64F(inputData.numRows, 1);

        CommonOps.extractColumn(inputData, 0, x); // ex4tract measurements
        CommonOps.extractColumn(inputData, 1, y);
        CommonOps.extractColumn(inputData, 2, z);

        // SOLVING OFFSETS
        // create model

        DenseMatrix64F D = constructModel(x, y, z, false);
        DenseMatrix64F Dt = CommonOps.transpose(D, null);
        DenseMatrix64F sums = CommonOps.sumRows(Dt, null);
        DenseMatrix64F DtD = new DenseMatrix64F(Dt.numRows, D.numCols);
        CommonOps.mult(Dt, D, DtD);
//        System.out.println("DtD "+DtD);
//        System.out.println("D "+D);
        DenseMatrix64F params = new DenseMatrix64F(sums.numRows, sums.numCols);

        if(!(CommonOps.solve(DtD, sums, params))) { // solve system to find ellipsoid parameters
            throw new IllegalArgumentException("Singular matrix constructed from input data");
        }

        DenseMatrix64F A = new DenseMatrix64F(4,4);
        A.set(0,0, params.get(0));
        A.set(0,1, params.get(3));
        A.set(0,2, params.get(4));
        A.set(0,3, params.get(6));

        A.set(1,0, params.get(3));
        A.set(1,1, params.get(1));
        A.set(1,2, params.get(5));
        A.set(1,3, params.get(7));

        A.set(2,0, params.get(4));
        A.set(2,1, params.get(5));
        A.set(2,2, params.get(2));
        A.set(2,3, params.get(8));

        A.set(3,0, params.get(6));
        A.set(3,1, params.get(7));
        A.set(3,2, params.get(8));
        A.set(3,3, -1);

        DenseMatrix64F d = new DenseMatrix64F(3,1);
        d.set(0, params.get(6));
        d.set(1, params.get(7));
        d.set(2, params.get(8));

        DenseMatrix64F Asub = new DenseMatrix64F(3,3);
        CommonOps.extract(A, 0, 3, 0, 3, Asub, 0, 0);

        DenseMatrix64F offsets = new DenseMatrix64F(3,1);
        if(!(CommonOps.solve(Asub,d,offsets))){
            throw new IllegalArgumentException("Singular matrix constructed from input data");
        }

        // SOLVING SCALING

        //remove offsets from data
        CommonOps.add(x, (offsets.get(0)));
        CommonOps.add(y, (offsets.get(1)));
        CommonOps.add(z, (offsets.get(2)));

        DenseMatrix64F K = constructModel(x, y, z, true);
//        System.out.println("K "+K);
        DenseMatrix64F Kt = CommonOps.transpose(K, null);
        DenseMatrix64F rowSums = CommonOps.sumRows(Kt, null);
        DenseMatrix64F KtK = new DenseMatrix64F(Kt.numRows, K.numCols);
        CommonOps.mult(Kt, K, KtK);
        DenseMatrix64F params2 = new DenseMatrix64F(rowSums.numRows, 1);
//        System.out.println("KtK "+KtK);
        if(!(CommonOps.solve(KtK, rowSums, params2))){ // solve system to find ellipsoid parameters
            throw new IllegalArgumentException("Singular matrix constructed from input data");
        }

        // ellipsoid algebraic form
        DenseMatrix64F AA = new DenseMatrix64F(3, 3);
        AA.set(0, 0, params2.get(0));
        AA.set(0, 1, params2.get(3));
        AA.set(0, 2, params2.get(4));

        AA.set(1, 0, params2.get(3));
        AA.set(1, 1, params2.get(1));
        AA.set(1, 2, params2.get(5));

        AA.set(2, 0, params2.get(4));
        AA.set(2, 1, params2.get(5));
        AA.set(2, 2, params2.get(2));
//        System.out.println("AA "+AA);

        // eigenvalue analysis

        if(eig.decompose(AA)){
//            System.out.println("Decomposition succesfull!");
        } else{
//            System.out.println("Decomposition unsucessfull");
        }
        DenseMatrix64F Dv = EigenOps.createMatrixD(eig);
//        System.out.println("Dv "+Dv);
        DenseMatrix64F V = EigenOps.createMatrixV(eig);
//        System.out.println("V "+V);
        DenseMatrix64F eigvals = new DenseMatrix64F(Dv.numRows, 1);
        CommonOps.extractDiag(Dv, eigvals);
//        System.out.println("eigvals "+eigvals);
        CommonOps.divide(1.0, eigvals);
//        System.out.println("divide eigvals "+eigvals);
        DenseMatrix64F radii = new DenseMatrix64F(eigvals.numRows, eigvals.numCols);
        CommonOps.elementPower(eigvals, 0.5, radii);
//        System.out.println("radii "+radii);

        double Bfield = Math.pow(radii.get(0)*radii.get(1)*radii.get(2), 1.0/3.0 );

        DenseMatrix64F Dvsq = new DenseMatrix64F(Dv.numRows, Dv.numCols);
        CommonOps.elementPower(Dv, 0.5, Dvsq);
        DenseMatrix64F Vinv = new DenseMatrix64F(V.numRows, V.numCols);

        if(!(CommonOps.invert(V, Vinv))){
            throw new IllegalArgumentException("Singular matrix");
        }
        DenseMatrix64F res1 = new DenseMatrix64F(V.numRows, Dvsq.numCols);
        CommonOps.mult(V, Dvsq, res1);
        DenseMatrix64F res = new DenseMatrix64F(res1.numRows, Vinv.numCols);
        CommonOps.mult(res1, Vinv, res);
        CommonOps.scale(Bfield, res);

        // format result
        for(int i=0; i<offsets.numRows; i++){
            outputOffset.set(i, offsets.get(i));
        }
        for(int j=0; j<res.numRows*res.numCols; j++){
            W_inverted.set(j, res.get(j));
        }

    }

    private static DenseMatrix64F constructModel(DenseMatrix64F x, DenseMatrix64F y, DenseMatrix64F z, boolean simplifiedForm){
        DenseMatrix64F D0 = new DenseMatrix64F(x.numRows, x.numCols);
        CommonOps.elementMult(x, x, D0);
        DenseMatrix64F D1 = new DenseMatrix64F(y.numRows, y.numCols);
        CommonOps.elementMult(y, y, D1);
        DenseMatrix64F D2 = new DenseMatrix64F(z.numRows, z.numCols);
        CommonOps.elementMult(z, z, D2);
        DenseMatrix64F D3 = new DenseMatrix64F(x.numRows, x.numCols);
        CommonOps.elementMult(x, y, D3);
        CommonOps.scale(2, D3);
        DenseMatrix64F D4 = new DenseMatrix64F(x.numRows, x.numCols);
        CommonOps.elementMult(x, z, D4);
        CommonOps.scale(2,D4);
        DenseMatrix64F D5 = new DenseMatrix64F(y.numRows, y.numCols);
        CommonOps.elementMult(y, z, D5);
        CommonOps.scale(2, D5);
        DenseMatrix64F D6 = new DenseMatrix64F(x.numRows, x.numCols);
        CommonOps.scale(2,x,D6);
        DenseMatrix64F D7 = new DenseMatrix64F(y.numRows, y.numCols);
        CommonOps.scale(2,y,D7);
        DenseMatrix64F D8 = new DenseMatrix64F(z.numRows, z.numCols);
        CommonOps.scale(2, z, D8);
        DenseMatrix64F D;
        if(!simplifiedForm) {
            D = new DenseMatrix64F(x.numRows, 9);

            CommonOps.insert(D0, D, 0, 0);
            CommonOps.insert(D1, D, 0, 1);
            CommonOps.insert(D2, D, 0, 2);
            CommonOps.insert(D3, D, 0, 3);
            CommonOps.insert(D4, D, 0, 4);
            CommonOps.insert(D5, D, 0, 5);
            CommonOps.insert(D6, D, 0, 6);
            CommonOps.insert(D7, D, 0, 7);
            CommonOps.insert(D8, D, 0, 8);
        } else{
            D = new DenseMatrix64F(x.numRows, 6);
            CommonOps.insert(D0, D, 0, 0);
            CommonOps.insert(D1, D, 0, 1);
            CommonOps.insert(D2, D, 0, 2);
            CommonOps.insert(D3, D, 0, 3);
            CommonOps.insert(D4, D, 0, 4);
            CommonOps.insert(D5, D, 0, 5);
        }

        return D;
    }

    /**
     * Calibrates all sensors paced in data
     * @param data [measrment index][sensor index][Floats of data]
     * @param offsets result offsets must be alloctated before hand
     * @param W_inverted result scaling must be allocated before hand
     */
    public void calibrateAllSensors(Vector<DenseMatrix64F> data, Vector<DenseMatrix64F> offsets, Vector<DenseMatrix64F> W_inverted){
        offsets.clear();
        W_inverted.clear();
        for(int i=0; i<data.size(); i++){
            DenseMatrix64F offs = new DenseMatrix64F(3,1);
            DenseMatrix64F Winv = new DenseMatrix64F(3,3);
            ellipsoidFitCalibration(data.get(i), offs, Winv);

            offsets.add(offs);
            W_inverted.add(Winv);
        }
    }

    // method for flushin calibration data to csv file
    public static void writeCalibDataToFile(Vector<DenseMatrix64F> offsets, Vector<DenseMatrix64F> scaling, File calibDataFile) throws IOException{
        PrintWriter pw = new PrintWriter(calibDataFile);
        pw.write(""+(offsets.size())+"\n");
        for(int i=0; i<offsets.size(); i++) {
            pw.print(offsets.get(i).get(0) + "," + offsets.get(i).get(1) + "," + offsets.get(i).get(2) + ",");
            for(int j=0; j<9; j++){
                pw.print(scaling.get(i).get(j));
                if(j<8){
                    pw.print(",");
                } else{
                    pw.print("\n");
                }
            }
        }
        pw.flush();
        pw.close();

    }
}
