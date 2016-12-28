package main;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class DenseMatrix implements Matrix {
    public double[][] data;

    public DenseMatrix(String fileName) {
        try {
            File f = new File(fileName);
            Scanner input = new Scanner(f);
            String[] line;
            ArrayList<Double[]> a = new ArrayList<Double[]>();
            Double[] tmp = {};
            while (input.hasNextLine()) {
                line = input.nextLine().split(" ");
                tmp = new Double[line.length];
                for (int i=0; i<tmp.length; i++) {
                    tmp[i] = Double.parseDouble(line[i]);
                }
                a.add(tmp);
            }
            double[][] result = new double[a.size()][tmp.length];
            for (int i=0; i<result.length; i++) {
                for (int j=0; j<result[0].length; j++) {
                    result[i][j] = a.get(i)[j];
                }
            }
            data = result;
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    public DenseMatrix(double[][] r) {
        data = r;
    }

    public Matrix mul(Matrix o) {
        if (o instanceof DenseMatrix) { return mul((DenseMatrix) o);}
        if (o instanceof SparseMatrix) { return mul((SparseMatrix) o);}
        else return null;
    }

    public double[][] trans() {
        double[][] matrix = new double[data[0].length][data.length];
        for (int i = 0; i < data[0].length; i++ ) {
            for (int j = 0; j < data.length; j++ ) {
                matrix[i][j] = data[j][i];
            }
        }
        return matrix;
    }

    private DenseMatrix mul(DenseMatrix other) {
        double[][] o = other.trans();
        double[][] result = new double[data.length][o.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = new double[o.length];
        }

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < o.length; j++) {
                for (int k = 0; k < o[0].length; k++) {
                    result[i][j] += data[i][k] * o[j][k];
                }
            }
        }
        return new DenseMatrix(result);
    }

    private DenseMatrix mul(SparseMatrix s) {
        SparseMatrix sT = s.trans();
        double[][] result = new double[data.length][s.col];
        double sum = 0;
        for (int i = 0; i < data.length; i++){
            for (HashMap.Entry<Integer, HashMap<Integer, Double>> row2 : sT.map.entrySet()) {
                for (int k = 0; k < data[0].length; k++) {
                    if (row2.getValue().containsKey(k)) {
                        sum += data[i][k]*row2.getValue().get(k);
                    }
                }
                result[i][row2.getKey()] = sum;
                sum = 0;
            }
        }
        return new DenseMatrix(result);
    }

    public void toFile(String filename) {
        try {
            PrintWriter w = new PrintWriter(filename);
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[0].length; j++) {
                    w.print(data[i][j]+" ");

                }
                w.println();
            }
            w.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    //---------------------------------------------
    public DenseMatrix threadmul(Matrix o) {
        DenseMatrix object = (DenseMatrix)o;
        class Dispatcher {
            int value = 0;
            public int next() {
                synchronized (this) {
                    return value++;
                }
            }
        }

        final double[][] result = new double[data.length][object.data[0].length];
        final double[][] transedmatrix = object.trans();
        final Dispatcher dispatcher = new Dispatcher();

        class MultRow implements Runnable {
            Thread thread;

            public MultRow(String s) {
                thread = new Thread(this, s);
                thread.start();
            }
            public void run() {
                int i = dispatcher.next();
                for (int j = 0; j < transedmatrix.length; j++) {
                    for (int k = 0; k < transedmatrix[0].length; k++) {
                        result[i][j] += data[i][k] * transedmatrix[j][k];
                    }
                }
            }
        }

        MultRow one = new MultRow("one");
        MultRow two = new MultRow("two");
        MultRow three = new MultRow("three");
        MultRow four = new MultRow("four");

        try {
            one.thread.join();
            two.thread.join();
            three.thread.join();
            four.thread.join();

        } catch (InterruptedException e){
            e.printStackTrace();
        }

        return new DenseMatrix(result);
    }

    @Override
    public boolean equals(Object object) {
        boolean y = true;
        if (object instanceof DenseMatrix) {
            DenseMatrix tmp = (DenseMatrix)object;
            if (data.length == tmp.data.length && data[0].length == tmp.data[0].length) {
                for (int i = 0; i < data.length; i++) {
                    for (int j=0; j < data[0].length; j++) {
                        if (data[i][j] != tmp.data[i][j]) {
                            y = false;
                        }
                    }
                }
            } else {
                y = false;
            }
        } else if (object instanceof SparseMatrix) {
            SparseMatrix tmp = (SparseMatrix)object;
            if (data.length == tmp.row && data[0].length == tmp.col) {
                for (int i = 0; i<tmp.row; i++) {
                    if (tmp.map.containsKey(i)) {
                        for (int j = 0; j<tmp.col; j++) {
                            if (tmp.map.get(i).containsKey(j)) {
                                if (tmp.map.get(i).get(j) != data[i][j]) {
                                    y = false;
                                }
                            } else {
                                if (data[i][j] != 0) {
                                    y = false;
                                }
                            }
                        }
                    } else {
                        for (int j = 0; j < tmp.col; j++) {
                            if (data[i][j] != 0) {
                                y = false;
                            }
                        }
                    }
                }
            } else {
                y = false;
            }
        }
        return y;
    }
}