import java.util.concurrent.locks.*;

import Utilities.Log;
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
    private Matrix<Integer> scooterDropRewards;
    private Matrix<Integer> scooterPickupRewards;

    public static class RadiusTooFar extends Exception {
        private static final long serialVersionUID = 1L;
        public RadiusTooFar(String message){
            super(message);
        }
    }

    public State(Integer gridDimension, Integer radius, Integer totalScooters){
        
        this.totalScooters = totalScooters;
        this.gridDimension = gridDimension;
        this.radius = radius;
        this.scooterList = new ArrayList<Scooter>();
        this.grid = new Matrix<List<Scooter>>(gridDimension);
        this.scooterDistribution = new Matrix<Integer>(gridDimension);
        this.scooterDropRewards = new Matrix<Integer>(gridDimension);
        this.scooterPickupRewards = new Matrix<Integer>(gridDimension);
        
        this.generateScooters();
        this.generateMap();
        this.generateAll();
    }

    public State(){
        // initialize the state by calling the 3-parameter constructor
        this(1,1,1);
    }

    private void generateAll(){
        Thread t1 = new Thread(new Runnable(){
            public void run(){
                while(true){
                    Log.all(Log.INFO,"Updating distribution and rewards");
                    updateDistribution();
                    generateRewards();
                    try{
                        Thread.sleep(30000); // 30 seconds
                    } catch (InterruptedException e){
                        Log.all(Log.ERROR,"Thread interrupted");
                    }
                }
            }
        });
        t1.start();
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
        return this.scooterDropRewards;
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
        this.scooterDropRewards.set(x, y, reward);
    }

    public Integer getDropReward(Integer x, Integer y){
        return this.scooterDropRewards.get(x, y);
    }

    public Integer getPickupReward(Integer x, Integer y){
        return this.scooterPickupRewards.get(x, y);
    }

    public void generateScooters(){
    // Generate random scooters in the city, based on the number of scooters
    // and the size of the city.
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
                    Integer pickupReward = reward*-1;
                    if(reward < 0)
                        reward = 0;
                    if(pickupReward < 0)
                        pickupReward = 0;
                    this.scooterPickupRewards.set(i, j, pickupReward);
                    this.scooterDropRewards.set(i, j, reward);
                }
            }
        }
        finally{
            this.lock.unlock();
        }
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
        this.lock.lock();
        try{
            scootersInRadius.setUserPos(x-start_x, y-start_y);
            for(int i = start_x;i<=end_x;i++){
                for(int j = start_y;j<=end_y;j++){
                    if (i < 0 || i >= this.gridDimension || j < 0 || j >= this.gridDimension){
                        scootersInRadius.set(i-start_x, j-start_y, null);
                        continue;
                    }
                    Integer distance = Matrix.manhattan(x, y, i, j);
                    if (distance <= this.radius){
                        List<Scooter> scooterList = this.grid.get(i, j);
                        if (scooterList == null)
                            scooterList = new ArrayList<Scooter>();
                        scootersInRadius.set(i-start_x, j-start_y, scooterList);
                    }
                    else{
                        scootersInRadius.set(i-start_x, j-start_y, null);
                    }
                }
            }
            return scootersInRadius;
        }catch(Exception e){
            Log.all(Log.ERROR, e.getMessage());
            return null;
        }
        finally{
            this.lock.unlock();
        }
    }

    public Matrix<Integer> getDropRewardsInRadius(Integer x, Integer y){
        Matrix<Integer> rewardsInRadius = new Matrix<Integer>(2*this.radius + 1);
        int start_x = x-this.radius;
        int end_x = x+this.radius;
        int start_y = y-this.radius;
        int end_y = y+this.radius;
        this.lock.lock();
        try{
            rewardsInRadius.setUserPos(x-start_x, y-start_y);
            for(int i = start_x;i<= end_x;i++){
                for(int j = start_y;j<=end_y;j++){
                    if (i < 0 || i >= this.gridDimension || j < 0 || j >= this.gridDimension){
                        rewardsInRadius.set(i-start_x, j-start_y, 0);
                        continue;
                    }
                    Integer distance = Matrix.manhattan(x, y, i, j);
                    if (distance <= this.radius)
                        rewardsInRadius.set(i-start_x, j-start_y, this.scooterDropRewards.get(i, j));
                    else
                        rewardsInRadius.set(i-start_x, j-start_y, 0);
                }
            }
        }
        finally{
            this.lock.unlock();
        }
        return rewardsInRadius;
    }

    public Matrix<Integer> getPickupRewardsInRadius(Integer x, Integer y){
        Matrix<Integer> pickupRewardsInRadius = new Matrix<Integer>(2*this.radius + 1);
        int start_x = x-this.radius;
        int end_x = x+this.radius;
        int start_y = y-this.radius;
        int end_y = y+this.radius;
        this.lock.lock();
        try{
            pickupRewardsInRadius.setUserPos(x-start_x, y-start_y);
            for(int i = start_x;i<=end_x;i++){
                for(int j = start_y;j<=end_y;j++){
                    if (i < 0 || i >= this.gridDimension || j < 0 || j >= this.gridDimension){
                        pickupRewardsInRadius.set(i-start_x, j-start_y, 0);
                        continue;
                    }
                    Integer distance = Matrix.manhattan(x, y, i, j);
                    if (distance <= this.radius)
                        pickupRewardsInRadius.set(i-start_x, j-start_y, this.scooterPickupRewards.get(i, j));
                    else
                        pickupRewardsInRadius.set(i-start_x, j-start_y, 0);
                }
            }
        }
        finally{
            this.lock.unlock();
        }
        return pickupRewardsInRadius;
    }

    public Pair<String,Scooter> reserveScooter(User user, int idScooter) throws Scooter.InvalidScooter,State.RadiusTooFar{
        this.lock.lock();
        Scooter scooter = null;
        try{
            for(Scooter a : this.scooterList){
                if(a.getId() == idScooter){
                    scooter = a;
                    break;
                }
            }
            if(scooter == null){
                throw new Scooter.InvalidScooter("Scooter does not exist");
            }
            if(scooter.isBeingUsed()){
                throw new Scooter.InvalidScooter("Scooter is being used");
            }
            Pair<Integer, Integer> scooterPos = scooter.getPos();
            Pair<Integer, Integer> userPos = user.getPosition();
            Integer distance = Matrix.manhattan(userPos.getL(), userPos.getR(), scooterPos.getL(), scooterPos.getR());
            if (distance > this.radius){
                throw new State.RadiusTooFar("Radius too far");
            }
            scooter.setLastUser(user.getUsername());
            List<Scooter> scooters = this.grid.get(scooterPos.getL(), scooterPos.getR());
            scooters.remove(scooter);
            this.grid.set(scooterPos.getL(), scooterPos.getR(), scooters);
            String code = scooter.aquire();
            if(user.Subscribed()){
                user.addReward(this.getPickupReward(scooterPos.getL(), scooterPos.getR()));
            }
            this.updateDistribution();
            this.generateRewards();
            return new Pair<String,Scooter>(code,scooter);
        }
        finally{
            this.lock.unlock();
        }
    }

    public Scooter getById(Integer idScooter) throws Scooter.InvalidScooter{
        this.lock.lock();
        try{
            for(Scooter a : this.scooterList){
                if(a.getId() == idScooter){
                    return a;
                }
            }
            throw new Scooter.InvalidScooter("Scooter does not exist");
        }
        finally{
            this.lock.unlock();
        }
    }

    public User parkScooter(User user,String reservationCode,Integer x, Integer y, Scooter scooter) throws Scooter.InvalidReservationCode{
        this.lock.lock();
        try{
            List<Scooter> scooterList = this.grid.get(x, y);
            if(scooterList == null){
                scooterList = new ArrayList<Scooter>();
            }
            int reward = this.scooterDropRewards.get(x, y);
            scooterList.add(scooter);
            this.grid.set(x, y, scooterList);
            Pair<Integer, Integer> stop = new Pair<Integer, Integer>(x, y);
            Scooter.Invoice invoice = scooter.generateInvoice(user,stop);
            user.addInvoice(invoice);
            if (user.Subscribed())
                user.addReward(reward);
            scooter.setPos(stop);
            scooter.release(reservationCode);
            this.updateDistribution();
            this.generateRewards();
            return user;
        }
        catch(Exception e){
            throw e;
        }
        finally{
            this.lock.unlock();
        }
    }

}