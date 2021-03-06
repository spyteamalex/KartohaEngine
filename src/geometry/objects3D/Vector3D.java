package geometry.objects3D;

import utils.throwables.ImpossibleVectorException;

import java.util.Objects;

public class Vector3D {
    public final double x, y, z;

    /**
     * Конструктор вектора по координатам
     * @param x x-координата
     * @param y y-координата
     * @param z z-координата
     */
    public Vector3D(double x, double y, double z) {
        this.x = utils.Math.roundNearZero(x);
        this.y = utils.Math.roundNearZero(y);
        this.z = utils.Math.roundNearZero(z);
    }

    /**
     * Конструктор вектора от начальной до конечной точки
     * @param p1 Начальная точка
     * @param p2 Конечная точка
     */
    public Vector3D(Point3D p1, Point3D p2) {
        this.x = utils.Math.roundNearZero(p2.x - p1.x);
        this.y = utils.Math.roundNearZero(p2.y - p1.y);
        this.z = utils.Math.roundNearZero(p2.z - p1.z);
    }

    /**
     * @param a Начальная точка
     * @return Точка, вектор от начальной до которой равен данному вектору
     */
    public Point3D addToPoint(Point3D a) {
        return new Point3D(a.x + x, a.y + y, a.z + z);
    }

    /**
     * @param d Коэффициент
     * @return Вектор, равный данному, умноженному на коэффициент
     */
    public Vector3D multiply(double d) {
        return new Vector3D(x * d, y * d, z * d);
    }

    /**
     * @param v Вектор
     * @return Вектор, равный сумме данного и текущего
     */
    public Vector3D add(Vector3D v) {
        return new Vector3D(x + v.x, y + v.y, z + v.z);
    }

    /**
     * @param v Вектор
     * @return Вектор, равнй разности текущего и данного
     */
    public Vector3D subtract(Vector3D v) {
        return new Vector3D(x - v.x, y - v.y, z - v.z);
    }

    /**
     * @return Длина вектора
     */
    public double getLength() {
        return utils.Math.roundNearZero(Math.sqrt(x * x + y * y + z * z));
    }

    /**
     * @param v Вектор
     * @return Скалярное произведение данного и текущего вектора
     */
    public double scalarProduct(Vector3D v) {
        return utils.Math.roundNearZero(x * v.x + y * v.y + z * v.z);
    }

    /**
     * @param r Ось поворота
     * @return Вектор, повернутый вокруг оси поворота на угол, равный длине оси
     */
    public Vector3D rotate(Vector3D r) {
        Vector3D e = r.normalize();
        double t = r.getLength();
        // v2 = v*cos(t)+sin(t)*(e×v)+(1-cos(t))(e·v)e - Формула поворота Родрига
        Vector3D res = this.multiply(Math.cos(t));
        res = res.add(e.vectorProduct(this).multiply(Math.sin(t)));
        res = res.add(e.multiply(e.scalarProduct(this)).multiply(1 - Math.cos(t)));
        return res;
    }

    /**
     * @param v Вектор
     * @return Векторное произведение текущего и данного вектора
     */
    public Vector3D vectorProduct(Vector3D v) {
        double A = y * v.z - v.y * z,
                B = v.x * z - v.z * x,
                C = x * v.y - v.x * y;

        return new Vector3D(A, B, C);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector3D vector3D = (Vector3D) o;
        return utils.Math.roundNearZero(vector3D.x - x) == 0 &&
                utils.Math.roundNearZero(vector3D.y - y) == 0 &&
                utils.Math.roundNearZero(vector3D.z - z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    /**
     * @return Единичный вектор того же направления
     */
    public Vector3D normalize() {
        double l = getLength();
        if (l == 0) {
            throw new ImpossibleVectorException();
        }
        return new Vector3D(x / l, y / l, z / l);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }
}

