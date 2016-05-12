package edu.nyu.jet.ice.models;

import edu.nyu.jet.ice.utils.IceInfoStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Index {
    private String name;
    private String type;
    private double cutoff;
    private IceInfoStatus status = IceInfoStatus.NOT_PROCESSED;

    public Index () {
	this.name = "";
	this.type = "";
	this.cutoff = 0.0;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType () { return type;}

    public void setType (String type) {
	this.type = type;
    }

    public double getCutoff() { return cutoff; }
    public void setCutoff(double cutoff) { this.cutoff = cutoff; }

    public IceInfoStatus getStatus() { return status; }
    public void setStatus(IceInfoStatus status) { this.status = status; }
    
}
