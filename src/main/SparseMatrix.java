package main;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class SparseMatrix implements Matrix {
    public HashMap<Integer, HashMap<Integer, Double>> map;
    public int row;
    public int col;

    public SparseMatrix(String fileName) {
        col = 0;
        row = 0;
        HashMap<Integer, HashMap<Integer, Double>> result = new HashMap<Integer, HashMap<Integer, Double>>();
        try {
            File f = new File(fileName);
            Scanner input = new Scanner(f);
            String[] line = {};
            HashMap<Integer, Double> tmp = new HashMap<Integer, Double>();

            while (input.hasNextLine()) {
                tmp = new HashMap<Integer, Double
                        >();
                line = input.nextLine().split(" ");
                for (int i=0; i<line.length; i++) {
                    if (line[i]!="0") {
                        tmp.put(i, Double.parseDouble(line[i]));
                    }
                }
                if (tmp.size()!=0) {
                    result.put(row++, tmp);
                }
            }
            col = line.length;
            map = result;
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    public SparseMatrix(HashMap<Integer, HashMap<Integer, Double>> map, int row, int col) {
        this.map = map;
        this.row = row;
        this.col = col;
    }

    public Matrix mul(Matrix o) {
        if (o instanceof DenseMatrix) { return mul((DenseMatrix) o);
        }
        if (o instanceof SparseMatrix) { return mul((SparseMatrix) o);
        }
        else return null;
    }

    public SparseMatrix trans() {
        HashMap<Integer, HashMap<Integer, Double>> result = new HashMap<Integer, HashMap<Integer, Double>>();
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> row : map.entrySet()){
            for (HashMap.Entry<Integer, Double> elem : row.getValue().entrySet()) {
                if (!result.containsKey(elem.getKey())) {
                    result.put(elem.getKey(), new HashMap<Integer, Double>());
                }
                result.get(elem.getKey()).put(row.getKey(), elem.getValue());
            }
        }
        return new SparseMatrix(result, col, row);
    }

    private SparseMatrix mul(SparseMatrix s) {
        SparseMatrix sT = s.trans();
        HashMap<Integer, HashMap<Integer, Double>> result = new HashMap<Integer, HashMap<Integer, Double>>();
        double sum = 0;
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> row1 : map.entrySet()){
            for (HashMap.Entry<Integer, HashMap<Integer, Double>> row2 : sT.map.entrySet()) {
                for (HashMap.Entry<Integer, Double> elem : row1.getValue().entrySet()) {
                    if (row2.getValue().containsKey(elem.getKey())) {
                        sum += elem.getValue()*row2.getValue().get(elem.getKey());
                    }
                }
                if (sum != 0) {
                    if (!result.containsKey(row1.getKey())) {
                        result.put(row1.getKey(), new HashMap<Integer, Double>());
                    }
                    result.get(row1.getKey()).put(row2.getKey(), sum);
                }
                sum = 0;
            }
        }
        return new SparseMatrix(result, row, s.col);
    }

    private DenseMatrix mul(DenseMatrix d) {
        double[][] dT = d.trans();
        double[][] result = new double[row][dT.length];
        double sum = 0;
        for (Map.Entry<Integer, HashMap<Integer, Double>> row1 : map.entrySet()){
            for (int j = 0; j<dT.length; j++) {
                for (HashMap.Entry<Integer, Double> elem : row1.getValue().entrySet()) {
                    if (row1.getValue().containsKey(elem.getKey())) {
                        sum += elem.getValue()*dT[j][elem.getKey()];
                    }
                }
                result[row1.getKey()][j] = sum;
                sum = 0;
            }
        }
        return new DenseMatrix(result);
    }

    public void toFile(String filename) {
        try {
            PrintWriter w = new PrintWriter(filename);
            for (int i = 0; i<row; i++) {
                if (map.containsKey(i)) {
                    for (int j = 0; j<col; j++) {
                        if (map.get(i).containsKey(j)) {
                            w.print(map.get(i).get(j));
                        } else {
                            w.print((double)0);
                        }
                    }
                } else {
                    for (int j = 0; j < col; j++) {
                        w.print((double)0);
                    }
                    w.println();
                }
            }
            w.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }





    //---------------------------------------------
    public Matrix threadmul(Matrix o){
        final SparseMatrix s = (SparseMatrix)o;



        class Dispatcher {
            Iterator <Map.Entry<Integer, HashMap<Integer, Double>>> rrow = map.entrySet().iterator();
            public Map.Entry<Integer, HashMap<Integer, Double>> next(){
                return rrow.next();
            }
        }



        SparseMatrix transedMatrix = s.trans();
        ConcurrentHashMap<Integer, HashMap<Integer, Double>> cmap = new ConcurrentHashMap<Integer, HashMap<Integer, Double>>();
        for (int i = 0; i < row; i++) {
            if (map.containsKey(i)) {
                cmap.put(i, new HashMap<Integer, Double>(map.get(i)));
            }
        }

        ConcurrentHashMap<Integer, HashMap<Integer, Double>> maph = new ConcurrentHashMap<Integer, HashMap<Integer, Double>>();
        for (int i = 0; i < transedMatrix.row; i++) {
            if (transedMatrix.map.containsKey(i)) {
                maph.put(i, transedMatrix.map.get(i));
            }
        }

        final Dispatcher dispatcher = new Dispatcher();

        final HashMap<Integer, HashMap<Integer, Double>> result = new HashMap<Integer, HashMap<Integer, Double>>();
        ConcurrentHashMap<Integer, HashMap<Integer, Double>> cresult = new ConcurrentHashMap<Integer, HashMap<Integer, Double>>();
        Iterator<Map.Entry<Integer, HashMap<Integer, Double>>> iter = cresult.entrySet().iterator();
        double sum = 0;
        class MultRow implements Runnable {
            Thread thread;
            HashMap<Integer, Double> tmp = new HashMap<Integer, Double>();

            public MultRow(String s) {
                thread = new Thread(this, s);
            }



            public SparseMatrix mulPar(SparseMatrix s) {
                SparseMatrix sT = s.trans();
                HashMap<Integer, HashMap<Integer, Double>> cresult = new HashMap<Integer, HashMap<Integer, Double>>();
                double sum = 0;
                Map.Entry<Integer, HashMap<Integer, Double>> row1;
                while ( (row1 = dispatcher.next()) != null) {
                    for (HashMap.Entry<Integer, HashMap<Integer, Double>> row2 : sT.map.entrySet()) {
                        for (HashMap.Entry<Integer, Double> elem : row1.getValue().entrySet()) {
                            if (row2.getValue().containsKey(elem.getKey())) {
                                sum += elem.getValue() * row2.getValue().get(elem.getKey());
                            }
                        }
                        if (sum != 0) {
                            if (!result.containsKey(row1.getKey())) {
                                result.put(row1.getKey(), new HashMap<Integer, Double>());
                            }
                            result.get(row1.getKey()).put(row2.getKey(), sum);
                        }
                        sum = 0;
                    }
                }
                return new SparseMatrix(cresult, row, s.col);
            }

            public void run() {
                this.mulPar(s);
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

        HashMap<Integer, Double> tmp = new HashMap<Integer, Double>();
        for (ConcurrentHashMap.Entry<Integer, HashMap<Integer, Double>> row1 : cresult.entrySet()){
            tmp = new HashMap<Integer, Double>();
            for (HashMap.Entry<Integer, Double> row2 : row1.getValue().entrySet()) {
                tmp.put(row2.getKey(), row2.getValue());
            }
            result.put(row1.getKey(), tmp);
        }

        return new SparseMatrix(result, row, s.col);
    }

    @Override
    public boolean equals(Object o) {
        boolean y = true;
        if (o instanceof DenseMatrix) {
            DenseMatrix tmp = (DenseMatrix)o;
            if (tmp.data.length == row && tmp.data[0].length == col) {
                for (int i = 0; i<row; i++) {
                    if (map.containsKey(i)) {
                        for (int j = 0; j<col; j++) {
                            if (map.get(i).containsKey(j)) {
                                if (map.get(i).get(j) != tmp.data[i][j]) {
                                    y = false;
                                }
                            } else {
                                if (tmp.data[i][j] != 0) {
                                    y = false;
                                }
                            }
                        }
                    } else {
                        for (int j = 0; j < col; j++) {
                            if (tmp.data[i][j] != 0) {
                                y = false;
                            }
                        }
                    }
                }
            } else {
                y = false;
            }
        } else if (o instanceof SparseMatrix) {
            SparseMatrix tmp = (SparseMatrix) o;
            if (tmp.col == col && tmp.row == row) {
                for (int i = 0; i<row; i++) {
                    if (map.containsKey(i) && tmp.map.containsKey(i))  {
                        for (int j = 0; j<col; j++) {
                            if (map.get(i).containsKey(j) && tmp.map.get(i).containsKey(j)) {
                                if (map.get(i).get(j).doubleValue() != tmp.map.get(i).get(j).doubleValue()) {
                                    y = false;
                                }
                            } else if (map.get(i).containsKey(j) || tmp.map.get(i).containsKey(j)) {
                                y = false;
                            }
                        }
                    } else if (map.containsKey(i) || tmp.map.containsKey(i)) {
                        y = false;
                    }
                }
            } else {
                y = false;
            }
        }
        return y;
    }
}
