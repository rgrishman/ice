package edu.nyu.jet.ice.models;

import edu.nyu.jet.ice.utils.IceInfoStatus;
import edu.nyu.jet.ice.relation.SeedPattern;

import java.util.List;

public class PatternInfo {
    private List<SeedPattern> patterns;
    private IceInfoStatus status;

    public IceInfoStatus getStatus() { return status; }

    public void setStatus(IceInfoStatus status) { this.status = status; }

    public List<SeedPattern> getPatterns() { return patterns; }

    public void setPatterns(List<SeedPattern> patterns) { this.patterns = patterns; }
}
