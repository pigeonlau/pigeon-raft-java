package cn.edu.nwpu.pigeon.raft;

import cn.edu.nwpu.pigeon.Consensus;
import cn.edu.nwpu.pigeon.config.NodeStatus;
import cn.edu.nwpu.pigeon.message.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zzf
 */
@Slf4j
public class ConsensusImpl implements Consensus {

    private final Node node;

    private final ReentrantLock voteLock = new ReentrantLock();

    private final ReentrantLock appendEntryLock = new ReentrantLock();

    public ConsensusImpl(Node node) {
        this.node = node;
    }

    @Override
    public VoteResult requestVote(VoteRequest voteRequest) {
        try {
            if (!voteLock.tryLock()) {
                return new VoteResult(node.getCurrentTerm(), false);
            }

            // 对方任期没有自己新
            if (voteRequest.getTerm() < node.getCurrentTerm()) {
                return new VoteResult(node.getCurrentTerm(), false);

            }

            // (当前节点并没有投票 或者 已经投票过了且是对方节点) && 对方日志和自己一样新
            log.info("节点 {} 当前投票给 [{}]", node.peerList.getSelf(), node.getVotedId());
            log.info("节点 {} 当前任期 {}, 对方任期 : {}", node.peerList.getSelf(), node.getCurrentTerm(), voteRequest.getTerm());

            if (((node.getVotedId() == null || node.getVotedId().equals("")) || node.getVotedId().equals(voteRequest.getCandidateId()))) {

                if (node.getLogEntries().getLast() != null) {
                    // 对方没有自己新
                    if (node.getLogEntries().getLast().getTerm() > voteRequest.getLastLogTerm()) {
                        return new VoteResult(0L, false);
                    }
                    // 对方没有自己新
                    if (node.getLogEntries().getLastIndex() > voteRequest.getLastLogIndex()) {
                        return new VoteResult(0L, false);
                    }
                }

                // 切换状态
                node.status = NodeStatus.FOLLOWER.getCode();
                // 更新
                node.peerList.setLeader(new Peer(voteRequest.getCandidateId()));
                node.setCurrentTerm(voteRequest.getTerm());
                node.setVotedId(voteRequest.getServerId());
                // 返回成功
                return new VoteResult(node.getCurrentTerm(), true);
            }
            return new VoteResult(node.getCurrentTerm(), false);

        } finally {
            voteLock.unlock();
        }
    }

    @Override
    public AppendEntryResult appendEntries(AppendEntryRequest param) {
        AppendEntryResult result = new AppendEntryResult(0L,false);
        try {
            if (!appendEntryLock.tryLock()) {
                return result;
            }

            result.setTerm(node.getCurrentTerm());

            if (param.getTerm() < node.getCurrentTerm()) {
                return result;
            }

            node.preHeartBeatTime = System.currentTimeMillis();
            node.preElectionTime = System.currentTimeMillis();
            node.peerList.setLeader(new Peer(param.getLeaderId()));


            if (param.getTerm() >= node.getCurrentTerm()) {
                log.debug("节点 {} 成为 FOLLOWER, currentTerm : {}",
                        node.peerList.getSelf(), node.currentTerm);

                node.status = NodeStatus.FOLLOWER.getCode();
            }
            // 使用对方term.
            node.setCurrentTerm(param.getTerm());

            //心跳
            if (param.getEntries() == null || param.getEntries().length == 0) {
                log.info("节点 {} 处理心跳成功 , leader term : {}, 节点 term : {}",
                        param.getLeaderId(), param.getTerm(), node.getCurrentTerm());


                // 下一个需要提交的日志的索引
                long nextCommit = node.getCommitedIndex() + 1;

                //如果 leaderCommit > commitIndex，令 commitIndex 等于 leaderCommit 和 新日志条目索引值中较小的一个
                if (param.getLeaderCommit() > node.getCommitedIndex()) {
                    int commitIndex = (int) Math.min(param.getLeaderCommit(), node.getLogEntries().getLastIndex());
                    node.setCommitedIndex(commitIndex);
                    node.setLastApplied(commitIndex);
                }

                while (nextCommit <= node.getCommitedIndex()) {
                    // 提交之前的日志
                    node.stateMachine.apply(node.getLogEntries().read(nextCommit));
                    nextCommit++;
                }
                return new AppendEntryResult(node.getCurrentTerm(), true);
            }


            if (node.getLogEntries().getLastIndex() != 0 && param.getPrevLogIndex() != 0) {
                LogEntry logEntry;
                if ((logEntry = node.getLogEntries().read(param.getPrevLogIndex())) != null) {
                    // 如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false
                    // 需要减小 nextIndex 重试.
                    if (!Objects.equals(logEntry.getTerm(), param.getPreLogTerm())) {
                        return result;
                    }
                } else {

                    return result;
                }

            }

            // 如果已经存在的日志条目和新的产生冲突（索引值相同但是任期号不同），删除这一条和之后所有的
            LogEntry existLog = node.getLogEntries().read(((param.getPrevLogIndex() + 1)));
            if (existLog != null && !Objects.equals(existLog.getTerm(), param.getEntries()[0].getTerm())) {
                // 删除这一条和之后所有的, 然后写入日志和状态机.
                node.getLogEntries().removeOnStartIndex(param.getPrevLogIndex() + 1);
            } else if (existLog != null) {

                result.setSuccess(true);
                return result;
            }

            // 写进日志
            for (LogEntry entry : param.getEntries()) {
                node.getLogEntries().write(entry);
                result.setSuccess(true);
            }

            long nextCommit = node.getCommitedIndex() + 1;


            if (param.getLeaderCommit() > node.getCommitedIndex()) {
                int commitIndex = (int) Math.min(param.getLeaderCommit(), node.getLogEntries().getLastIndex());
                node.setCommitedIndex(commitIndex);
                node.setLastApplied(commitIndex);
            }

            while (nextCommit <= node.getCommitedIndex()) {

                node.stateMachine.apply(node.getLogEntries().read(nextCommit));
                nextCommit++;
            }

            result.setTerm(node.getCurrentTerm());

            node.status = NodeStatus.FOLLOWER.getCode();
            return result;
        } finally {
            appendEntryLock.unlock();
        }
    }
}
