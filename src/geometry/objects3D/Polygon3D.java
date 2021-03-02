package geometry.objects3D;

import utils.throwables.ImpossiblePlaneException;
import utils.throwables.ImpossiblePolygonException;

import java.awt.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Polygon3D implements Object3D {
    public final Point3D a1, a2, a3;
    public final Color color;

    public Polygon3D(Point3D a1, Point3D a2, Point3D a3, Color color) {
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.color = color;
        try{
            getPlane();
        }catch (ImpossiblePlaneException e){
            throw new ImpossiblePolygonException();
        }
        if(a1.equals(a2) || a1.equals(a3) || a2.equals(a3))
            throw new ImpossiblePolygonException();
    }

    public Polygon3D from(Point3D p){
        return new Polygon3D(a1.from(p), a2.from(p), a3.from(p), color);
    }

    public Plane3D getPlane(){
        return new Plane3D(a1,a2,a3);
    }

    public Polygon3D rotate(Vector3D v, Point3D p){
        return new Polygon3D(a1.rotate(v,p), a2.rotate(v,p), a3.rotate(v,p), color);
    }

    @Override
    public Region3D getRegion() {
        return new Region3D(
                new Point3D(min(min(a1.x, a2.x), a3.x), min(min(a1.y, a2.y), a3.y), min(min(a1.z, a2.z), a3.z)),
                new Point3D(max(max(a1.x, a2.x), a3.x), max(max(a1.y, a2.y), a3.y), max(max(a1.z, a2.z), a3.z)));
    }

    @Override
    public String toString() {
        return String.format("[%s, %s, %s]", a1, a2, a3);
    }
}
