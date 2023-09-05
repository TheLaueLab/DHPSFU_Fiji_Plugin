package uk.ac.cam.dhpsfu.analysis;

public class DC_GeneralParas {
    public double px_size;
    public double upFactor;
    public double burst;
    public double cycle;

    public DC_GeneralParas(double px_size, double upFactor, int burst, int cycle ){
        this.px_size = px_size;
        this.upFactor = upFactor;
        this.burst = burst;
        this.cycle = cycle;
    }

    public double getPxSize(){
        return px_size;
    }
    public void setPxSize(double px_size){
        this.px_size = px_size;
    }

    public double getUpFactor(){
        return upFactor;
    }
    public void setUpFactor(double upFactor){
        this.upFactor = upFactor;
    }

    public double getBurst(){
        return burst;
    }
    public void setBurst(double burst){
        this.burst = burst;
    }

    public double getCycle(){
        return cycle;
    }
    public void setCycle(double cycle){
        this.cycle = cycle;
    }
}
