package sift;


/**
 * 类floatArray.java的实现描述：一个用于存储float的一维数组，可以快速地直接访问数组元素。
 * @author
 */
public abstract class FloatArray {
    public float[] data;                     //公开为public可以直接访问而不是在访问大量的数据元素时需要大量的getter方法调用
    public abstract FloatArray clone();
}
