import java.util.concurrent.locks.*;

import Utilities.Matrix;
import Utilities.Pair;
import Utilities.Rand;

import java.util.ArrayList;
import java.util.List;

public class State {
    private static final Integer MAX_SCOOTERS_PER_SQUARE = 3;
    private static final Integer MIN_SCOOTERS_PER_SQUARE = 1;
    private static final Integer AVERAGE_SCOOTERS_PER_SQUARE = State.MAX_SCOOTERS_PER_SQUARE - State.MIN_SCOOTERS_PER_SQUARE;
    private Integer gridDimension;
    private Integer radius;
    private Integer totalScooters;
    private Lock lock = new ReentrantLock();
    private ArrayList<Scooter> scooterList;
    private Matrix<List<Scooter>> grid;
    private Matrix<Integer> scooterDistribution;
    private Matrix<Integer> scooterRewards;


    public State(Integer gridDimension, Integer radius, Integer totalScooters){
        this.totalScooters = totalScooters;
        this.gridDimension = gridDimension;
        this.radius = radius;
        this.scooterList = new ArrayList<Scooter>();
        this.grid = new Matrix<List<Scooter>>(gridDimension);
        this.scooterDistribution = new Matrix<Integer>(gridDimension);
        this.scooterRewards = new Matrix<Integer>(gridDimension);
        this.generateScooters();
        this.generateMap();
        this.updateDistribution();
        this.generateRewards();
    }

    public State(){
        this(1,1,1);
    }

    public Integer dimension(){
        return this.gridDimension;
    }

    public Integer radius(){
        return this.radius;
    }

    public Integer totalScooters(){
        return this.totalScooters;
    }

    public void setTotalScooters(Integer totalScooters){
        this.totalScooters = totalScooters;
    }

    public ArrayList<Scooter> scooterList(){
        return this.scooterList;
    }

    public Matrix<Integer> rewards(){
        return this.scooterRewards;
    }

    public Matrix<List<Scooter>> grid(){
        return this.grid;
    }

    public void setRadius(Integer radius){
        this.radius = radius;
    }

    public List<Scooter> getScootersInPos(Integer x, Integer y){
        return this.grid.get(x,y);
    }

    public void setReward(Integer x, Integer y, Integer reward){
        this.scooterRewards.set(x, y, reward);
    }

    public Integer getReward(Integer x, Integer y){
        return this.scooterRewards.get(x, y);
    }

    public void generateScooters(){
        for(int i = 0; i < this.totalScooters; i++){
            Integer x = Rand.randInt(0, this.gridDimension - 1);
            Integer y = Rand.randInt(0, this.gridDimension - 1);
            this.addScooter(new Scooter(x,y));
        }
    }

    public void generateMap(){
        for(Scooter scooter : this.scooterList){
            Pair<Integer, Integer> pos = scooter.getPos();
            this.addScooterToGrid(scooter, pos.getL(), pos.getR());
        }
    }

    public void addScooter(Scooter scooter){
        this.lock.lock();
        try{
            this.scooterList.add(scooter);
            //Pair<Integer, Integer> pos = scooter.getPos();
            //this.addScooterToGrid(scooter, pos.getL(), pos.getR());
        }
        finally{
            this.lock.unlock();
        }
    }

    public void addScooterToGrid(Scooter scooter, Integer x, Integer y){
        this.lock.lock();
        try{
            List<Scooter> scooterList = this.grid.get(x,y);
            if(scooterList == null){
                scooterList = new ArrayList<Scooter>();
            }
            scooterList.add(scooter);
            this.grid.set(x, y, scooterList);
        }
        finally{
            this.lock.unlock();
        }
    }

    public void removeScooterFromGrid(Scooter scooter, Integer x, Integer y){
        this.lock.lock();
        try{
            List<Scooter> scooterList = this.grid.get(x,y);
            if(scooterList != null){
                scooterList.remove(scooter);
                this.grid.set(x, y, scooterList);
                scooter.setPos(-1,-1);
            }
        }
        finally{
            this.lock.unlock();
        }
    }

    public void removeScooter(Scooter scooter){
        this.lock.lock();
        try{
            this.scooterList.remove(scooter);
            scooter.setPos(-1,-1);
        }
        finally{
            this.lock.unlock();
        }
    }

    public ArrayList<Pair<Integer,Integer>> getPositionsInRange(Integer x, Integer y){
        ArrayList<Pair<Integer,Integer>> positions = new ArrayList<Pair<Integer,Integer>>();
        this.lock.lock();
        try{
            for(int i = x - this.radius; i <= x + this.radius; i++){
                for(int j = y - this.radius; j <= y + this.radius; j++){
                    if(i >= 0 && i < this.gridDimension && j >= 0 && j < this.gridDimension){
                        positions.add(new Pair<Integer,Integer>(i,j));
                    }
                }
            }
        }
        finally{
            this.lock.unlock();
        }
        return positions;
    }

    private void generateRewards(){
        this.lock.lock();
        try{
            for(int i = 0; i < this.gridDimension; i++){
                for(int j = 0; j < this.gridDimension; j++){
                    Integer scootersInPos = this.getDistribution(i, j);
                    if (scootersInPos == null){
                        scootersInPos = State.AVERAGE_SCOOTERS_PER_SQUARE;
                    }
                    Integer reward = State.AVERAGE_SCOOTERS_PER_SQUARE - scootersInPos;
                    this.scooterRewards.set(i, j, reward);
                }
            }
        }
        finally{
            this.lock.unlock();
        }
        //System.out.println(this.scooterRewards);
    }

    private void generateDistribution(){
        this.lock.lock();
        try{
            for(int i = 0; i < this.gridDimension; i++){
                for(int j = 0; j < this.gridDimension; j++){
                    this.scooterDistribution.set(i, j, 0);
                    List<Scooter> scooterList = this.grid.get(i,j);
                    if(scooterList != null){
                        for (Scooter scooter : scooterList) {
                            if(!scooter.isBeingUsed()){
                                this.scooterDistribution.set(i, j, this.scooterDistribution.get(i, j) + 1);
                            }
                        }
                    }
                    else{
                        this.scooterDistribution.set(i, j, 0);
                    }
                }
            }
        }
        finally{
            this.lock.unlock();
        }
        //System.out.println(this.scooterDistribution);
    }

    private Integer getDistribution(Integer x, Integer y){
        this.lock.lock();
        try{
            return this.scooterDistribution.get(x, y);
        }
        finally{
            this.lock.unlock();
        }
    }

    private void updateDistribution(){
        this.lock.lock();
        try{
            this.generateDistribution();
        }
        finally{
            this.lock.unlock();
        }
    }

    public Matrix<List<Scooter>> getScootersInRadius(Integer x,Integer y){
        Matrix<List<Scooter>> scootersInRadius = new Matrix<List<Scooter>>(2*this.radius + 1);
        int start_x = x-this.radius;
        int end_x = x+this.radius;
        int start_y = y-this.radius;
        int end_y = y+this.radius;
        if (x-this.radius < 0){start_x = 0; }
        if (x+this.radius > this.gridDimension-1){end_x = this.gridDimension-1; }
        if (y-this.radius < 0){start_y = 0; }
        if (y+this.radius > this.gridDimension-1){end_y = this.gridDimension-1; }
        this.lock.lock();
        try{ 
            for(int i = start_x;i<=end_x;i++){
                for(int j = start_y;j<=end_y;j++){
                    Integer distance = Matrix.manhattan(x, y, i, j);
                    if (distance <= this.radius){
                        List<Scooter> scooterList = this.grid.get(i, j);
                        if (scooterList == null)
                            scooterList = new ArrayList<Scooter>();
                        scootersInRadius.set(i-start_x, j-start_y, scooterList);
                    }
                    else{
                        scootersInRadius.set(i-start_x, j-start_y, new ArrayList<Scooter>());
                    }
                }
            }
            return scootersInRadius;
        }
        finally{
            this.lock.unlock();
        }
    }

    public Matrix<Integer> getRewardsInRadius(Integer x, Integer y){
        Matrix<Integer> rewardsInRadius = new Matrix<Integer>(2*this.radius + 1);
        this.lock.lock();
        try{ 
            for(int i = x-this.radius;i<=x+this.radius;i++){
                for(int j = y-this.radius;j<=y+this.radius;j++){
                    Integer distance = Matrix.manhattan(x, y, i, j);
                    if (distance <= this.radius)
                        rewardsInRadius.set(i, j, this.scooterRewards.get(i, j));
                    else
                        rewardsInRadius.set(i, j, null);
                }
            }
        }
        finally{
            this.lock.unlock();
        }
        return rewardsInRadius;
    }
}
/*
        void existemTrotinetesProximas(Integer x1,Integer y1,PrintWriter out){//envia para o cliente as posições das trotinetes
            Integer number1 = 0,number2 = 0;
            try{
                lock.lock();
            
            while(number1<N){
                while(number2<N){
                    if((Math.abs(x1-number1 + Math.abs(y1-number2))<=D) && mapa[number1][number2].size() >= 1) {
                        out.write(number1);out.flush();
                        out.write(number2);out.flush();
                        
                    }
                    number2++;
                }
                number2=0;
                number1++;
            }
            }finally{
                lock.unlock();
            }
        }
    
        void listaRecompensas(Integer x1,Integer y1,PrintWriter out){//envia para o cliente as posições das recompensas e o seu valor
            Integer number1 = 0,number2 = 0;
            try{
                lock.lock();
            
            while(number1<N){
                while(number2<N){
                    if((Math.abs(x1-number1 + Math.abs(y1-number2))<=D) && mapaRecompensas[number1][number2] >= 1) {
                        out.write(number1);out.flush();
                        out.write(number2);out.flush();
                        out.write(mapaRecompensas[number1][number2]);out.flush();
                    }
                    number2++;
                }
                number2=0;
                number1++;
            }
        }finally{
            lock.unlock();
        }
        }
        int[] trotineteMaisProx(Integer x,Integer y){//retorna uma lista com o x e o y da trotinete mais próxima
            Integer number1 = 0,number2 = 0,flag = 0;
            int[] res = new int[2];
            while(number1<N && flag == 1){
                while(number2<N && flag == 1){
                    if((Math.abs(x-number1 + Math.abs(y-number2))<=D) && mapa[number1][number2].size() >= 1) {
                        res[0] = number1;
                        res[1] = number2;
                        flag = 1;
                    }
                    number2++;
                }
                number2=0;
                number1++;
            }
            return res;
        }
        void reservaTrotinete(Integer x,Integer y,PrintWriter out){
            try{
                lock.lock();
            
            int[] pos = trotineteMaisProx(x, y);
            if (mapa[pos[0]][pos[1]].get(0).estado == 0) {
                mapa[pos[0]][pos[1]].get(0).reserva();
                mapa[pos[0]][pos[1]].remove(0);
                out.write("O código de reserva é" +  mapa[pos[0]][pos[1]].get(0).codReserva);out.flush();
            }
            else{
                out.write("Código de insucesso" + 0);out.flush();
            }
        }finally{
            lock.unlock();
        }
        }
    
        void estacionaTrotinete(Integer codRes,Integer x,Integer y,PrintWriter out){
            trotinete p = listaTrotinetes.get(codRes);
            p.larga();
            try{
                lock.lock();
                mapa[x][y].add(p);
    
            }finally{
                lock.unlock();
            }
    
        }
    
    
}
*/