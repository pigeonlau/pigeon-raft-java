package cn.edu.nwpu.pigeon.raft.schedule;

import cn.edu.nwpu.pigeon.config.NodeStatus;
import cn.edu.nwpu.pigeon.message.LogEntry;
import cn.edu.nwpu.pigeon.message.VoteRequest;
import cn.edu.nwpu.pigeon.message.VoteResult;
import cn.edu.nwpu.pigeon.raft.Node;
import cn.edu.nwpu.pigeon.raft.Peer;
import cn.edu.nwpu.pigeon.raft.threadpool.RaftThreadPool;
import cn.edu.nwpu.pigeon.rpc.Request;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@AllArgsConstructor
@Slf4j
public
class ElectionTask implements Runnable {

        private Node node;

        @Override
        public void run() {

            if (node.status == NodeStatus.LEADER.getCode()) {
                return;
            }

            long current = System.currentTimeMillis();
            // 基于 RAFT 的随机时间,解决冲突.
            node.electionTime = node.electionTime + ThreadLocalRandom.current().nextInt(50);
            if (current - node.preElectionTime < node.electionTime) {
                return;
            }
            node.status = NodeStatus.CANDIDATE.getCode();
            log.info("节点 {} 成为候选人   开始leader 选举, current term : [{}], LastEntry : [{}]",
                    node.peerList.getSelf(), node.currentTerm, node.logEntries.getLast());

            node.preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150;

            node.currentTerm = node.currentTerm + 1;
            // 推荐自己.
            node.votedId = node.peerList.getSelf().getAddress();

            List<Peer> peers = node.peerList.getOtherPeers();

            ArrayList<Future<VoteResult>> futureArrayList = new ArrayList<>();

            log.info("成员数量 : {}, 成员列表 : {}", peers.size(), peers);

            // 发送请求
            for (Peer peer : peers) {

                futureArrayList.add(RaftThreadPool.submit(() -> {
                    long lastTerm = 0L;
                    LogEntry last = node.logEntries.getLast();
                    if (last != null) {
                        lastTerm = last.getTerm();
                    }

                    VoteRequest param = VoteRequest.builder().
                            term(node.currentTerm).
                            candidateId(node.peerList.getSelf().getAddress()).
                            lastLogIndex(node.logEntries.getLastIndex()).
                            lastLogTerm(lastTerm).
                            build();

                    Request request = Request.builder()
                            .requestType(Request.REQUEST_VOTE)
                            .obj(param)
                            .url(peer.getAddress())
                            .build();

                    try {
                        return node.getRpcClient().send(request).to(VoteResult.class);
                    } catch (Exception e) {
                        log.error("选举 RPC 失败 , URL : " + request.getUrl());
                        return null;
                    }
                }));
            }

            AtomicInteger success2 = new AtomicInteger(0);
            CountDownLatch countDownLatch = new CountDownLatch(futureArrayList.size());

            log.info("请求数 : {}", futureArrayList.size());
            // 等待结果.
            for (Future<VoteResult> future : futureArrayList) {
                RaftThreadPool.submit(() -> {
                    try {
                        VoteResult result = future.get(3000, MILLISECONDS);
                        if (result == null) {
                            return -1;
                        }
                        boolean isVoteGranted = result.isVoteGranted();

                        if (isVoteGranted) {
                            success2.incrementAndGet();
                        } else {
                            // 更新自己的任期.
                            long resTerm = result.getTerm();
                            if (resTerm >= node.currentTerm) {
                                node.currentTerm = resTerm;
                            }
                        }
                        return 0;
                    } catch (Exception e) {
                        log.error("响应获取失败 , e : ", e);
                        return -1;
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }

            try {
                // 稍等片刻
                countDownLatch.await(6000, MILLISECONDS);
            } catch (InterruptedException e) {
                log.warn("aaaaaaaaaaaaaaaaaa");
            }

            int success = success2.get();
            log.info("节点 {} 投票结果 , 投票数 = {} , status : {}", node.peerList.getSelf(), success, NodeStatus.value(node.status));
            // 如果投票期间,有其他服务器发送 appendEntry , 就可能变成 follower ,这时,应该停止.
            if (node.status == NodeStatus.FOLLOWER.getCode()) {
                return;
            }
            // 加上自身.
            if (success >= peers.size() / 2) {
                log.warn("节点 {} 选举成功, 成为leader", node.peerList.getSelf());
                node.status = NodeStatus.LEADER.getCode();
                node.peerList.setLeader(node.peerList.getSelf());
                node.votedId = "";
                node.leaderToDo();
            } else {
                // else 重新选举
                node.votedId = "";
            }
            // 再次更新选举时间
            node.preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150;

        }
    }