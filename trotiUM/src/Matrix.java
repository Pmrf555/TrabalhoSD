import java.io.Serializable;

import Utilities.Pair;

public class Matrix<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private T[][] matrix;
    private Integer dimension;

    @SuppressWarnings("unchecked")
    public Matrix(Integer dimension){
        this.dimension = dimension;
        this.matrix = (T[][]) new Object[dimension][dimension];
    }

    public T get(Integer x, Integer y) throws IndexOutOfBoundsException{
        if(x >= this.dimension || y >= this.dimension)
            throw new IndexOutOfBoundsException("Index out of bounds: " + x + ", " + y);
        return this.matrix[x][y];
    }

    public boolean contains(T object){
        for(int i = 0; i < this.dimension; i++){
            for(int j = 0; j < this.dimension; j++){
                if(this.matrix[i][j].equals(object))
                    return true;
            }
        }
        return false;
    }

    public void set(Integer x, Integer y, T value) throws IndexOutOfBoundsException{
        if(x >= this.dimension || y >= this.dimension)
            throw new IndexOutOfBoundsException("Index out of bounds: " + x + ", " + y);
        this.matrix[x][y] = value;
    }

    public Integer dimension(){
        return this.dimension;
    }

    public String toString(){
        String res = "";
        for(int i = 0; i < this.dimension; i++){
            for(int j = 0; j < this.dimension; j++){
                res += this.matrix[i][j] + " ";
            }
            res += "\n";
        }
        return res;
    }

    public static Integer manhattan(Integer x1, Integer y1, Integer x2, Integer y2){
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    public static void main(String[] args){
        Integer radius = 2;
        Matrix<Integer> m = new Matrix<Integer>(2*radius + 1);
        Pair<Integer,Integer> pos = new Pair<Integer,Integer>(2,2); 
        for(int i = pos.getL()-radius;i<=pos.getL()+radius;i++){
            for(int j = pos.getR()-radius;j<=pos.getR()+radius;j++){
                Integer distance = Matrix.manhattan(pos.getL(), pos.getR(), i, j);
                if (distance <= radius)
                    m.set(i, j, distance);
                else
                    m.set(i, j, 0);
            }
        }
        System.out.println(m);
    }
}
