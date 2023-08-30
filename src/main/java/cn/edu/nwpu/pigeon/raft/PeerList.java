package cn.edu.nwpu.pigeon.raft;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zzf
 */
@Data
public class PeerList {
    private List<Peer> peerList = new ArrayList<>();

    private volatile Peer leader;

    private volatile Peer self;


    public List<Peer> getOtherPeers(){
        List<Peer> list2 = new ArrayList<>(peerList);
        list2.remove(self);
        return list2;
    }

    public void addPeer(Peer peer){
        peerList.add(peer);
    }

}
