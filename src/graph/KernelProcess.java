package graph;

import com.aparapi.Kernel;
import geometry.objects3D.Vector3D;
import javafx.util.Pair;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public class KernelProcess extends Kernel {

    double[] focus, screenVector, screenPoint, bH, bW, res;
    double[] resultX, resultY, resultZ, x, y, z;
    double[] bufferX2D, bufferY2D;
    double[] bufferX, bufferY, bufferZ;
    int[] imageData;
    int[] prefix, minX;
    double[] depth;
    int[] colors;
    int count = 0;
    BufferedImage image;

    KernelProcess(Camera c, int count, BufferedImage image){
        this.count = count;
        x = new double[count*3];
        y = new double[count*3];
        z = new double[count*3];
        colors = new int[count];
        resultX = new double[count];
        resultY = new double[count];
        resultZ = new double[count];
        bufferX2D = new double[3*count];
        bufferY2D = new double[3*count];
        bufferX = new double[3*count];
        bufferY = new double[3*count];
        bufferZ = new double[3*count];
        prefix = new int[count+1];
        minX = new int[count];

        put(x);
        put(y);
        put(z);
        put(colors);
        put(resultX);
        put(resultY);
        put(resultZ);
        put(bufferX2D);
        put(bufferY2D);
        put(bufferX);
        put(bufferY);
        put(bufferZ);
        put(minX);



        this.focus = new double[3];
        this.res = new double[2];
        this.bW = new double[3];
        this.bH = new double[3];
        this.screenPoint = new double[3];
        this.screenVector = new double[3];
        depth = new double[(int)c.getResolution().height*(int)c.getResolution().width];

        System.out.println((int)c.getResolution().height*(int)c.getResolution().width*Integer.BYTES);

        setCamera(c, image);
        setExplicit(true);

    }

    public BufferedImage get(){
        if(isExplicit()){
            get(imageData);
        }
        return image;
    }

    public void setCamera(Camera c, BufferedImage image){
        this.focus[0] = c.getScreen().focus.x;
        this.focus[1] = c.getScreen().focus.y;
        this.focus[2] = c.getScreen().focus.z;


        Pair<Vector3D, Vector3D> basises = c.getBasises(c.getResolution().width / 2, c.getResolution().height / 2);
        Vector3D bW = basises.getKey(),
                bH = basises.getValue();


        this.res[0] = c.getResolution().width;
        this.res[1] = c.getResolution().height;

        this.bW[0] = bW.x;
        this.bW[1] = bW.y;
        this.bW[2] = bW.z;


        this.bH[0] = bH.x;
        this.bH[1] = bH.y;
        this.bH[2] = bH.z;

        this.screenPoint[0] = c.getScreen().point.x;
        this.screenPoint[1] = c.getScreen().point.y;
        this.screenPoint[2] = c.getScreen().point.z;

        this.screenVector[0] = c.getScreen().vector.x;
        this.screenVector[1] = c.getScreen().vector.y;
        this.screenVector[2] = c.getScreen().vector.z;
//        depth = new double[(int)c.getResolution().height*(int)c.getResolution().width];

        Arrays.fill(depth, Integer.MAX_VALUE);
        Arrays.fill(prefix, 0);

        this.image = image;
        imageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        Arrays.fill(imageData, 0);

        put(imageData);
        put(focus);
        put(res);
        put(this.bW);
        put(this.bH);
        put(screenPoint);
        put(screenVector);
        put(depth);



        int sum = 0;
        for(int i = 0; i <= count; i++) {
            prefix[i] = sum;
            if(i < count) {
                double minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                if (project(3*i, x[i * 3], y[i * 3], z[i * 3])) {
                    minX = min(minX, bufferX2D[i*3]);
                    maxX = max(maxX, bufferX2D[i*3]);
                } else continue;
                if (project(i*3+1, x[i * 3 + 1], y[i * 3 + 1], z[i * 3 + 1])) {
                    minX = min(minX, bufferX2D[i*3+1]);
                    maxX = max(maxX, bufferX2D[i*3+1]);
                } else continue;
                if (project(i*3+2, x[i * 3 + 2], y[i * 3 + 2], z[i * 3 + 2])) {
                    minX = min(minX, bufferX2D[i*3+2]);
                    maxX = max(maxX, bufferX2D[i*3+2]);
                } else continue;

                sum += ceil(maxX) - floor(minX);
                this.minX[i] = (int)floor(minX);
            }
        }

        put(prefix);
    }
    
    public boolean inRegion(double r1x, double r1y, double r1z, double r2x, double r2y, double r2z, double x, double y, double z){
        double lx = min(r1x, r2x);
        double ly = min(r1y, r2y);
        double lz = min(r1z, r2z);
        double hx = max(r1x, r2x);
        double hy = max(r1y, r2y);
        double hz = max(r1z, r2z);

        return lx <= x && ly <= y && lz <= z && hx >= x && hy >= y && hz >= z;
        
    }

    public double getDepth(double d1, double d2, double L, double l){
        return sqrt(((l-L)*(L*l-d1*d1)+l*d2*d2)/L);
    }

    public double getValue(double x1, double y1, double x2, double y2, double x3){
        return (y2-y1)*(x3-x1)/(x2-x1)+y1;
    }

    public double getDistance2D(double x1, double y1, double x2, double y2){
        return sqrt(pow(x2-x1, 2)+pow(y2-y1, 2));
    }

    public double getDistance3D(double x1, double y1, double z1, double x2, double y2, double z2){
        return sqrt(pow(x2-x1, 2)+pow(y2-y1, 2)+pow(z2-z1, 2));
    }

    public boolean getIntersection(int gid, double x1, double y1, double z1, double x2, double y2, double z2){

        double vectorX = x2-x1,
                vectorY = y2-y1,
                vectorZ = z2-z1;
        double sp = vectorX*screenVector[0]+vectorY*screenVector[1]+vectorZ*screenVector[2];
        if (sp != 0) {
            double d = -(screenVector[0] * screenPoint[0] + screenVector[1] * screenPoint[1] + screenVector[2] * screenPoint[2]);
            double t = -(d + x1 * screenVector[0] + y1 * screenVector[1] + z1 * screenVector[2]) / sp;
            bufferX[gid] = vectorX*t+x1;
            bufferY[gid] = vectorY*t+y1;
            bufferZ[gid] = vectorZ*t+z1;
            return true;
        }
        return false;
    }

    public boolean project(int gid, double px, double py, double pz){
        if((focus[0] != px || focus[1] != py || focus[2] != pz) && getIntersection(gid, focus[0], focus[1], focus[2], px, py,pz) && !inRegion(px,py,pz, bufferX[gid], bufferY[gid], bufferZ[gid], focus[0], focus[1], focus[2])) {
            double projectionX = bufferX[gid],
                    projectionY = bufferY[gid],
                    projectionZ = bufferZ[gid];
            double smmX = screenPoint[0];
            double smmY = screenPoint[1];
            double smmZ = screenPoint[2];
            double roW = 0, roH = 0;
            boolean roWNotNull = false, roHNotNull = false;
            if (bH[1] != 0 && bH[2] != 0 && bW[1] != 0 && bW[2] != 0) {
                roW = (bH[2] * (projectionY - smmY) - bH[1] * (projectionZ - smmZ)) / (bH[2] * bW[1] - bH[1] * bW[2]);
                roH = (projectionZ - smmZ - bW[2] * roW) / bH[2];
                roHNotNull = true;
                roWNotNull = true;
            } else if (bH[0] != 0 && bH[2] != 0 && bW[0] != 0 && bW[2] != 0) {
                roW = (bH[2] * (projectionX - smmX) - bH[0] * (projectionZ - smmZ)) / (bH[2] * bW[0] - bH[0] * bW[2]);
                roH = (projectionX - smmX - bW[0] * roW) / bH[0];
                roHNotNull = true;
                roWNotNull = true;
            } else if (bH[0] != 0 && bH[1] != 0 && bW[0] != 0 && bW[1] != 0) {
                roW = (bH[1] * (projectionX - smmX) - bH[0] * (projectionY - smmY)) / (bH[1] * bW[0] - bH[0] * bW[1]);
                roH = (projectionX - smmX - bW[0] * roW) / bH[0];
                roHNotNull = true;
                roWNotNull = true;
            } else {
                if (bH[0] == 0 && bW[0] != 0) {
                    roW = (projectionX - smmX) / bW[0];
                    roWNotNull = true;
                } else if (bH[1] == 0 && bW[1] != 0) {
                    roW = (projectionY - smmY) / bW[1];
                    roWNotNull = true;
                } else if (bH[2] == 0 && bW[2] != 0) {
                    roW = (projectionZ - smmZ) / bW[2];
                    roWNotNull = true;
                }

                if (bH[1] != 0 && bW[1] == 0) {
                    roH = (projectionY - smmY) / bH[1];
                    roHNotNull = true;
                } else if (bH[0] != 0 && bW[0] == 0) {
                    roH = (projectionX - smmX) / bH[0];
                    roHNotNull = true;
                } else if (bH[2] != 0 && bW[2] == 0) {
                    roH = (projectionZ - smmZ) / bH[2];
                    roHNotNull = true;
                }

                if (!roWNotNull && roHNotNull) {
                    if (bW[0] != 0) {
                        roW = (projectionX - smmX - bH[0] * roH) / bW[0];
                        roWNotNull = true;
                    } else if (bW[1] != 0) {
                        roW = (projectionY - smmY - bH[1] * roH) / bW[1];
                        roWNotNull = true;
                    } else if (bW[2] != 0) {
                        roW = (projectionZ - smmZ - bH[2] * roH) / bW[2];
                        roWNotNull = true;
                    }
                }

                if (!roHNotNull && roWNotNull) {
                    if (bH[0] != 0) {
                        roH = (projectionX - smmX - bW[0] * roW) / bH[0];
                        roHNotNull = true;
                    } else if (bH[1] != 0) {
                        roH = (projectionY - smmY - bW[1] * roW) / bH[1];
                        roHNotNull = true;
                    } else if (bH[2] != 0) {
                        roH = (projectionZ - smmZ - bW[2] * roW) / bH[2];
                        roHNotNull = true;
                    }
                }
            }
            if(roHNotNull && roWNotNull){
                bufferX2D[gid] = roW * res[0] + res[0] / 2;
                bufferY2D[gid] = -roH * res[1] + res[1] / 2;
                return true;
            }
        }
        return false;
    }
//
//    public void draw() {
//        Optional<Point2D> a12D = project(a1),
//                a22D = project(a2),
//                a32D = project(a3);
//        if (!a12D.isPresent() || !a22D.isPresent() || !a32D.isPresent() || !new Polygon2D(a12D.get(), a22D.get(), a32D.get(), color).getRegion().crosses(new Region2D(new Point2D(0, 0), new Point2D(camera.getResolution().width, camera.getResolution().height))))
//            return;
//
////        cp.set((int) a12D.get().x, (int) a12D.get().y, new Pixel(1, color));
////        cp.set((int) a22D.get().x, (int) a22D.get().y, new Pixel(1, color));
////        cp.set((int) a32D.get().x, (int) a32D.get().y, new Pixel(1, color));
////        new Polygon2D(a12D.get(), a22D.get(), a32D.get(), color).draw(cp, camera);
//        Vector2D v232D = new Vector2D(a22D.get(), a32D.get());
//        Vector3D v23 = new Vector3D(a2, a3);
//        for (double j = 0; j <= v232D.getLength(); j += 0.5) {
//            Point3D p = v23.multiply(j / v232D.getLength()).addToPoint(a2);
//            Vector3D v1p = new Vector3D(a1, p);
//            Point2D p2D = v232D.multiply(j / v232D.getLength()).addToPoint(a22D.get());
//            Vector2D v1p2D = new Vector2D(a12D.get(), p2D);
//            for (double i = 0; i <= v1p2D.getLength(); i += 0.5) {
//                Point3D p2 = v1p.multiply(i / v1p2D.getLength()).addToPoint(a1);
//                Point2D p22D = v1p2D.multiply(i / v1p2D.getLength()).addToPoint(a12D.get());
//                cp.set((int) p22D.x, (int) p22D.y, new Pixel(new Vector3D(camera.getScreen().focus, p2).getLength(), color));
//            }
//        }
//    }

    public int getPolyIndex(int i){
        int min = 0,
            max = count;
        while (max - min > 1){
            int mid = (max+min)/2;
            if(prefix[mid] > i){
                max = mid;
            }else{
                min = mid;
            }
        }
        return min;
    }


    @Override
    public void run() {
        run(getGlobalId());
    }

    public void run(int gid) {
        int poly = getPolyIndex(gid);
        int i = gid - prefix[poly] + minX[poly];
        double a1x = 0, a1y = 0, a1z = 0, a12Dx = 0, a12Dy = 0,
                a2x = 0, a2y = 0, a2z = 0, a22Dx = 0, a22Dy = 0,
                a3x = 0, a3y = 0, a3z = 0, a32Dx = 0, a32Dy = 0,
                a1depth = 0, a2depth = 0, a3depth = 0;
        if (project(3 * poly, x[poly * 3], y[poly * 3], z[poly * 3])) {
            a1x = bufferX[3 * poly];
            a1y = bufferY[3 * poly];
            a1z = bufferZ[3 * poly];
            a12Dx = bufferX2D[3 * poly];
            a12Dy = bufferY2D[3 * poly];
            a1depth = getDistance3D(a1x, a1y, a1z, focus[0], focus[1], focus[2]);
        } else return;
        if (project(poly * 3 + 1, x[poly * 3 + 1], y[poly * 3 + 1], z[poly * 3 + 1])) {
            a2x = bufferX[poly * 3 + 1];
            a2y = bufferY[poly * 3 + 1];
            a2z = bufferZ[poly * 3 + 1];
            a22Dx = bufferX2D[poly * 3 + 1];
            a22Dy = bufferY2D[poly * 3 + 1];
            a2depth = getDistance3D(a2x, a2y, a2z, focus[0], focus[1], focus[2]);
        } else return;
        if (project(poly * 3 + 2, x[poly * 3 + 2], y[poly * 3 + 2], z[poly * 3 + 2])) {
            a3x = bufferX[poly * 3 + 2];
            a3y = bufferY[poly * 3 + 2];
            a3z = bufferZ[poly * 3 + 2];
            a32Dx = bufferX2D[poly * 3 + 2];
            a32Dy = bufferY2D[poly * 3 + 2];
            a3depth = getDistance3D(a3x, a3y, a3z, focus[0], focus[1], focus[2]);
        } else return;

        double maxPointY = 0, maxPointDepth = -1, minPointY = 0, minPointDepth = -1;
        if (i >= min(a22Dx, a12Dx) && i <= max(a22Dx, a12Dx)) {
            double y = getValue(a12Dx, a12Dy, a22Dx, a22Dy, i),
                    dis = getDistance2D(a12Dx, a12Dy, a22Dx, a22Dy);
            if (dis != 0 && (maxPointDepth == -1 || y > maxPointY)) {
                maxPointY = y;
                maxPointDepth = getDepth(a1depth, a2depth, dis, getDistance2D(a12Dx, a12Dy, i, y));
            }
            if (dis != 0 && (minPointDepth == -1 || y < minPointY)) {
                minPointY = y;
                minPointDepth = getDepth(a1depth, a2depth, dis, getDistance2D(a12Dx, a12Dy, i, y));
            }
        }
        if (i >= min(a22Dx, a32Dx) && i <= max(a22Dx, a32Dx)) {
            double y = getValue(a32Dx, a32Dy, a22Dx, a22Dy, i),
                    dis = getDistance2D(a32Dx, a32Dy, a22Dx, a22Dy);
            if (dis != 0 && (maxPointDepth == -1 || y > maxPointY)) {
                maxPointY = y;
                maxPointDepth = getDepth(a3depth, a2depth, dis, getDistance2D(a32Dx, a32Dy, i, y));
            }
            if (dis != 0 && (minPointDepth == -1 || y < maxPointY)) {
                minPointY = y;
                minPointDepth = getDepth(a3depth, a2depth, dis, getDistance2D(a32Dx, a32Dy, i, y));
            }
        }
        if (i >= min(a32Dx, a12Dx) && i <= max(a32Dx, a12Dx)) {
            double y = getValue(a12Dx, a12Dy, a32Dx, a32Dy, i),
                    dis = getDistance2D(a12Dx, a12Dy, a32Dx, a32Dy);
            if (dis != 0 && (maxPointDepth == -1 || y > maxPointY)) {
                maxPointY = y;
                maxPointDepth = getDepth(a1depth, a3depth, dis, getDistance2D(a12Dx, a12Dy, i, y));
            }
            if (dis != 0 && (minPointDepth == -1 || y < minPointY)) {
                minPointY = y;
                minPointDepth = getDepth(a1depth, a3depth, dis, getDistance2D(a12Dx, a12Dy, i, y));
            }
        }

        if (maxPointDepth != -1 && minPointDepth != -1) {
//            maxPointY = max(0, min(res[1], maxPointY));
//        minPointY = max(0, min(res[1], minPointY));
            for (int j = (int) floor(minPointY); j <= (int) ceil(maxPointY); j++) {
                double d = getDepth(minPointDepth, maxPointDepth, maxPointY - minPointY, j - minPointY);
                int index = j * (int) res[0] + i;
                if (i >= 0 && i < res[0] && j >= 0 && j < res[1] && d < depth[index]) {
                    imageData[index] = colors[poly];
                    depth[index] = d;
                }

//                v23Len = Math.sqrt(v23x*v23x + v23y*v23y + v23z*v23z);

            }
        }
    }

//    @Override
//    public void run() {
//        int gid = getGlobalId();
//        int r = colors[gid*3],
//                g = colors[gid*3+1],
//                b = colors[gid*3+2];
//        if(project(gid, x[gid*3], y[gid*3], z[gid*3]) && bufferX2D[gid] >= 0 && bufferX2D[gid] < res[0] && bufferY2D[gid] >= 0 && bufferY2D[gid] <= res[1]) {
//            int i = (int)bufferY2D[gid]*(int)res[0]+(int)bufferX2D[gid];
//            imageData[i]=r*256*256+g*256+b;
////            result[3*i+1]=g;
////            result[3*i+2]=b;
//        }
//        if(project(gid, x[gid*3+1], y[gid*3+1], z[gid*3+1]) && bufferX2D[gid] >= 0 && bufferX2D[gid] < res[0] && bufferY2D[gid] >= 0 && bufferY2D[gid] <= res[1]) {
//            int i = (int)bufferY2D[gid]*(int)res[0]+(int)bufferX2D[gid];
//            imageData[i]=r*256*256+g*256+b;
//        }
//        if(project(gid, x[gid*3+2], y[gid*3+2], z[gid*3+2]) && bufferX2D[gid] >= 0 && bufferX2D[gid] < res[0] && bufferY2D[gid] >= 0 && bufferY2D[gid] <= res[1]) {
//            int i = (int)bufferY2D[gid]*(int)res[0]+(int)bufferX2D[gid];
//            imageData[i]=r*256*256+g*256+b;
//        }
//    }
}
