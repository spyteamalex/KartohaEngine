package utils;

public class Math {
    public static double destroyMinusZeros(double d){
        if(d < 0.0000001 && d > -0.0000001){
            return 0;
        }
        return  d;
    }
}