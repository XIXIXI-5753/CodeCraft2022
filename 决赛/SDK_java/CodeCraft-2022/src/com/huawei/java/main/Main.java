package com.huawei.java.main;
import javax.swing.*;
import javax.swing.plaf.IconUIResource;
import java.io.*;
import java.net.Inet4Address;
import java.util.*;



public class Main {

    public static ClientNode[] clientSet ;  //用户节点数组
    public static EdgeNode[] edgeNodeSet;   //边缘节点数组
    public static int edgeNodeSize;         //边缘节点数量
    public static int clientNodeSize;       //客户节点数量
    public static int T;                    //时间
    public static int[][] demandDyn;        //动态需求矩阵
    public static int[][] demandDynBase;    //大于基本费用的动态矩阵
    public static ArrayList<Stream>[] streamT;  //流的总链表
    public static ArrayList<Stream> streamAll;
    public static int base_cost;
    public static int[] demandT;
    public static int streamMax;
    public static int[] centerCostLow;     //中心节点的理论最低价格
    public static double center_cost;
    public static TNode[] tList;
    public static int centerPriceMin;      //中心节点最低价格百分之九十五分位,会动态变化
    public static int[][] streamGroupMax;  //每组流当前没分配出去的最大值
    public static int[][] demandDynBig;   //大于20倍基本费用的动态矩阵
    public static int[] demandTBig;       //大于20倍基本费用的时间矩阵
    public static ArrayList<Integer>[] cacheChange;    //选择缓存变动的边缘节点

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        String filepath1 = "/data/demand.csv";
        String filepath2 = "/data/qos.csv";
        String filepath3 = "/data/site_bandwidth.csv";

        ArrayList<List<String>> demand = getdata(filepath1);   //读取三个csv文件
        ArrayList<List<String>> site_bandwidth = getdata(filepath3);
        ArrayList<List<String>> qos = getdata(filepath2);
        int qos_constraint = getini()[0];
        base_cost = getini()[1];
        center_cost = getiniCenter();
        clientNodeSize = demand.get(0).size() - 2;
        edgeNodeSize = site_bandwidth.size() - 1;

        String[] clientName = new String[clientNodeSize];
        for(int i = 2; i < demand.get(0).size(); i++){
            clientName[i-2] = demand.get(0).get(i);
        }

        String[] edgeName = new String[site_bandwidth.size() - 1];
        for(int i = 1; i < site_bandwidth.size(); i++){
            edgeName[i-1] = site_bandwidth.get(i).get(0);
        }

        String[] clientName1 = new String[clientNodeSize];
        for(int i = 1; i < qos.get(0).size(); i++){
            clientName1[i-1] = qos.get(0).get(i);
        }

        String[] edgeName1 = new String[site_bandwidth.size() - 1];
        for(int i = 1; i < qos.size(); i++){
            edgeName1[i - 1] = qos.get(i).get(0);
        }

        int[] clientNameInd = new int[demand.get(0).size() - 2];
        for(int i = 0; i < demand.get(0).size() - 2; i++){
            for(int j = 0; j < demand.get(0).size() - 2; j++){
                if(clientName[i].equals(clientName1[j])){
                    clientNameInd[i] = j;
                    break;
                }
            }
        }



        int[] edgeNameInd = new int[site_bandwidth.size() - 1];
        for(int i = 0; i < site_bandwidth.size() - 1; i++){
            for(int j = 0; j < site_bandwidth.size() - 1; j++){
                if(edgeName[i].equals(edgeName1[j])){
                    edgeNameInd[i] = j;
                    break;
                }
            }
        }


        //初始化时间列表
        T = 1;
        String timeString = demand.get(1).get(0);
        ArrayList<String> timeList = new ArrayList<String>();
        timeList.add(timeString);
        for(int i = 1; i < demand.size(); i++){
            if(timeString.equals(demand.get(i).get(0))) continue;
            T++;
            timeString = demand.get(i).get(0);
            timeList.add(timeString);  //做一个时间列表
        }

        edgeNodeSet = new EdgeNode[edgeNodeSize];
        clientSet = new ClientNode[clientNodeSize];
        demandDyn = new int[clientNodeSize][T];
        demandDynBase = new int[clientNodeSize][T];
        demandT = new int[T];
        streamGroupMax = new int[T][100];
        centerCostLow = new int[T];
        demandTBig = new int[T];
        demandDynBig = new int[clientNodeSize][T];
        cacheChange = (ArrayList<Integer>[]) new ArrayList[T];
        for(int i = 0; i < T; i++){
            cacheChange[i] = new ArrayList<Integer>();
        }
        ArrayList<Stream>[] streamTbig = (ArrayList<Stream>[])new ArrayList[T];
        for(int t = 0; t < T; t++){
            streamTbig[t] = new ArrayList<Stream>();
        }

        for(int i = 0; i < edgeNodeSize; i++){     //初始化边缘节点
            String name = site_bandwidth.get(i + 1).get(0);
            int bandwidth = Integer.parseInt(site_bandwidth.get(i + 1).get(1));
            int[] bandwidthA = new int[T];
            ArrayList<Integer> avalibleClient = new ArrayList<Integer>();
            for(int j = 0; j < clientNodeSize; j++){
                if(Integer.parseInt(qos.get(edgeNameInd[i]+1).get(clientNameInd[j]+1)) < qos_constraint){  //得到可用客户节点列表
                    avalibleClient.add(j);
                }
            }
            edgeNodeSet[i] = new EdgeNode(name, bandwidth, avalibleClient);
            edgeNodeSet[i].bandwidthUsed = new int[T];
            edgeNodeSet[i].bandwidthCache = new int[T];
            edgeNodeSet[i].usedMaxBandwidth = base_cost * 21 - 1;
        }


        streamMax = 0;

        ArrayList<Integer> edgelist4 = sortEdge2(edgeNodeSet);

        ArrayList<Integer> edgelist5 = sortEdge4(edgeNodeSet);   //带宽乘以度由大到小排序


        for(int i = 0; i < clientNodeSize; i++){    //初始化用户节点数组
            int[] clientdemand = new int[T];
            ArrayList<Integer> avalibleEdgeNode = new ArrayList<Integer>();
            ArrayList<Integer> avalibleEdgeNode2 = new ArrayList<Integer>();
            for (int j : edgelist5) {
                if (Integer.parseInt(qos.get(edgeNameInd[j] + 1).get(clientNameInd[i] + 1)) < qos_constraint) {
                    avalibleEdgeNode.add(j);
                }
            }
            for (int j : edgelist4) {
                if (Integer.parseInt(qos.get(edgeNameInd[j] + 1).get(clientNameInd[i] + 1)) < qos_constraint) {
                    avalibleEdgeNode2.add(j);
                }
            }

            int avalibelEdgeSize = avalibleEdgeNode.size();
            clientSet[i] = new ClientNode(demand.get(0).get(i+2), avalibleEdgeNode);
            clientSet[i].streams = new Stream[T][100];
            clientSet[i].avalibleEdgeNode2 = avalibleEdgeNode2;
            clientSet[i].demandStream =(ArrayList<Stream> []) new ArrayList[T];
            int t = 0;
            String timeNow = demand.get(1).get(0);
            clientSet[i].demandStream[t] = new ArrayList<Stream>();
            int k = 0;
            for(int x = 1; x < demand.size(); x++){
                if(!demand.get(x).get(0).equals(timeNow)){
                    t++;
                    k = 0;
                    timeNow = demand.get(x).get(0);
                    clientSet[i].demandStream[t] = new ArrayList<Stream>();
                }
                int streamDemand = Integer.valueOf(demand.get(x).get(i+2));
                clientdemand[t] += streamDemand;   //这个字段可能不太需要
                demandDyn[i][t] += streamDemand;
                if(streamDemand > streamMax) streamMax = streamDemand;
                if(streamDemand > base_cost){
                    demandDynBase[i][t] += streamDemand;
                }

                demandT[t] += streamDemand;
                //System.out.println(t);
                if(streamDemand == 0) {
                    k++;
                    continue;
                }
                if(streamDemand > streamGroupMax[t][k]) streamGroupMax[t][k] = streamDemand;


                Stream thisStream = new Stream(demand.get(x).get(1), streamDemand, i,t,avalibelEdgeSize);
                if(streamDemand / 20 > base_cost){
                    demandDynBig[i][t] += streamDemand;
                    demandTBig[t] += streamDemand;
                    streamTbig[t].add(thisStream);
                }
                thisStream.sort = k;
                clientSet[i].demandStream[t].add(thisStream);
                clientSet[i].streams[t][k] = thisStream;
                Collections.sort(clientSet[i].demandStream[t]);
                k++;
            }
            clientSet[i].demand = clientdemand;
        }




        ArrayList<Integer> edgelist3 = sortEdge3(edgeNodeSet);  //决定边缘节点在客户中的顺序,先按带宽从大到小,再按度从小到大

        streamT = (ArrayList<Stream>[]) new ArrayList[T];
        for(int t = 0; t < T; t++){
            streamT[t] = new ArrayList<Stream>();
            for(int i = 0; i < clientNodeSize; i++){
                streamT[t].addAll(clientSet[i].demandStream[t]);
            }
            Collections.sort(streamT[t]);
        }



        //对时间建模
        tList = new TNode[T];
        ArrayList<TNode> tList1 = new ArrayList<TNode>();
        ArrayList<TNode> tList2 = new ArrayList<TNode>();
        for(int t = 0; t < T; t++){
            for(int k = 0; k < 100; k++){
                centerCostLow[t] += streamGroupMax[t][k];
            }
            TNode t1 = new TNode(t, centerCostLow[t]);

            t1.demandAll = demandT[t];
            t1.centerCostHigh = demandT[t];
            tList[t] = t1;
            tList1.add(t1);
            tList2.add(t1);
        }

        //对T的理论中心节点价格从小到大排个序
        Collections.sort(tList1, new Comparator<TNode>() {
            @Override
            public int compare(TNode o1, TNode o2) {
                return Integer.compare(o1.centerCostLow,o2.centerCostLow);
            }
        });
        centerPriceMin = tList1.get(T - T/20 - 1).centerCostLow;

        //对T的需求从大到小排序
        Collections.sort(tList2, new Comparator<TNode>() {
            @Override
            public int compare(TNode o1, TNode o2) {
                return Integer.compare(o1.demandAll,o2.demandAll);
            }
        });


        //将理论上最低价格最高的百分之五设置为中心节点的max5时刻
        for(int t = T - T/22; t < T; t++){
//            tList2.get(t).centerMax5 = true;
//            tList2.get(t).centerCostLow = tList2.get(t).centerCostHigh;
        }





        //输出显示一下没分出去的总需求
        int demandSum2 = 0;
        ArrayList<Integer> sortTList = sortT(demandT);
        for(int t = 0; t < sortTList.size(); t++){
            demandSum2 += demandT[t];
        }
        System.out.println(demandSum2);




/*        int edgeEnd = 0;
        ArrayList<Integer> edgeSuper = new ArrayList<Integer>();
        for(int j = edgelist3.size() -  edgeEnd; j < edgelist3.size(); j++){   //初始化超级节点
            edgeSuper.add(edgelist3.get(j));
        }*/

        edgelist3 = sortEdge10(edgeNodeSet);  //决定边缘节点在客户中的顺序,先按带度从大到小,再按带宽从大到小,再人为选取一些度大的边缘节点排在最后


        //按事先排好的顺序选取边缘节点,再对每一个边缘节点进行Top5时刻的选取,再进行分配
        for(int j1 = 0 ; j1 < edgelist3.size() ; j1++) {
            int j = edgelist3.get(j1);
            if(edgeNodeSet[j].max5.size() == T/20) continue;

            int[] max5ThisEdge = sortList18(j);  //选取时刻，每选取一个时刻，就将该时刻百分之一小缓存边缘节点标记为当前节点

            for (int t = 0; t < max5ThisEdge.length; t++) {
                edgeNodeSet[j].max5.add(max5ThisEdge[t]);    //将选取的时刻标记为边缘节点的Top5时刻
            }


            for(int t : max5ThisEdge){

                int streamInd = 0;
                if(tList[t].centerCostHigh <= centerPriceMin || tList[t].centerMax5){
                    while(true) {
                        if(edgeNodeSet[j].max5.contains(t + 1) || t == T - 1){
                            if (streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= edgeNodeSet[j].bandwidth)
                                break;//如果流分完了或者该边缘节点的带宽分完了退出去
                        }else if(t + 1 < T && cacheChange[t + 1].contains(j)){

                            if(streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= Math.min(100 * base_cost + 99,edgeNodeSet[j].bandwidth)){
                                break;

                            }
                        }else{
                            if (streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= 18 * base_cost){
                                break;//如果流分完了或者该边缘节点的带宽分完了退出去
                            }
                        }
                        Stream s = streamT[t].get(streamInd);
                        if(!edgeNodeSet[j].avalibleClient.contains(s.source)){
                            streamInd++;
                            continue;
                        }
                        if(streamJudge(j, t, s.demand)){
                            s.destination = j;
                            demandT[t] -= s.demand;
                            tList[t].demandAll -= s.demand;
                            if(s.demand >= base_cost * 20 + 19) {
                                demandTBig[t] -= s.demand;
                                demandDynBig[s.source][t] -= s.demand;
                            }
                            if(s.demand > base_cost) demandDynBase[s.source][t] -= s.demand;
                            demandDyn[s.source][t] -= s.demand;
                            edgeNodeUpdate(j,t,s.demand);
                            streamT[t].remove(streamInd);//分了之后移除该streamInd
                            continue;
                        }
                        streamInd++;
                    }
                }
                else {
                    //  HashMap<Integer, ArrayList> streamGroup = new HashMap<Integer, ArrayList>();
                    int streamInd1 = 0;
                    int[] streamGroupDemand = new int[100];
                    int[] streamBigGroupDemand = new  int[100];
                    while(true){
                        if(streamInd1 >= streamT[t].size()) break;
                        Stream s = streamT[t].get(streamInd1);
                        if(!edgeNodeSet[j].avalibleClient.contains(s.source)) {
                            streamInd1++;
                            continue;
                        }
                        streamGroupDemand[s.sort] += s.demand;
                        if(s.demand > base_cost){
                            streamBigGroupDemand[s.sort] += s.demand;
                        }
                        streamInd1++;
                    }


                    while (true) {
                        if(edgeNodeSet[j].max5.contains(t + 1) || t == T - 1){
                            if (streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= edgeNodeSet[j].bandwidth)
                                break;//如果流分完了或者该边缘节点的带宽分完了退出去
                        }else if(t + 1 < T && cacheChange[t + 1].contains(j)){
                            if(streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= Math.min(100 * base_cost + 99,edgeNodeSet[j].bandwidth)){
                                break;
                            }
                        }else{
                            if (streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= 18 * base_cost){
                                break;//如果流分完了或者该边缘节点的带宽分完了退出去
                            }
                        }


                        //选流
                        int sortChoose = -1;
                        int streamBigGroupDemandSum = 0;


                        for (int i = 0; i < streamBigGroupDemand.length; i++) {
                            streamBigGroupDemandSum += streamBigGroupDemand[i];
                        }
                        if (streamBigGroupDemandSum == 0){
                            int tempGroupDemand = 0;
                            for(int i = 0; i < streamGroupDemand.length; i++){
                                if(streamGroupDemand[i] > tempGroupDemand){
                                    tempGroupDemand = streamGroupDemand[i];
                                    sortChoose = i;
                                }
                            }
                        } else {
                            int tempGroupDemand = 0;
                            for (int i = 0; i < streamBigGroupDemand.length; i++) {
                                if (streamBigGroupDemand[i] > tempGroupDemand) {
                                    tempGroupDemand = streamBigGroupDemand[i];
                                    sortChoose = i;
                                }
                            }
                        }

                        if(sortChoose == -1) break;
                        // System.out.println(sortChoose+":" + streamGroupDemand[sortChoose]);
                        int streamInd2 = 0;
                        int getMax = 0;
                        int getAll = 0;
                        while(true){
                            //在这里写
                            if(streamInd2 >= streamT[t].size()) break;
                            Stream s = streamT[t].get(streamInd2);
                            if(s.sort != sortChoose){
                                streamInd2++;
                                continue;
                            }
                            if(!edgeNodeSet[j].avalibleClient.contains(s.source)){
                                streamInd2++;
                                continue;
                            }
                            if(streamJudge(j,t,s.demand)){
                                //   System.out.println("ss");
                                s.destination = j;
                                demandT[t] -= s.demand;
                                tList[t].demandAll -= s.demand;
                                if(s.demand >= base_cost * 20 + 19) {
                                    demandTBig[t] -= s.demand;
                                    demandDynBig[s.source][t] -= s.demand;
                                }
                                if(s.demand > getMax) getMax = s.demand;
                                getAll += s.demand;
                                if(s.demand > base_cost) demandDynBase[s.source][t] -= s.demand;
                                demandDyn[s.source][t] -= s.demand;
                                edgeNodeUpdate(j,t,s.demand);
                                //	edgeNodeSet[j].bandwidthAvalible[t] -= s.demand;
                                streamT[t].remove(streamInd2);//分了之后移除该streamInd
                                continue;
                            }
                            streamInd2++;
                        }

                        streamGroupDemand[sortChoose] = 0;
                        streamBigGroupDemand[sortChoose] = 0;
                        //维护一下该时刻的最低中心价格
                        int tempMax = 0;   //找到当前时刻该种流没分出去的最大值
                        for (int i = 0; i < clientNodeSize; i++) {
                            if (clientSet[i].streams[t][sortChoose] == null) continue;
                            Stream ss = clientSet[i].streams[t][sortChoose];
                            if (ss.destination == -1) {
                                if (ss.demand > tempMax) {
                                    tempMax = ss.demand;
                                }
                            }
                        }
                        tList[t].centerCostLow += Math.min(tempMax, getMax);
                        //维护该时刻的最高中心价格
                        tList[t].centerCostHigh = tList[t].centerCostHigh + getMax - getAll;
                    }
                }
                if(!edgeNodeSet[j].max5.contains(t + 1)){
                    if(edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] > edgeNodeSet[j].usedMaxBandwidth)
                        edgeNodeSet[j].usedMaxBandwidth = edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t];
                }
            }

            Collections.sort(tList1, new Comparator<TNode>() {
                @Override
                public int compare(TNode o1, TNode o2) {
                    return Integer.compare(o1.centerCostLow,o2.centerCostLow);
                }
            });
            centerPriceMin = tList1.get(T - T/20 - 1).centerCostLow;
        }


        //此时会剩余一些边缘节点的Top5时刻没有选定，此时面向理论中心价格最高的时刻，在Top5时刻剩余的边缘节点中，选择在需求最大的边缘节点，降低该时刻的最高价格
        while(true){
            int t = maxDemandT();
            if(t == -1) break;
            //找节点
            int edgeInd = -1;
            int demandMax = 0;
            for(int j : edgelist3){  //选一个最合适的节点用它的前百分之五时刻来降低价格
                if(edgeNodeSet[j].max5.contains(t)) continue;
                if(edgeNodeSet[j].max5.size() == T/20) continue;
                if(!streamJudge(j, t, edgeNodeSet[j].bandwidth/2)) continue;
                int demandThisTime = 0;
                for(int i : edgeNodeSet[j].avalibleClient){
                    demandThisTime += demandDyn[i][t];
                }
                if(demandThisTime > demandMax) {
                    demandMax = demandThisTime;
                    edgeInd = j;
                }
            }
            if(edgeInd == -1) {
                tList[t].demandAll = 0;
                continue;
            }
            if(t < T - 1 && !cacheChange[t + 1].contains(edgeInd) && cacheChange[t + 1].size() < 20)  cacheChange[t + 1].add(edgeInd);
            if(!tList[t].centerMax5 && tList[t].centerCostHigh > centerPriceMin){
                int streamInd = 0;
                int j = edgeInd;
                //想个办法维护最低和最高中心价格
                while(true) {
                    if(edgeNodeSet[j].max5.contains(t + 1) || t == T - 1){
                        if (streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= edgeNodeSet[j].bandwidth)
                            break;//如果流分完了或者该边缘节点的带宽分完了退出去
                    }else if(t + 1 < T && cacheChange[t + 1].contains(j)){
                        if(streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= Math.min(100 * base_cost + 99,edgeNodeSet[j].bandwidth)){
                            break;
                        }
                    }else{
                        if (streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= 18 * base_cost){
                            break;//如果流分完了或者该边缘节点的带宽分完了退出去
                        }
                    }
                    //   if(streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] == edgeNodeSet[j].bandwidth) break;//如果流分完了或者该边缘节点的带宽分完了退出去
                    Stream s = streamT[t].get(streamInd);
                    if(!edgeNodeSet[j].avalibleClient.contains(s.source)){
                        streamInd++;
                        continue;
                    }
                    //	if(edgeNodeSet[j].bandwidthAvalible[t] >= s.demand) {//把能塞进该节点的都塞进去
                    if(streamJudge(j, t, s.demand)){
                        s.destination = j;
                        int sortIndex = s.sort;
                        demandT[t] -= s.demand;
                        tList[t].demandAll -= s.demand;
                        if(s.demand >= base_cost * 20 + 19) {
                            demandTBig[t] -= s.demand;
                            demandDynBig[s.source][t] -= s.demand;
                        }
                        if(s.demand > base_cost) demandDynBase[s.source][t] -= s.demand;
                        demandDyn[s.source][t] -= s.demand;
                        edgeNodeUpdate(j,t,s.demand);
                        //	edgeNodeSet[j].bandwidthAvalible[t] -= s.demand;
                        streamT[t].remove(streamInd);//分了之后移除该streamInd


                        int getMax = s.demand;
                        int getAll = s.demand;
                        int streamInd2 = streamInd;
                        while(streamInd2 < streamT[t].size()){   //再将同样的流取走
                            if(edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] == edgeNodeSet[j].bandwidth) break;
                            Stream s1 = streamT[t].get(streamInd2);
                            if(s1.sort != sortIndex) {
                                streamInd2++;
                                continue;
                            }

                            if(!edgeNodeSet[j].avalibleClient.contains(s1.source)){
                                streamInd2++;
                                continue;
                            }
                            if(streamJudge(j, t, s1.demand)){
                                if(s1.demand > getMax) getMax = s1.demand;
                                if(s1.demand >= base_cost * 21) {
                                    demandTBig[t] -= s1.demand;
                                    demandDynBig[s1.source][t] -= s1.demand;
                                }
                                getAll += s1.demand;
                                s1.destination = j;
                                demandT[t] -= s1.demand;
                                tList[t].demandAll -= s1.demand;
                                if(s1.demand > base_cost) demandDynBase[s1.source][t] -= s1.demand;
                                demandDyn[s1.source][t] -= s1.demand;
                                edgeNodeUpdate(j,t,s1.demand);
                                //	edgeNodeSet[j].bandwidthAvalible[t] -= s.demand;
                                streamT[t].remove(streamInd2);//分了之后移除该streamInd
                                continue;
                            }
                            streamInd2++;
                        }

                        //维护一下该时刻的最低中心价格
                        int tempMax = 0;   //找到当前时刻该种流没分出去的最大值
                        for(int i = 0; i < clientNodeSize; i++){
                            if(clientSet[i].streams[t][sortIndex] == null) continue;
                            Stream ss = clientSet[i].streams[t][sortIndex];
                            if(ss.destination == -1){
                                if(ss.demand > tempMax){
                                    tempMax = ss.demand;
                                }
                            }
                        }
                        tList[t].centerCostLow += Math.min(tempMax,getMax);

                        //维护该时刻的最高中心价格
                        tList[t].centerCostHigh = tList[t].centerCostHigh + getMax - getAll;

                        continue;
                    }
                    streamInd++;

                }
                //  System.out.println(edgeNodeSet[j].bandwidthUsed[t]);

                edgeNodeSet[j].max5.add(t);
                if(edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] > edgeNodeSet[j].usedMaxBandwidth)
                    edgeNodeSet[j].usedMaxBandwidth = edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t];
            }else{
                int streamInd = 0;
                int j = edgeInd;
                while(true){
                    if(edgeNodeSet[j].max5.contains(t + 1) || t == T - 1){
                        if (streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= edgeNodeSet[j].bandwidth)
                            break;//如果流分完了或者该边缘节点的带宽分完了退出去
                    }else if(t + 1 < T && cacheChange[t + 1].contains(j)){
                        if(streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= Math.min(100 * base_cost + 99,edgeNodeSet[j].bandwidth)){
                            break;
                        }
                    }else{
                        if (streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] >= 18 * base_cost){
                            break;//如果流分完了或者该边缘节点的带宽分完了退出去
                        }
                    }
                    Stream s = streamT[t].get(streamInd);
                    if(!edgeNodeSet[j].avalibleClient.contains(s.source)){
                        streamInd++;
                        continue;
                    }
                    if(streamJudge(j, t, s.demand)){
                        s.destination = j;
                        int sortIndex = s.sort;
                        demandT[t] -= s.demand;
                        tList[t].demandAll -= s.demand;
                        if(s.demand >= base_cost * 20 + 19) {
                            demandTBig[t] -= s.demand;
                            demandDynBig[s.source][t] -= s.demand;
                        }
                        if(s.demand > base_cost) demandDynBase[s.source][t] -= s.demand;
                        demandDyn[s.source][t] -= s.demand;
                        edgeNodeUpdate(j,t,s.demand);
                        //	edgeNodeSet[j].bandwidthAvalible[t] -= s.demand;
                        streamT[t].remove(streamInd);//分了之后移除该streamInd

                        //维护最高价格
                        tList[t].centerCostHigh -= s.demand;
                        continue;
                    }
                    streamInd++;
                }
                edgeNodeSet[j].max5.add(t);
                if(edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] > edgeNodeSet[j].usedMaxBandwidth)
                    edgeNodeSet[j].usedMaxBandwidth = edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t];
            }

            Collections.sort(tList1, new Comparator<TNode>() {
                @Override
                public int compare(TNode o1, TNode o2) {
                    return Integer.compare(o1.centerCostLow,o2.centerCostLow);
                }
            });
            centerPriceMin = tList1.get(T - T/20 - 1).centerCostLow;
        }




        //分完所有边缘节点的百分之五后需要更新每个边缘节点的价格
        for(int j : edgelist3){
            EdgeNode e = edgeNodeSet[j];
            int price95 = 0;
            for(int t = 0; t < T; t++){
                if(e.max5.contains(t)) continue;
                if(e.bandwidthUsed[t] + e.bandwidthCache[t] > price95) price95 = e.bandwidthUsed[t] + e.bandwidthCache[t];
            }
            e.price95 = Math.max(price95,base_cost);
            if(e.price95 > base_cost) {
                e.price = Math.pow(e.price95 - base_cost, 2)/(double) edgeNodeSet[j].bandwidth;
            }
        }


        //对剩余的所有流进行排序
        streamAll = new ArrayList<Stream>();
        for(int t = 0; t < T; t++){
            streamAll.addAll(streamT[t]);
        }
        Collections.sort(streamAll, new Comparator<Stream>() {
            @Override
            public int compare(Stream o1, Stream o2) {
                if(o1.demand < o2.demand){   //先带宽从大到小,再度从小到大
                    return 1;
                }else if(o1.demand > o2.demand){
                    return -1;
                }else{
                    return Integer.compare(o1.avalibleEdgeSize, o2.avalibleEdgeSize);
                }
            }
        });



        //显示一下剩余流的总需求
        int demandSum = 0;
        for(int t = 0; t < sortTList.size(); t++){
            demandSum += demandT[t];
        }
        System.out.println(demandSum);



        //对剩余的所有流进行分配
        for(Stream streamThis : streamAll){
            int clientInd = streamThis.source;
            int t = streamThis.time;
            int flag = 0;  //判断程序是否需要跳出


            //第一优先级 若该流需求不大于base-cost,判断是否能够分到某个边缘节点的后95时刻上而不超过base-cost
            if(streamThis.demand <= base_cost){
                for(int j : clientSet[clientInd].avalibleEdgeNode2){
                    if(edgeNodeSet[j].bandwidthUsed[t] + edgeNodeSet[j].bandwidthCache[t] + streamThis.demand > base_cost){
                        continue;
                    }
                    if(!streamJudge(j, t, streamThis.demand)) continue;
                    if(ifOverPrice(j, t, streamThis.demand)) continue;
                    if((!edgeNodeSet[j].max5.contains(t)) ){
                        streamThis.destination = j;
                        edgeNodeUpdate2(j, t, streamThis.demand);
                        //edgeNodeSet[j].bandwidthAvalible[t] -= streamThis.demand;
                        flag = 1;
                        break;
                    }
                }
            }
            if(flag == 1) continue;


            //第二优先级 此时所有节点的max5时刻已用完,后95时刻一定会超过base-cost,需要找到不超过最大费用的边缘节点
            for(int j : clientSet[clientInd].avalibleEdgeNode2){
                if(ifOverPrice(j, t, streamThis.demand)) continue;
                if(streamJudge(j, t, streamThis.demand)){
                    streamThis.destination = j;
                    edgeNodeUpdate2(j, t, streamThis.demand);
                    flag = 1;
                    break;
                }
            }
            if(flag == 1) continue;




            //第六优先级 此时必须要增加节点的价格了,且该价格一定会超过基本价格,需要判断加到哪个节点上
            int edgeInd = -1;
            double priceIf = 0;
            double temp = 0;
            for(int j : clientSet[clientInd].avalibleEdgeNode){
/*			for(int j1 = clientSet[clientInd].avalibleEdgeNode.size() - 1; j1 >= 0; j1--){
				int j = clientSet[clientInd].avalibleEdgeNode.get(j1);*/
                if(!streamJudge(j, t, streamThis.demand)) continue;
                int price95If = ifPrice95(j, t, streamThis.demand);
                //这里恐怕只是单纯考虑了带宽没有考虑度,是否可以考虑给度大的节点多一点容错空间
                double priceIfNow = Math.pow(price95If - base_cost , 2)/(double) edgeNodeSet[j].bandwidth;
                double priceDifference =  (priceIfNow - edgeNodeSet[j].price)/Math.pow(edgeNodeSet[j].avalibleClient.size(),1);
                //	System.out.println(priceDifference);
                //if(edgeNodeSet[j].bandwidthAvalible[t] >= streamThis.demand && temp > edgeNodeSet[j].bandwidth - edgeNodeSet[j].bandwidthAvalible[t] + streamThis.demand){
                if(edgeInd == -1 || priceDifference < temp){
                    //  System.out.println("第六优先级" +":"+streamThis.demand);
                    temp = priceDifference;
                    edgeInd = j;
                    priceIf = priceIfNow;
                }
            }
            if(edgeInd != -1){
                streamThis.destination = edgeInd;
                edgeNodeUpdate2(edgeInd, t, streamThis.demand);
                flag = 1;
            }

            if(flag == 0){
                System.out.println(streamThis.demand +","+ "该流的度" +":" + clientSet[clientInd].avalibleEdgeNode.size() );
            }

        }



        //选定一下每个时刻剩余的小缓存节点
        for(int t = 1; t < T; t++){
            int[] demandEdge = new int[edgeNodeSet.length];
            for(int j = 0; j < edgeNodeSet.length; j++){
                if(cacheChange[t].contains(j)) continue;
                demandEdge[j] = edgeNodeSet[j].bandwidthCache[t - 1] + edgeNodeSet[j].bandwidthUsed[t - 1];
            }
            for(int x = cacheChange[t].size(); x < 20; x++){
                int temp = 0;
                int tempJ = -1;
                for(int j = 0; j < demandEdge.length; j++){
                    if(temp < demandEdge[j]){
                        temp = demandEdge[j];
                        tempJ = j;
                    }
                }
                if(tempJ == -1) break;
                cacheChange[t].add(tempJ);
                demandEdge[tempJ] = 0;
            }
            for(int x = cacheChange[t].size(); x < 20; x++){
                for(int j = 0; j < edgeNodeSize; j++){
                    if(!cacheChange[t].contains(j)) {
                        cacheChange[t].add(j);
                        break;
                    }
                }
            }
        }


        output();
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);

    }


    public static int maxDemandT(){   //找到理论最低价格最高的时刻开始降低价格
        int temp = 0;
        int result = -1;
        for(TNode t : tList){
            if(t.centerMax5) continue;
            if(t.demandAll == 0) continue;
            if(t.centerCostHigh <= centerPriceMin) continue;
            if(temp < t.centerCostHigh){
                temp = t.centerCostHigh;
                result = t.time;
            }
        }
        return result;
    }



    public static boolean streamJudge(int j, int t, int demand){ //用于判断当前需求为demand的流是否能够在t时刻卸载到边缘节点j上
        boolean result = true;
        EdgeNode e = edgeNodeSet[j];
        int usedThisTime = demand + e.bandwidthUsed[t];
        int cacheThisTime = e.bandwidthCache[t];
        if(usedThisTime + cacheThisTime > e.bandwidth * 400 / 401 || usedThisTime + cacheThisTime > e.bandwidth - base_cost/20 ){
            result = false;
            return result;
        }
        while(true){
            if(usedThisTime + cacheThisTime > e.bandwidth){
                result = false;
                return result;
            }
            t++;
            if(t >= T) break;
            if(cacheChange[t].contains(j)) {
                //  cacheThisTime = (edgeNodeSet[j].bandwidthCache[t - 1] + edgeNodeSet[j].bandwidthUsed[t - 1])/100;
                cacheThisTime = (usedThisTime + cacheThisTime)/100;
            }else {
                // cacheThisTime = (edgeNodeSet[j].bandwidthCache[t - 1] + edgeNodeSet[j].bandwidthUsed[t1 - 1])/20;
                cacheThisTime = (usedThisTime + cacheThisTime)/20;
            }
            if(cacheThisTime == e.bandwidthCache[t]) break;
            usedThisTime = e.bandwidthUsed[t];
        }
        return result;
    }

    public static boolean ifOverPrice(int j, int t, int demand){   //判断demand需求大小的流在t时刻加在边缘节点j上是否会导致价格的增加
        boolean result = false;
        EdgeNode e = edgeNodeSet[j];
        int usedThisTime = demand + e.bandwidthUsed[t];
        int cacheThisTime = e.bandwidthCache[t];
        if((!edgeNodeSet[j].max5.contains(t) )&& usedThisTime + cacheThisTime > edgeNodeSet[j].price95 ){
            result = true;
            return result;
        }
        while(true){
            if(!edgeNodeSet[j].max5.contains(t)&& usedThisTime + cacheThisTime > edgeNodeSet[j].price95){
                result = true;
                return result;
            }
            t++;
            if(t >= T) break;

            if(cacheChange[t].contains(j)) {
                //  cacheThisTime = (edgeNodeSet[j].bandwidthCache[t - 1] + edgeNodeSet[j].bandwidthUsed[t - 1])/100;
                cacheThisTime = (usedThisTime + cacheThisTime)/100;
            }else {
                // cacheThisTime = (edgeNodeSet[j].bandwidthCache[t - 1] + edgeNodeSet[j].bandwidthUsed[t1 - 1])/20;
                cacheThisTime = (usedThisTime + cacheThisTime)/20;
            }
            //cacheThisTime = (usedThisTime + cacheThisTime)/20;
            if(cacheThisTime == e.bandwidthCache[t]) break;
            usedThisTime = e.bandwidthUsed[t];
        }
        return result;
    }

    public static int ifPrice95(int j, int t, int demand){
        int result = 0;
        EdgeNode e = edgeNodeSet[j];
        int usedThisTime = demand + e.bandwidthUsed[t];
        int cacheThisTime = e.bandwidthCache[t];
        if((!edgeNodeSet[j].max5.contains(t) )&& usedThisTime + cacheThisTime > edgeNodeSet[j].price95 ){
            result = usedThisTime + cacheThisTime;
            // return result;
        }
        while(true){
            if(!edgeNodeSet[j].max5.contains(t)&& usedThisTime + cacheThisTime > Math.max(edgeNodeSet[j].price95, result)){
                result = usedThisTime + cacheThisTime;
                // return result;
            }
            t++;
            if(t >= T) break;
            if(cacheChange[t].contains(j)) {
                cacheThisTime = (usedThisTime + cacheThisTime)/100;
            }else {
                cacheThisTime = (usedThisTime + cacheThisTime)/20;
            }
            if(cacheThisTime == e.bandwidthCache[t]) break;
            usedThisTime = e.bandwidthUsed[t];
        }
        return result;
    }


    public static void edgeNodeUpdate(int j, int t, int demand){  //当流卸载到边缘节点上后,更新流的状态
        edgeNodeSet[j].bandwidthUsed[t] += demand;
        for(int t1 = t + 1; t1 < T; t1++){
            int cacheThisTime = 0;
            if(cacheChange[t1].contains(j)) {
                cacheThisTime = (edgeNodeSet[j].bandwidthCache[t1 - 1] + edgeNodeSet[j].bandwidthUsed[t1 - 1])/100;
            }else {
                cacheThisTime = (edgeNodeSet[j].bandwidthCache[t1 - 1] + edgeNodeSet[j].bandwidthUsed[t1 - 1])/20;
            }
            if(cacheThisTime == edgeNodeSet[j].bandwidthCache[t1]) break;
            edgeNodeSet[j].bandwidthCache[t1] = cacheThisTime;
        }
    }

    public static void edgeNodeUpdate2(int j, int t, int demand){  //当流卸载到边缘节点上后,更新边缘节点的状态,并更新price
        edgeNodeSet[j].bandwidthUsed[t] += demand;
        if(!edgeNodeSet[j].max5.contains(t)){
            if(edgeNodeSet[j].bandwidthCache[t] + edgeNodeSet[j].bandwidthUsed[t] > edgeNodeSet[j].price95){
                edgeNodeSet[j].price95 = edgeNodeSet[j].bandwidthCache[t] + edgeNodeSet[j].bandwidthUsed[t];
                if(edgeNodeSet[j].price95 > base_cost)
                    edgeNodeSet[j].price = Math.pow(edgeNodeSet[j].price95 - base_cost, 2)/(double)edgeNodeSet[j].bandwidth;
            }
        }
        for(int t1 = t + 1; t1 < T; t1++){
            int cacheThisTime = 0;
            if(cacheChange[t1].contains(j)) {
                cacheThisTime = (edgeNodeSet[j].bandwidthCache[t1 - 1] + edgeNodeSet[j].bandwidthUsed[t1 - 1])/100;
            }else {
                cacheThisTime = (edgeNodeSet[j].bandwidthCache[t1 - 1] + edgeNodeSet[j].bandwidthUsed[t1 - 1])/20;
            }
            if(cacheThisTime == edgeNodeSet[j].bandwidthCache[t1]) break;
            edgeNodeSet[j].bandwidthCache[t1] = cacheThisTime;
            if(edgeNodeSet[j].max5.contains(t1)) continue;
            if(edgeNodeSet[j].bandwidthCache[t1] + edgeNodeSet[j].bandwidthUsed[t1] > edgeNodeSet[j].price95){
                edgeNodeSet[j].price95 = edgeNodeSet[j].bandwidthCache[t1] + edgeNodeSet[j].bandwidthUsed[t1];
                if(edgeNodeSet[j].price95 > base_cost)
                    edgeNodeSet[j].price = Math.pow(edgeNodeSet[j].price95 - base_cost, 2)/(double)edgeNodeSet[j].bandwidth;
            }

        }
    }


    public static ArrayList<List<String>>  getdata(String filepath){
        int i =0;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line1 = null;
        ArrayList demand = new ArrayList<List<String>>();
        try {
            while((line1 = br.readLine()) != null){
                String[] x = line1.split(",");  //已经拿到数组了
                List<String> x1 = Arrays.asList(x);  //又把数组转为列表了
                demand.add(x1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return demand;
    }

    public static int[] getini(){
        String filepath = "/data/config.ini";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line1 = null;
        int qos = 0;
        int cost = 0;
        double center = 0;
        try {
            br.readLine();  //先读一行
            line1 = br.readLine();
            String[] x = line1.split("=");
            line1 = br.readLine();
            qos = Integer.parseInt(x[1]);
            x = line1.split("=");
            cost = Integer.parseInt(x[1]);
            line1 = br.readLine();
            x = line1.split("=");
            center = Double.parseDouble(x[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new int[]{qos, cost};
    }

    public static double getiniCenter(){
        String filepath = "/data/config.ini";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line1 = null;
        double center = 0;
        try {
            br.readLine();  //先读一行
            br.readLine();
            br.readLine();
            line1 = br.readLine();
            String[] x = line1.split("=");
            center = Double.parseDouble(x[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return center;
    }

    public static ArrayList<Integer> sortT(int[] demand){
        ArrayList<Integer>  result = new ArrayList<Integer>();
        HashSet<Integer> sethash = new HashSet<Integer>();
        ArrayList<Integer> sortlist = new ArrayList<Integer>();
        for(int i = 0; i < T; i++){
            sethash.add(demand[i]);
        }
        sortlist.addAll(sethash);
        Collections.sort(sortlist);
        for(int i = 0; i < sortlist.size(); i++){
            for(int j = 0; j < T; j++){
                if(sortlist.get(i) == demand[j])
                    result.add(j);
            }
        }
        return result;
    }

    public static int[] sortList(int[] x){   //取出数组中最大的前百分之五的索引
        int[] result = new int[T/20];

        for(int i = 0; i < result.length; i++){
            int temp = 0;
            for(int j = 0; j < x.length; j++){
                if(x[j] > temp){
                    temp = x[j];
                    result[i] = j;
                }
            }
            x[result[i]] = 0;
        }
        return result;
    }

    public static int[] sortList4(int j){    //选择时刻1:1
        int[] demand = new int[T];
        for (int t = 0; t < T; t++) {
            for (int i: edgeNodeSet[j].avalibleClient) {
                demand[t] += demandDyn[i][t];
            }
        }

        ArrayList<Integer> maxList = new ArrayList<Integer>();
        for(int i = 0; i < T/40; i++){
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demand.length; t++){
                if(demand[t] > temp){
                    temp = demand[t];
                    tempT = t;
                }
            }
            demand[tempT] = 0;
            maxList.add(tempT);
            if(tempT + 1 < T && !maxList.contains(tempT + 1)) maxList.add(tempT + 1);
        }
        int[] result = new int[maxList.size()];
        for(int i = 0; i < maxList.size(); i++){
            result[i] = maxList.get(i);
        }
        Arrays.sort(result);
        return result;
    }

    public static int[] sortList18(int j){    //选择时刻1:1
        int[] demand = new int[T];
        int[] demandBase = new int[T];//初始化大流矩阵
        for (int t = 0; t < T; t++) {
            for (int i: edgeNodeSet[j].avalibleClient) {
                demandBase[t] += demandDynBase[i][t];  //会出现大流比较少的情况
                demand[t] += demandDyn[i][t];  //会出现大流比较少的情况
            }
        }


        ArrayList<Integer> maxList = new ArrayList<Integer>();
        while(maxList.size() < T/20 - 1){
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demandBase.length; t++){
                if(demandBase[t] > temp){
                    temp = demandBase[t];
                    tempT = t;
                }
            }
            if(temp < 10 *base_cost) break;
            demandBase[tempT] = 0;
            demand[tempT] = 0;
            if(maxList.contains(tempT)) continue;
            if(tempT + 1 < T && cacheChange[tempT + 1].size() < 20) {
                maxList.add(tempT);
                cacheChange[tempT + 1].add(j);
            } else if(tempT + 1 < T && cacheChange[tempT + 1].size() >= 20){
                maxList.add(tempT);
                if(tempT + 1 < T && !maxList.contains(tempT + 1)) maxList.add(tempT + 1);
            }else if(tempT + 1 == T) {
                maxList.add(tempT);
            }

        }

        while(maxList.size() < T/20 - 1){
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demand.length; t++){
                if(demand[t] > temp){
                    temp = demand[t];
                    tempT = t;
                }
            }
            demand[tempT] = 0;
            if(maxList.contains(tempT)) continue;

            if(tempT + 1 < T && cacheChange[tempT + 1].size() < 20) {
                maxList.add(tempT);
                cacheChange[tempT + 1].add(j);
            } else if(tempT + 1 < T && cacheChange[tempT + 1].size() >= 20){
                maxList.add(tempT);
                if(tempT + 1 < T && !maxList.contains(tempT + 1)) maxList.add(tempT + 1);
            }else if(tempT + 1 == T) {
                maxList.add(tempT);
            }
            //  if(tempT + 1 < T && !maxList.contains(tempT + 1)) maxList.add(tempT + 1);
        }
        int[] result = new int[maxList.size()];
        for(int i = 0; i < maxList.size(); i++){
            result[i] = maxList.get(i);
        }
        Arrays.sort(result);
        return result;
    }

    public static int[] sortList14(int j){    //选择时刻1:1
        int[] demand = new int[T];
        for (int t = 0; t < T; t++) {
            for (int i: edgeNodeSet[j].avalibleClient) {
                demand[t] += demandDyn[i][t];
            }
        }

        ArrayList<Integer> maxList = new ArrayList<Integer>();
        while(maxList.size() < T/20 - 1){
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demand.length; t++){
                if(demand[t] > temp){
                    temp = demand[t];
                    tempT = t;
                }
            }
            demand[tempT] = 0;
            if(maxList.contains(tempT)) continue;
            if(tempT + 1 < T && cacheChange[tempT + 1].size() < 20) {
                maxList.add(tempT);
                cacheChange[tempT + 1].add(j);
            } else if(tempT + 1 < T && cacheChange[tempT + 1].size() >= 20){
                maxList.add(tempT);
                if(tempT + 1 < T && !maxList.contains(tempT + 1)) maxList.add(tempT + 1);
            }else if(tempT + 1 == T) {
                maxList.add(tempT);
            }
            //  if(tempT + 1 < T && !maxList.contains(tempT + 1)) maxList.add(tempT + 1);
        }
        int[] result = new int[maxList.size()];
        for(int i = 0; i < maxList.size(); i++){
            result[i] = maxList.get(i);
        }
        Arrays.sort(result);
        return result;
    }


    public static int[] sortList15(int j){    //选择时刻1:1
        int[] demand = new int[T];
        int[] demandBase = new int[T];//初始化大流矩阵
        for (int t = 0; t < T; t++) {
            for (int i: edgeNodeSet[j].avalibleClient) {
                demandBase[t] += demandDynBase[i][t];  //会出现大流比较少的情况
                demand[t] += demandDyn[i][t];  //会出现大流比较少的情况
            }
        }


        ArrayList<Integer> maxList = new ArrayList<Integer>();
        while(maxList.size() < T/20 - 1){
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demand.length; t++){
                if(demand[t] > temp){
                    temp = demand[t];
                    tempT = t;
                }
            }
            demand[tempT] = 0;

            if(tempT + 1 < T && cacheChange[tempT + 1].size() < 20) {
                maxList.add(tempT);
                cacheChange[tempT + 1].add(j);
            } else if(tempT + 1 < T && cacheChange[tempT + 1].size() >= 20){
                maxList.add(tempT);
                if(tempT + 1 < T && !maxList.contains(tempT + 1)) maxList.add(tempT + 1);
            }else if(tempT + 1 == T) {
                maxList.add(tempT);
            }
            //  if(tempT + 1 < T && !maxList.contains(tempT + 1)) maxList.add(tempT + 1);
        }
        int[] result = new int[maxList.size()];
        for(int i = 0; i < maxList.size(); i++){
            result[i] = maxList.get(i);
        }
        Arrays.sort(result);
        return result;
    }


    public static int[] sortList6(int j){ //寻找两个需求最大的连续时刻
        int[] demand = new int[T];
        for (int t = 0; t < T; t++) {
            for (int i: edgeNodeSet[j].avalibleClient) {
                demand[t] += demandDyn[i][t];
            }
        }

        ArrayList<Integer> maxList = new ArrayList<Integer>();
        for(int i = 0; i < (T/20)/3; i++){
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demand.length - 1; t++){
                if(demand[t] + demand[t + 1] > temp){
                    temp = demand[t] + demand[t + 1];
                    tempT = t;
                }
            }
            demand[tempT] = 0;
            demand[tempT + 1] = 0;
            if(!maxList.contains(tempT)){
                demand[tempT] = 0;
                maxList.add(tempT);
            }
            if(!maxList.contains(tempT + 1)){
                demand[tempT + 1] = 0;
                maxList.add(tempT + 1);
            }
            if(tempT + 2 < T && !maxList.contains(tempT + 2)) {
                maxList.add(tempT + 2);
                // demand[tempT + 2] = 0;
            }
        }
        int[] result = new int[maxList.size()];
        for(int i = 0; i < maxList.size(); i++){
            result[i] = maxList.get(i);
        }
        System.out.println(result.length);
        Arrays.sort(result);
        return result;
    }


    public static int[] sortList7(int j){ //动态选择连续两个时刻或者一个时刻
        int[] demand = new int[T];
        for (int t = 0; t < T; t++) {
            for (int i: edgeNodeSet[j].avalibleClient) {
                demand[t] += demandDyn[i][t];
            }
        }

        ArrayList<Integer> maxList = new ArrayList<Integer>();
        while(maxList.size() <= T/20 - 2){
            int temp = 0;
            int tempT = -1;
            int temp2 = 0;
            int tempT2 = -1;
            for(int t = 0; t < demand.length - 1; t++){
                if(demand[t] + demand[t + 1] > temp){
                    temp2 = demand[t] + demand[t + 1];
                    tempT2 = t;
                }
                if(demand[t] > temp){
                    temp = demand[t];
                    tempT = t;
                }
            }
            if(demand[T - 1] > temp){
                temp = demand[T - 1];
                tempT = T - 1;
            }

            if(temp2 > temp*13/10 && maxList.size() <= T/20 - 3){  //此时选择两个时刻
                demand[tempT2] = 0;
                demand[tempT2 + 1] = 0;
                if(!maxList.contains(tempT2)){
                    maxList.add(tempT2);
                }
                if(!maxList.contains(tempT2 + 1)){
                    maxList.add(tempT2 + 1);
                }
                if(tempT2 + 2 < T && !maxList.contains(tempT2 + 2)) {
                    maxList.add(tempT2 + 2);
                }
            }else if(maxList.size() <= T/20 - 2){
                demand[tempT] = 0;
                if(!maxList.contains(tempT)){
                    maxList.add(tempT);
                }
                if(tempT + 1 < T && !maxList.contains(tempT + 1)) {
                    maxList.add(tempT + 1);
                }
            }else break;
        }

        int[] result = new int[maxList.size()];
        for(int i = 0; i < maxList.size(); i++){
            result[i] = maxList.get(i);
        }
        System.out.println(result.length);
        Arrays.sort(result);
        return result;
    }



    public static int[] sortList8(int j){ //寻找两个需求最大的连续时刻,先分大流多的时刻,再分整体多的时刻
        int[] demand = new int[T];
        int[] demandBase = new int[T];//初始化大流矩阵
        for (int t = 0; t < T; t++) {
            for (int i: edgeNodeSet[j].avalibleClient) {
                demandBase[t] += demandDynBase[i][t];  //会出现大流比较少的情况
                demand[t] += demandDyn[i][t];  //会出现大流比较少的情况
            }
        }

        ArrayList<Integer> maxList = new ArrayList<Integer>();
        for(int i = 0; i < (T/20)/3; i++){
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demandBase.length - 1; t++){
                if(demandBase[t] + demandBase[t + 1] > temp){
                    temp = demandBase[t] + demandBase[t + 1];
                    tempT = t;
                }
            }
            if(temp < base_cost * 20) break;
            demandBase[tempT] = 0;
            demand[tempT] = 0;
            demandBase[tempT + 1] = 0;
            demand[tempT + 1] = 0;
            if(!maxList.contains(tempT)){
//                demandBase[tempT] = 0;
//                demand[tempT] = 0;
                maxList.add(tempT);
            }
            if(!maxList.contains(tempT + 1)){
//                demandBase[tempT + 1] = 0;
//                demand[tempT + 1] = 0;
                maxList.add(tempT + 1);
            }
            if(tempT + 2 < T && !maxList.contains(tempT + 2)) {
                maxList.add(tempT + 2);
                // demand[tempT + 2] = 0;
            }
        }
        int lastT = T/20 - maxList.size();
        //  System.out.println(maxList.size() + ":" + lastT);
        for(int i = 0; i < lastT/3; i++) {
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demand.length - 1; t++){
                if(demand[t] + demand[t + 1] > temp) {
                    temp = demand[t] + demand[t + 1];
                    tempT = t;
                }
            }
            demand[tempT] = 0;
            demand[tempT + 1] = 0;
            if(!maxList.contains(tempT)){
//                demand[tempT] = 0;
                maxList.add(tempT);
            }
            if(!maxList.contains(tempT + 1)){
//                demand[tempT + 1] = 0;
                maxList.add(tempT + 1);
            }
            if(tempT + 2 < T && !maxList.contains(tempT + 2)) {
                maxList.add(tempT + 2);
            }

        }
        int[] result = new int[maxList.size()];
        for(int i = 0; i < maxList.size(); i++){
            result[i] = maxList.get(i);
        }
        //   System.out.println("时间长度为" + result.length);
        Arrays.sort(result);
        return result;
    }


    public static int[] sortList10(int j){ //寻找连续三个最大时刻
        int[] demand = new int[T];
        int[] demandBase = new int[T];//初始化大流矩阵
        for (int t = 0; t < T; t++) {
            for (int i: edgeNodeSet[j].avalibleClient) {
                demandBase[t] += demandDynBase[i][t];  //会出现大流比较少的情况
                demand[t] += demandDyn[i][t];  //会出现大流比较少的情况
            }
        }

        ArrayList<Integer> maxList = new ArrayList<Integer>();
        for(int i = 0; i < (T/20)/4; i++){
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demandBase.length - 3; t++){
                if(demandBase[t] + demandBase[t + 1] > temp){
                    temp = demandBase[t] + demandBase[t + 1] + demandBase[t + 2];
                    tempT = t;
                }
            }
            if(temp < base_cost * 28) break;
            demandBase[tempT] = 0;
            demand[tempT] = 0;
            demandBase[tempT + 1] = 0;
            demand[tempT + 1] = 0;
            demandBase[tempT + 2] = 0;
            demand[tempT + 2] = 0;
            if(!maxList.contains(tempT)){
//                demandBase[tempT] = 0;
//                demand[tempT] = 0;
                maxList.add(tempT);
            }
            if(!maxList.contains(tempT + 1)){
//                demandBase[tempT + 1] = 0;
//                demand[tempT] = 0;
                maxList.add(tempT + 1);
            }
            if(tempT + 2 < T && !maxList.contains(tempT + 2)) {
                maxList.add(tempT + 2);
                // demand[tempT + 2] = 0;
            }
            if(tempT + 2 < T && !maxList.contains(tempT + 3)) {
                maxList.add(tempT + 3);
                // demand[tempT + 2] = 0;
            }
        }
        int lastT = T/20 - maxList.size();
        System.out.println(maxList.size() + ":" + lastT);
        for(int i = 0; i < lastT/3; i++) {
            int temp = 0;
            int tempT = -1;
            for(int t = 0; t < demand.length - 1; t++){
                if(demand[t] + demand[t + 1] > temp) {
                    temp = demand[t] + demand[t + 1];
                    tempT = t;
                }
            }
            if(!maxList.contains(tempT)){
                demand[tempT] = 0;
                maxList.add(tempT);
            }
            if(!maxList.contains(tempT + 1)){
                demand[tempT + 1] = 0;
                maxList.add(tempT + 1);
            }
            if(tempT + 2 < T && !maxList.contains(tempT + 2)) {
                maxList.add(tempT + 2);
            }

        }
        int[] result = new int[maxList.size()];
        for(int i = 0; i < maxList.size(); i++){
            result[i] = maxList.get(i);
        }
        System.out.println("时间长度为" + result.length);
        Arrays.sort(result);
        return result;
    }


    public static int[] sortList5(int j){
        int[] demand = new int[T];
        for (int t = 0; t < T; t++) {
            if(edgeNodeSet[j].max5.contains(t)) continue;
            for (int i: edgeNodeSet[j].avalibleClient) {
                demand[t] += demandDyn[i][t];
            }
        }
        int[] result = new int[T/20 - T/22];
        for(int i = 0; i < result.length; i++){
            int temp = 0;
            for(int t = 0; t < demand.length; t++){
                if(demand[t] > temp){
                    temp = demand[t];
                    result[i] = t;
                }
            }
            demand[result[i]] = 0;
        }
        Arrays.sort(result);
        return result;
    }

    public static int[] sortList3(int j, int j1, int j2){
        int tNum = T/20 - edgeNodeSet[j].max5.size();
        int[] result = new int[tNum];
        int[] demandEdgeBase = new int[T];
        //初始化大流需求矩阵
        for(int t = 0; t < T; t++){
            if(edgeNodeSet[j].max5.contains(t)) continue;
            for(int i : edgeNodeSet[j].avalibleClient){

                demandEdgeBase[t] += demandDynBase[i][t];
            }
        }

        int i = 0; //可以用来标识当前阶段抽取了几个时刻

        for(i = 0; i < result.length; i++){
            int temp = 0;
            for(int t = 0; t < demandEdgeBase.length; t++){
                if(demandEdgeBase[t] > temp){
                    temp = demandEdgeBase[t];
                    result[i] = t;
                }
            }
            if(j1 < j2/2) {
                if (temp < edgeNodeSet[j].bandwidth* 2 / 3) break;
            }
            else {
                if(temp < edgeNodeSet[j].bandwidth/2) break;
            }
            //if (temp < edgeNodeSet[j].bandwidth * 10) break;
            demandEdgeBase[result[i]] = 0;
            if(result[i] > 0)   demandEdgeBase[result[i] - 1] = 0;
        }
        if(i == result.length) return result;

        int[] demandEdge = new int[T];
        for(int t = 0; t < T; t++){
            if( edgeNodeSet[j].max5.contains(t)) continue;
            for(int clientInd : edgeNodeSet[j].avalibleClient){
                demandEdge[t] += demandDyn[clientInd][t];
            }
        }
        for(int x = 0; x < i; x++){
            demandEdge[result[x]] = 0;
            if(result[i] > 0)   demandEdge[result[i] - 1] = 0;
            //System.out.println("ss");
        }
        int x;

        for(x = i; x < result.length; x++){
            int temp = 0;
            for(int t = 0; t < demandEdge.length; t++){
                if(demandEdge[t] > temp){
                    temp = demandEdge[t];
                    result[x] = t;
                }

            }
            //	if(temp < edgeNodeSet[j].bandwidth *2 /3  && edgeNodeSet[j].avalibleClient.size() >= clientNodeSize/3) break;
            demandEdge[result[x]] = 0;
            //System.out.println(result[x]);
        }
        //System.out.println(result[T/20 - 1]);
        //   if(x == result.length) return result;
        return result;
    }



    public static int[] sortList2(int j, int j1, int j2){   //取的逻辑 可以优先取去掉小流的最大时刻,当此时发现比较小时,再选取不去掉小流的
        int[] result = new int[T/20];
        int[] demandEdgeBase = new int[T];
        //初始化大流需求矩阵
        for(int t = 0; t < T; t++){
            //  if(!streamJudge(j, t, base_cost*20)) continue;
            for(int i : edgeNodeSet[j].avalibleClient){
                demandEdgeBase[t] += demandDynBase[i][t];
            }
        }


        int i = 0; //可以用来标识当前阶段抽取了几个时刻

        for(i = 0; i < result.length; i++){
            int temp = 0;
            for(int t = 0; t < demandEdgeBase.length; t++){
                if(demandEdgeBase[t] > temp){
                    temp = demandEdgeBase[t];
                    result[i] = t;
                }
            }
            if(j1 < j2/2) {
                if (temp < edgeNodeSet[j].bandwidth* 2 / 3) break;
            }
            else {
                if(temp < edgeNodeSet[j].bandwidth/2) break;
            }
            //if (temp < edgeNodeSet[j].bandwidth * 10) break;
            demandEdgeBase[result[i]] = 0;

            if(result[i] > 0)   demandEdgeBase[result[i] - 1] = 0;
        }

        if(i == result.length) return result;

        int[] demandEdge = new int[T];
        for(int t = 0; t < T; t++){
            for(int clientInd : edgeNodeSet[j].avalibleClient){
                demandEdge[t] += demandDyn[clientInd][t];
            }
        }
        for(int x = 0; x < i; x++){
            demandEdge[result[x]] = 0;
            if(result[i] > 0)   demandEdge[result[i] - 1] = 0;
            //System.out.println("ss");
        }

        for(int t = 0; t < T; t++){
            //System.out.println(t + ":" + demandEdge[t]);
        }

        int x;

        for(x = i; x < T/20; x++){
            int temp = 0;
            for(int t = 0; t < demandEdge.length; t++){
                if(demandEdge[t] > temp){
                    temp = demandEdge[t];
                    result[x] = t;
                }

            }
            //	if(temp < edgeNodeSet[j].bandwidth *2 /3  && edgeNodeSet[j].avalibleClient.size() >= clientNodeSize/3) break;
            demandEdge[result[x]] = 0;
            //System.out.println(result[x]);
        }
        //System.out.println(result[T/20 - 1]);
        if(x == result.length) return result;


        int[] demandAllNow = new int[T];
        for(int t = 0; t < T; t++){
            demandAllNow[t] = demandT[t];
        }

        for(int y = 0; y < x; y++){
            demandAllNow[result[y]] = 0;
        }

        int y;
        for(y = x; y < T/20; y++){
            int temp = 0;
            for(int t = 0; t < demandAllNow.length; t++){
                if(demandAllNow[t] > temp){
                    temp = demandAllNow[t];
                    result[y] = t;
                }
            }
            if(demandEdge[result[y]] == 0 ){
                demandAllNow[result[y]] = 0;
                y--;
                continue;
            }

            demandEdge[result[y]] = 0;
            //System.out.println(demandAllNow[result[y]]);
        }
        return result;
    }

    public static ArrayList<Integer> sortEdge3(EdgeNode[] set){  //该排序决定边缘节点在客户中的顺序,这是带宽乘度

        ArrayList<Integer> result = new ArrayList<Integer>();
        ArrayList<EdgeNode> temp = new ArrayList<EdgeNode>();
        ArrayList<EdgeNode> temp2 = new ArrayList<EdgeNode>();
        for(int i = 0; i < edgeNodeSize; i++){
            if(set[i].avalibleClient.size() != 0 && set[i].bandwidth >= streamMax) temp.add(set[i]);
            else if(set[i].avalibleClient.size() != 0 && set[i].bandwidth < streamMax && set[i].bandwidth > base_cost) temp2.add(set[i]);
        }
        Collections.sort(temp, new Comparator<EdgeNode>() {
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
                if(o1.avalibleClient.size() > o2.avalibleClient.size()) return -1;   //这是度从大到小
                else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return 1;
                else return Integer.compare(o2.bandwidth,o1.bandwidth);      //这是带宽从大到小
                //   return Integer.compare(o2.bandwidth/o2.avalibleClient.size(), o1.bandwidth/o1.avalibleClient.size());
/*				if(o1.avalibleClient.size() > o2.avalibleClient.size()) return 1;   //这是度从小到大
				else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return -1;
				else return Integer.compare(o2.bandwidth,o1.bandwidth);*/



            }
        });
        Collections.sort(temp2, new Comparator<EdgeNode>() {
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
                if(o1.avalibleClient.size() > o2.avalibleClient.size()) return -1;   //这是度从大到小
                else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return 1;
                else return Integer.compare(o2.bandwidth,o1.bandwidth);      //这是带宽从大到小
/*				if(o1.bandwidth > o2.bandwidth) return -1;   //这是带宽从大到小
				else if(o1.bandwidth < o2.bandwidth) return 1;
				else return Integer.compare(o1.avalibleClient.size(),o2.avalibleClient.size());      //这是度从小到大*/
                //  return Integer.compare(o2.bandwidth/o2.avalibleClient.size(), o1.bandwidth/o1.avalibleClient.size());
            }
        });
        temp.addAll(temp2);
        for(int i = 0; i < temp.size(); i++){
            for(int j = 0; j < set.length; j++){
                if(temp.get(i).name.equals(set[j].name)){
                    result.add(j);
                    break;
                }
            }
        }
        return result;
    }

    public static ArrayList<Integer> sortEdge9(EdgeNode[] set){
        ArrayList<Integer> result = new ArrayList<Integer>();
        ArrayList<EdgeNode> temp = new ArrayList<EdgeNode>();
        for(int i = 0; i < edgeNodeSize; i++){
            if(set[i].avalibleClient.size() != 0 && set[i].bandwidth > base_cost) temp.add(set[i]);
        }
        Collections.sort(temp, new Comparator<EdgeNode>() {
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
                if(o1.avalibleClient.size() == o2.avalibleClient.size()) return Integer.compare(o2.bandwidth,o1.bandwidth);
                else return Integer.compare(o2.avalibleClient.size(),o1.avalibleClient.size());
            }
        });
        for(int i = 0; i < temp.size(); i++){
            for(int j = 0; j < set.length; j++){
                if(temp.get(i).name.equals(set[j].name)){
                    result.add(j);
                    break;
                }
            }
        }
        return result;
    }





    public static ArrayList<Integer> sortEdge8(EdgeNode[] set){  //该排序决定边缘节点在客户中的顺序,这是带宽乘度

        ArrayList<Integer> result = new ArrayList<Integer>();
        ArrayList<EdgeNode> temp = new ArrayList<EdgeNode>();
        ArrayList<EdgeNode> temp2 = new ArrayList<EdgeNode>();
        for(int i = 0; i < edgeNodeSize; i++){
            if(set[i].avalibleClient.size() != 0 && set[i].bandwidth >= base_cost * 21 - 1) temp.add(set[i]);
            else if(set[i].avalibleClient.size() != 0 && set[i].bandwidth < base_cost * 21 - 1 && set[i].bandwidth > base_cost) temp2.add(set[i]);
        }
        Collections.sort(temp, new Comparator<EdgeNode>() {
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
/*                if(o1.avalibleClient.size() > o2.avalibleClient.size()) return -1;   //这是度从大到小
                else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return 1;
                else return Integer.compare(o2.bandwidth,o1.bandwidth);      //这是带宽从大到小*/
                return Integer.compare(o2.bandwidth/o2.avalibleClient.size(), o1.bandwidth/o1.avalibleClient.size());
/*				if(o1.avalibleClient.size() > o2.avalibleClient.size()) return 1;   //这是度从小到大
				else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return -1;
				else return Integer.compare(o2.bandwidth,o1.bandwidth);*/



            }
        });
        Collections.sort(temp2, new Comparator<EdgeNode>() {
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
/*                if(o1.avalibleClient.size() > o2.avalibleClient.size()) return -1;   //这是度从大到小
                else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return 1;
                else return Integer.compare(o2.bandwidth,o1.bandwidth);      //这是带宽从大到小*/
/*				if(o1.bandwidth > o2.bandwidth) return -1;   //这是带宽从大到小
				else if(o1.bandwidth < o2.bandwidth) return 1;
				else return Integer.compare(o1.avalibleClient.size(),o2.avalibleClient.size());      //这是度从小到大*/
                return Integer.compare(o2.bandwidth/o2.avalibleClient.size(), o1.bandwidth/o1.avalibleClient.size());
            }
        });
        temp.addAll(temp2);
        for(int i = 0; i < temp.size(); i++){
            for(int j = 0; j < set.length; j++){
                if(temp.get(i).name.equals(set[j].name)){
                    result.add(j);
                    break;
                }
            }
        }
        return result;
    }

    public static ArrayList<Integer> sortEdge(EdgeNode[] set){

        ArrayList<Integer> result = new ArrayList<Integer>();
        ArrayList<EdgeNode> temp = new ArrayList<EdgeNode>();
        for(int i = 0; i < edgeNodeSize; i++){
            if(set[i].avalibleClient.size() != 0 && set[i].bandwidth > base_cost) temp.add(set[i]);
        }
        Collections.sort(temp);
        for(int i = 0; i < temp.size(); i++){
            for(int j = 0; j < set.length; j++){
                if(temp.get(i).name.equals(set[j].name)){
                    result.add(j);
                    break;
                }
            }
        }
        return result;
    }


    public static int bigMaxT(){   //找到大流需求最高的时刻
        int result = -1;
        int temp = 0;
        for(int t = 0; t < T; t++){
            if(demandTBig[t] > temp){
                result = t;
                temp = demandTBig[t];
            }
        }
        return result;
    }



    public static ArrayList<Integer> sortEdge2(EdgeNode[] set){   //排序第一优先级：度从小到大  第二优先级：带宽从大到小

        ArrayList<Integer> result = new ArrayList<Integer>();
        ArrayList<EdgeNode> temp = new ArrayList<EdgeNode>();
        for(int i = 0; i < edgeNodeSize; i++){
            if(set[i].avalibleClient.size() != 0 && set[i].bandwidth > base_cost) temp.add(set[i]);
        }
        Collections.sort(temp, new Comparator<EdgeNode>() {
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
                if(o1.avalibleClient.size() > o2.avalibleClient.size()) return 1;
                else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return -1;
                else return Integer.compare(o2.bandwidth,o1.bandwidth);
            }
        });
        for(int i = 0; i < temp.size(); i++){
            for(int j = 0; j < set.length; j++){
                if(temp.get(i).name.equals(set[j].name)){
                    result.add(j);
                    break;
                }
            }
        }
        return result;
    }

    public static ArrayList<Integer> sortEdge10(EdgeNode[] set){  //该排序决定边缘节点在客户中的顺序,这是带宽乘度

        ArrayList<Integer> result = new ArrayList<Integer>();
        ArrayList<EdgeNode> temp = new ArrayList<EdgeNode>();
        ArrayList<EdgeNode> temp2 = new ArrayList<EdgeNode>();
        for(int i = 0; i < edgeNodeSize; i++){
            if(set[i].avalibleClient.size() != 0 && set[i].bandwidth >= streamMax) temp.add(set[i]);
            else if(set[i].avalibleClient.size() != 0 && set[i].bandwidth < streamMax && set[i].bandwidth > base_cost) temp2.add(set[i]);
        }
        Collections.sort(temp2, new Comparator<EdgeNode>() {
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
                if(o1.avalibleClient.size() > o2.avalibleClient.size()) return -1;   //这是度从大到小
                else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return 1;
                else return Integer.compare(o1.bandwidth,o2.bandwidth);      //这是带宽从大到小
            }
        });
        ArrayList<EdgeNode> temp3 = new ArrayList<EdgeNode>();
        int size = temp2.size()/10;
        for(int i = 0; i < size; i++){
            temp3.add( temp2.get(0));
            temp2.remove(0);
        }
        temp.addAll(temp2);
        Collections.sort(temp, new Comparator<EdgeNode>() {
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
                if(o1.avalibleClient.size() > o2.avalibleClient.size()) return -1;   //这是度从大到小
                else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return 1;
                else return Integer.compare(o2.bandwidth,o1.bandwidth);      //这是带宽从大到小
            }
        });
        temp.addAll(temp3);
        for(int i = 0; i < temp.size(); i++){
            for(int j = 0; j < set.length; j++){
                if(temp.get(i).name.equals(set[j].name)){
                    result.add(j);
                    break;
                }
            }
        }
        return result;
    }


    public static ArrayList<Integer> sortEdge4(EdgeNode[] set){   //按照带宽乘以度对边缘节点排序

        ArrayList<Integer> result = new ArrayList<Integer>();
        ArrayList<EdgeNode> temp = new ArrayList<EdgeNode>();
        for(int i = 0; i < edgeNodeSize; i++){
            if(set[i].avalibleClient.size() != 0 && set[i].bandwidth > base_cost) temp.add(set[i]);
        }
        Collections.sort(temp, new Comparator<EdgeNode>() {
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
                return Integer.compare(o2.bandwidth * o2.avalibleClient.size(), o1.bandwidth * o1.avalibleClient.size());
            }
        });
        for(int i = 0; i < temp.size(); i++){
            for(int j = 0; j < set.length; j++){
                if(temp.get(i).name.equals(set[j].name)){
                    result.add(j);
                    break;
                }
            }
        }
        return result;
    }


    public static int maxT(){
        int result = 0;
        int temp = 0;
        for(int t = 0; t < T; t++){
            if(streamT[t].get(0).demand > temp){
                result = t;
                temp = streamT[t].get(0).demand;
            }
        }
        return result;
    }

    public static int maxL(int[] x){
        int result = -1;
        for(int i = 0; i < x.length; i++){
            if(result == -1 || x[i] > x[result] )
                result = i;
        }
        return result;
    }
    public static int maxArray(int[] x){
        int result = -1;
        int temp = 0;
        for(int i = 0; i < x.length; i++){
            if(x[i] > temp){
                result = i;
                temp = x[i];
            }
        }
        return result;
    }


    public static void output(){
        String path = "/output/solution.txt";
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(path));
            for (int t = 0; t < T; t++) {

                if(t != 0){
                    int flag = 0;
                    for(int j : cacheChange[t]){
                        if(flag == 1) bw.write(",");
                        bw.write(edgeNodeSet[j].name);
                        flag = 1;
                    }
                    bw.newLine();
                }

                for (int i = 0; i < clientNodeSize; i++) {

                    HashMap<String,ArrayList<String>> outMap = new HashMap<String, ArrayList<String>>();
                    //	ArrayList<String>[] out = (ArrayList<String>[]) new ArrayList[clientSet[i].avalibleEdgeNode.size()];
                    for (int k = 0; k < clientSet[i].demandStream[t].size(); k++) {

                        int edgeInd = clientSet[i].demandStream[t].get(k).destination;
                        String edgeName = edgeNodeSet[edgeInd].name;
                        if(outMap.containsKey(edgeName)){
                            outMap.get(edgeName).add(clientSet[i].demandStream[t].get(k).name);
                        }else{
                            ArrayList<String> streamName = new ArrayList<String>();
                            streamName.add(clientSet[i].demandStream[t].get(k).name);
                            outMap.put(edgeName,streamName);
                        }

/*
						if(out[edgeInd] == null)  out[edgeInd] = new ArrayList<String>();
						out[edgeInd].add(clientSet[i].demandStream[t].get(k).name);

						*/
                    }
                    int flag = 0;
                    bw.write(clientSet[i].name + ":");
                    for(Map.Entry<String, ArrayList<String>> x : outMap.entrySet()){
                        if(flag == 1) bw.write(",");
                        bw.write("<" + x.getKey());
                        for(int k = 0; k < x.getValue().size(); k++){
                            bw.write("," + x.getValue().get(k));
                        }
                        bw.write(">");
                        flag = 1;
                    }
                    if(i == clientNodeSize-1 && t == T-1) break;
                    bw.newLine();
                }
            }
            bw.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}




