package cn.edu.nwpu.pigeon.raft.schedule;

import cn.edu.nwpu.pigeon.config.NodeStatus;
import cn.edu.nwpu.pigeon.message.AppendEntryRequest;
import cn.edu.nwpu.pigeon.message.AppendEntryResult;
import cn.edu.nwpu.pigeon.raft.Node;
import cn.edu.nwpu.pigeon.raft.Peer;
import cn.edu.nwpu.pigeon.raft.threadpool.RaftThreadPool;
import cn.edu.nwpu.pigeon.rpc.Request;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public
class HeartBeatTask implements Runnable {

    private Node node;

        @Override
        public void run() {

            if (node.status != NodeStatus.LEADER.getCode()) {
                return;
            }

            long current = System.currentTimeMillis();
            if (current - node.preHeartBeatTime < node.heartBeatTick) {
                return;
            }
            log.info("****************** Heart Beat **********************");
            for (Peer peer : node.peerList.getOtherPeers()) {
                log.info("Peer {} nextIndex={}", peer.getAddress(), node.nextIndexs.get(peer));
            }

            node.preHeartBeatTime = System.currentTimeMillis();

            for (Peer peer : node.peerList.getOtherPeers()) {

                AppendEntryRequest param = AppendEntryRequest.builder()
                        .entries(null)// 心跳,空日志.
                        .leaderId(node.peerList.getSelf().getAddress())
                        .serverId(peer.getAddress())
                        .term(node.currentTerm)
                        .leaderCommit(node.commitedIndex) //  commit index
                        .build();

                Request request = new Request(
                        Request.APPEND_ENTRIES,
                        peer.getAddress(),
                        param);

                RaftThreadPool.execute(() -> {
                    try {
                        AppendEntryResult appendEntryResult = node.rpcClient.send(request).to(AppendEntryResult.class);
                        long term = appendEntryResult.getTerm();

                        if (term > node.currentTerm) {
                            node.currentTerm = term;
                            node.votedId = "";
                            node.status = NodeStatus.FOLLOWER.getCode();
                        }
                    } catch (Exception e) {
                        log.error("心跳 RPC Fail, URL : {} ", request.getUrl());
                    }
                }, false);
            }
        }
    }

