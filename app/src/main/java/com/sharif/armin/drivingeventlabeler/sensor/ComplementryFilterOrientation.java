package com.sharif.armin.drivingeventlabeler.sensor;

import android.hardware.SensorManager;
import java.util.Arrays;
import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import com.sharif.armin.drivingeventlabeler.util.Utils;

public class ComplementryFilterOrientation {
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float EPSILON = 0.000000001f;
    public  float FILTER_COEFICIENT = 0.2f;
    private long prevTime = 0;
    private Quaternion gyroRotationVector;
    private float[] orientation = new float[3];

    public float[] calculateOrientation(long time, float[] rac, float[] gyr, float[] mgm){
        if (isInitialized()) {
            if (prevTime != 0){
                float deltaT = (time - prevTime) * NS2S;
                Quaternion accMgmRotationVector = getAccMgmOrientationVector(rac, mgm);
                if (accMgmRotationVector != null){
                    gyroRotationVector = integrateGyroRotationVector(gyroRotationVector, gyr, deltaT);
                    gyroRotationVector = (gyroRotationVector.multiply(FILTER_COEFICIENT)).add(
                            accMgmRotationVector.multiply(1.0f-FILTER_COEFICIENT)
                    );

                }
                Rotation rotation = new Rotation(gyroRotationVector.getQ0(), gyroRotationVector.getQ1(),
                        gyroRotationVector.getQ2(), gyroRotationVector.getQ3(),
                        true);
                orientation =  Utils.toFloatArray(rotation.getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR));

            }
            this.prevTime = time;
            return orientation;

        }else {
            throw new IllegalStateException("Initialize rotation vector!");
        }
    }

    public float[] getRotationVector(){
        double[] tmp = new double[] {gyroRotationVector.getQ1(), gyroRotationVector.getQ2(),
                                     gyroRotationVector.getQ3(), gyroRotationVector.getQ0()};
        return Utils.toFloatArray(tmp);
    }

    public void reset(){
        prevTime = 0;
        gyroRotationVector = null;
    }
    public boolean isInitialized(){
        return (gyroRotationVector != null);
    }

    public void setFilterCoefiecient(float fc){
        this.FILTER_COEFICIENT = fc;
    }

    public void setInitialOrientation(Quaternion initialOrientation) {
        this.gyroRotationVector = initialOrientation;
    }

    public static Quaternion integrateGyroRotationVector(Quaternion prevRot, float[] gyr, float dt){
        float magnitude = (float) Math.sqrt(gyr[0] * gyr[0] + gyr[1] * gyr[1] + gyr[2] * gyr[2]);
        if (magnitude > EPSILON){
            gyr[0] /= magnitude;
            gyr[1] /= magnitude;
            gyr[2] /= magnitude;
        }
        float thetaOverTwo = magnitude * dt / 2.0f;
        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
        double[] deltaVector = new double[4];
        deltaVector[0] = sinThetaOverTwo * gyr[0];
        deltaVector[1] = sinThetaOverTwo * gyr[1];
        deltaVector[2] = sinThetaOverTwo * gyr[2];
        deltaVector[3] = cosThetaOverTwo;
        return prevRot.multiply(new Quaternion(deltaVector[3], Arrays.copyOfRange(deltaVector, 0, 3)));
    }

    public static Quaternion getAccMgmOrientationVector(float[] rac, float[] mgm){
        float[] rotationMatrix = new float[9];
        if (SensorManager.getRotationMatrix(rotationMatrix, null, rac, mgm)){
            float[] rotatinVector = new float[3];
            SensorManager.getOrientation(rotationMatrix, rotatinVector);
            Rotation rotation = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR,
                    rotatinVector[1], -rotatinVector[2], rotatinVector[0]);
            return new Quaternion(rotation.getQ0(), rotation.getQ1(),rotation.getQ2(),rotation.getQ3());
        }
        return null;
    }


}
