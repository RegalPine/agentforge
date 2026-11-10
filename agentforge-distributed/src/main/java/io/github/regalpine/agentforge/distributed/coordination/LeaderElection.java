package io.github.regalpine.agentforge.distributed.coordination;

public interface LeaderElection {

    boolean tryAcquireLeadership();

    void releaseLeadership();

    boolean isLeader();
}
