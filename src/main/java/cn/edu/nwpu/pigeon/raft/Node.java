package cn.edu.nwpu.pigeon.raft;

import cn.edu.nwpu.pigeon.Consensus;
import cn.edu.nwpu.pigeon.Log;
import cn.edu.nwpu.pigeon.StateMachine;
import cn.edu.nwpu.pigeon.config.NodeConfig;
import cn.edu.nwpu.pigeon.config.NodeStatus;
import cn.edu.nwpu.pigeon.message.*;
import cn.edu.nwpu.pigeon.raft.schedule.ElectionTask;
import cn.edu.nwpu.pigeon.raft.schedule.HeartBeatTask;
import cn.edu.nwpu.pigeon.raft.threadpool.RaftThreadPool;
import cn.edu.nwpu.pigeon.rpc.Request;
import cn.edu.nwpu.pigeon.rpc.RpcClient;
import cn.edu.nwpu.pigeon.rpc.RpcServer;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author zzf
 */
@Data
@NoArgsConstructor
@Slf4j
public class Node {




    private Consensus consensus;

    private HeartBeatTask heartBeatTask = new HeartBeatTask(this);
    private ElectionTask electionTask = new ElectionTask(this);


    /**
     * 选举间隔
     */
    public volatile long electionTime = 10 * 1000;
    /**
     * 选举时间
     */
    public volatile long preElectionTime = 0;

    /**
     * 心跳时间
     */
    public volatile long preHeartBeatTime = 0;
    /**
     * 心跳间隔
     */
    public final long heartBeatTick = 30 * 1000;

    /**
     * 节点状态
     */
    public volatile int status = NodeStatus.FOLLOWER.getCode();

    public PeerList peerList;

    private volatile boolean running = false;



    public volatile long currentTerm = 0;

    public volatile String votedId;

    /**
     * 日志条目集；每一个条目包含一个用户状态机执行的指令，和收到时的任期号
     */
    public Log logEntries;


    /**
     * 最大的已被提交的日志索引值
     */
    public volatile long commitedIndex;

    /**
     * 状态机最后日志条目索引值
     */
    public volatile long lastApplied = 0;

    public Map<Peer, Long> nextIndexs;


    public Map<Peer, Long> matchIndexs;

    public NodeConfig config;

    public RpcServer rpcServer;

    public RpcClient rpcClient = new RpcClient();

    public StateMachine stateMachine;




    public void init() throws Throwable {
        running = true;
        rpcServer = new RpcServer(this);
        rpcServer.init(Integer.parseInt(config.getSelfAddress().split(":")[1]));

        consensus = new ConsensusImpl(this);


        RaftThreadPool.scheduleWithFixedDelay(heartBeatTask, 10000);
        RaftThreadPool.scheduleAtFixedRate(electionTask, 6000, 500);


        LogEntry logEntry = logEntries.getLast();
        if (logEntry != null) {
            currentTerm = logEntry.getTerm();
        }

        log.info("启动成功, selfId : {} ", peerList.getSelf());
    }


    public void setConfig(NodeConfig config) {
        this.config = config;
        stateMachine = new StateMachineImpl();
        logEntries = new LogModuleImpl();

        peerList = new PeerList();
        for (String s : config.getPeerAddress()) {
            Peer peer = new Peer(s);
            peerList.addPeer(peer);
            if (s.equals(config.getSelfAddress())) {
                peerList.setSelf(peer);
            }
        }

    }


    public VoteResult handlerRequestVote(VoteRequest param) {
        log.warn("处理投票 : {}", param);
        return consensus.requestVote(param);
    }


    public AppendEntryResult handlerAppendEntries(AppendEntryRequest param) {
        if (param.getEntries() != null) {
            log.warn("获取 {} 日志复制请求, 日志内容  = {}", param.getLeaderId(), param.getEntries());
        }

        return consensus.appendEntries(param);
    }


    public ClientKVResult redirect(ClientKVRequest request) {
        Request r = Request.builder()
                .obj(request)
                .url(peerList.getLeader().getAddress())
                .requestType(Request.CLIENT_REQ).build();

        return rpcClient.send(r).to(ClientKVResult.class);
    }

    public synchronized ClientKVResult handlerClientRequest(ClientKVRequest request) {

        log.warn(" {}操作,  and key : [{}], value : [{}]",
                ClientKVRequest.Type.value(request.getType()), request.getKey(), request.getValue());

        if (status != NodeStatus.LEADER.getCode()) {
            log.warn("重定向 leader address : {}",
                    peerList.getLeader());
            return redirect(request);
        }

        if (request.getType() == ClientKVRequest.GET) {
            String value = stateMachine.get(request.getKey());
            if (value != null) {
                return new ClientKVResult(value);
            }
            return new ClientKVResult(null);
        }

        LogEntry logEntry = LogEntry.builder()
                .command(Command.builder().
                        method(Command.SET).
                        key(request.getKey()).
                        value(request.getValue()).
                        build())
                .term(currentTerm)
                .build();

        // 预提交到本地日志
        logEntries.write(logEntry);
        log.info("预提交日志成功, logEntry : {}, log index : {}", logEntry, logEntry.getIndex());

        final AtomicInteger success = new AtomicInteger(0);

        List<Future<Boolean>> futureList = new ArrayList<>();

        int count = 0;
        //  复制
        for (Peer peer : peerList.getOtherPeers()) {
            count++;
            futureList.add(replication(peer, logEntry));
        }

        CountDownLatch countDownLatch = new CountDownLatch(futureList.size());
        List<Boolean> resultList = new CopyOnWriteArrayList<>();

        getRPCAppendResult(futureList, countDownLatch, resultList);

        try {
            countDownLatch.await(1000, MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        for (Boolean aBoolean : resultList) {
            if (aBoolean) {
                success.incrementAndGet();
            }
        }

        List<Long> matchIndexList = new ArrayList<>(matchIndexs.values());

        int median = 0;
        if (matchIndexList.size() >= 2) {
            Collections.sort(matchIndexList);
            median = matchIndexList.size() / 2;
        }
        Long N = matchIndexList.get(median);
        if (N > commitedIndex) {
            LogEntry entry = logEntries.read(N);
            if (entry != null && entry.getTerm() == currentTerm) {
                commitedIndex = N;
            }
        }


        if (success.get() >= (count / 2)) {
            // 更新
            commitedIndex = logEntry.getIndex();
            getStateMachine().apply(logEntry);
            lastApplied = commitedIndex;

            log.info("成功提交复制状态机,  logEntry info : {}", logEntry);
            // 返回成功.
            return ClientKVResult.success();
        } else {
            // 回滚
            logEntries.removeOnStartIndex(logEntry.getIndex());
            log.warn("提交失败, 回滚状态,  logEntry info : {}", logEntry);
            return ClientKVResult.fail();
        }
    }

    private void getRPCAppendResult(List<Future<Boolean>> futureList, CountDownLatch latch, List<Boolean> resultList) {
        for (Future<Boolean> future : futureList) {
            RaftThreadPool.execute(() -> {
                try {
                    resultList.add(future.get(5000, MILLISECONDS));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    resultList.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }
    }


    /**
     * 复制到其他机器
     */
    public Future<Boolean> replication(Peer peer, LogEntry entry) {

        return RaftThreadPool.submit(() -> {

            long start = System.currentTimeMillis(), end = start;

            while (end - start < 20 * 1000L) {

                AppendEntryRequest appendEntryRequest = new AppendEntryRequest();
                appendEntryRequest.setTerm(currentTerm);
                appendEntryRequest.setServerId(peer.getAddress());
                appendEntryRequest.setLeaderId(peerList.getSelf().getAddress());

                appendEntryRequest.setLeaderCommit(commitedIndex);

                Long nextIndex = nextIndexs.get(peer);
                LinkedList<LogEntry> logEntries = new LinkedList<>();
                if (entry.getIndex() >= nextIndex) {
                    for (long i = nextIndex; i <= entry.getIndex(); i++) {
                        LogEntry l = Node.this.logEntries.read(i);
                        if (l != null) {
                            logEntries.add(l);
                        }
                    }
                } else {
                    logEntries.add(entry);
                }

                LogEntry preLog = getPreLog(logEntries.getFirst());
                appendEntryRequest.setPreLogTerm(preLog.getTerm());
                appendEntryRequest.setPrevLogIndex(preLog.getIndex());

                appendEntryRequest.setEntries(logEntries.toArray(new LogEntry[0]));

                Request request = Request.builder()
                        .requestType(Request.APPEND_ENTRIES)
                        .obj(appendEntryRequest)
                        .url(peer.getAddress())
                        .build();

                try {
                    AppendEntryResult result = rpcClient.send(request).to(AppendEntryResult.class);
                    if (result == null) {
                        return false;
                    }
                    if (result.isSuccess()) {
                        log.info("追加成功 , follower=[{}], entry=[{}]", peer, appendEntryRequest.getEntries());

                        nextIndexs.put(peer, entry.getIndex() + 1);
                        matchIndexs.put(peer, entry.getIndex());
                        return true;
                    } else {
                        if (result.getTerm() > currentTerm) {
                            log.warn("节点 [{}] 任期 [{}] 大于自身任期 [{}]",
                                    peer, result.getTerm(), currentTerm);
                            currentTerm = result.getTerm();

                            status = NodeStatus.FOLLOWER.getCode();
                            return false;
                        }
                        else {
                            // 递减
                            if (nextIndex == 0) {
                                nextIndex = 1L;
                            }
                            nextIndexs.put(peer, nextIndex - 1);
                        }
                    }

                    end = System.currentTimeMillis();

                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                    return false;
                }
            }

            return false;
        });

    }

    private LogEntry getPreLog(LogEntry logEntry) {
        LogEntry entry = logEntries.read(logEntry.getIndex() - 1);

        if (entry == null) {
            entry = LogEntry.builder().index(0L).term(0L).command(null).build();
        }
        return entry;
    }

    public void leaderToDo() {
        nextIndexs = new ConcurrentHashMap<>();
        matchIndexs = new ConcurrentHashMap<>();
        for (Peer peer : peerList.getOtherPeers()) {
            nextIndexs.put(peer, logEntries.getLastIndex() + 1);
            matchIndexs.put(peer, 0L);
        }


        LogEntry logEntry = LogEntry.builder()
                .command(null)
                .term(currentTerm)
                .build();

        logEntries.write(logEntry);
        log.info("预提交 logEntry info : {}, log index : {}", logEntry, logEntry.getIndex());

        final AtomicInteger success = new AtomicInteger(0);

        List<Future<Boolean>> futureList = new ArrayList<>();

        int count = 0;

        for (Peer peer : peerList.getOtherPeers()) {
            count++;
            futureList.add(replication(peer, logEntry));
        }

        CountDownLatch countDownLatch = new CountDownLatch(futureList.size());
        List<Boolean> resultList = new CopyOnWriteArrayList<>();

        getRPCAppendResult(futureList, countDownLatch, resultList);

        try {
            countDownLatch.await(4000, MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        for (Boolean aBoolean : resultList) {
            if (aBoolean) {
                success.incrementAndGet();
            }
        }

        List<Long> matchIndexList = new ArrayList<>(matchIndexs.values());
        int median = 0;
        if (matchIndexList.size() >= 2) {
            Collections.sort(matchIndexList);
            median = matchIndexList.size() / 2;
        }
        Long N = matchIndexList.get(median);
        if (N > commitedIndex) {
            LogEntry entry = logEntries.read(N);
            if (entry != null && entry.getTerm() == currentTerm) {
                commitedIndex = N;
            }
        }

        //  响应客户端(成功一半)
        if (success.get() >= (count / 2)) {
            // 更新
            commitedIndex = logEntry.getIndex();
            //  应用到状态机
            getStateMachine().apply(logEntry);
            lastApplied = commitedIndex;

            log.info("提交复制状态机成功,  logEntry info : {}", logEntry);
        } else {
            // 回滚已经提交的日志
            logEntries.removeOnStartIndex(logEntry.getIndex());
            log.warn("提交失败,  logEntry info : {}", logEntry);

            status = NodeStatus.FOLLOWER.getCode();
            peerList.setLeader(null);
            votedId = "";
        }

    }


}
