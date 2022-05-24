package com.huawei.java.main;
import java.io.*;
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

        clientNodeSize = demand.get(0).size() - 2;
        edgeNodeSize = site_bandwidth.size() - 1;
        //System.out.println(demand);
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
            edgeNodeSet[i].bandwidthAvalible = new int[T];
            for(int k = 0; k < T; k++){
                edgeNodeSet[i].bandwidthAvalible[k] = bandwidth;
            }
        }
        sortEdge_top10(edgeNodeSet); //选择10个最大的带宽*度最大的边缘节点

        for (int i=0 ; i <edgeNodeSize ; i++){
            if(edgeNodeSet[i].superEdge)
            {
                System.out.println(edgeNodeSet[i].name);
            }

        }
//        System.out.println(edgeNodeSet.length);


        //	System.out.println(edgeNodeSet[0].bandwidthAvalible[0]);

        streamMax = 0;

        ArrayList<Integer> edgelist4 = sortEdge2(edgeNodeSet);


        ArrayList<Integer> edgelist5 = sortEdge4(edgeNodeSet);   //带宽乘以度由大到小排序
/*		for(int j : edgelist5){
			System.out.println(edgeNodeSet[j].bandwidth + ":" + edgeNodeSet[j].avalibleClient.size());
		}*/
        //	int edgeBan = edgelist5.get(edgelist5.size() - 1);
        //	edgelist5.remove(Integer.valueOf(edgeBan));
        //	edgelist4.remove(Integer.valueOf(edgeBan));
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
            clientSet[i].avalibleEdgeNode2 = avalibleEdgeNode2;
            clientSet[i].demandStream =(ArrayList<Stream> []) new ArrayList[T];
            int t = 0;
            String timeNow = demand.get(1).get(0);
            clientSet[i].demandStream[t] = new ArrayList<Stream>();
            for(int x = 1; x < demand.size(); x++){
                if(!demand.get(x).get(0).equals(timeNow)){
                    t++;
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
                if(streamDemand == 0) continue;
                Stream thisStream = new Stream(demand.get(x).get(1), streamDemand, i,t,avalibelEdgeSize);
                clientSet[i].demandStream[t].add(thisStream);
                Collections.sort(clientSet[i].demandStream[t]);
            }
            clientSet[i].demand = clientdemand;
        }

        ArrayList<Integer> edgelist3 = sortEdge3(edgeNodeSet);  //决定边缘节点在客户中的顺序,先按带宽从大到小,再按度从小到大
        //	edgelist3.remove(Integer.valueOf(edgeBan));
        System.out.println(edgelist3.size());
        streamT = (ArrayList<Stream>[]) new ArrayList[T];
        for(int t = 0; t < T; t++){
            streamT[t] = new ArrayList<Stream>();
            for(int i = 0; i < clientNodeSize; i++){
                streamT[t].addAll(clientSet[i].demandStream[t]);//addAll 把这个客户这一时刻的所有流加进去
            }
            Collections.sort(streamT[t]);
        }





/*
		for(int j : edgelist3){
			System.out.println(edgeNodeSet[j].avalibleClient.size()+","+edgeNodeSet[j].bandwidth);
		}
*/


        int edgeEnd = 0;
        ArrayList<Integer> edgeSuper = new ArrayList<Integer>();
        for(int j = edgelist3.size() -  edgeEnd; j < edgelist3.size(); j++){   //初始化超级节点
            edgeSuper.add(edgelist3.get(j));
        }


        //int[] xxx = sortList2(edgelist3.get(0));



//        for (int i=0 ; i <edgeNodeSize ; i++){
//            System.out.println(edgeNodeSet[i].superEdge);
//        }

        for(int j1 = 0; j1 < edgelist3.size() - edgeEnd ; j1++){   //分前百分之五
            int j = edgelist3.get(j1);
//            System.out.println(j);
            int[] demandEdge = new int[T];
//            System.out.println(edgeNodeSet[j].superEdge);



/*			if(j1 < edgelist3.size()/1.5) {
                for (int t1 = 0; t1 < T; t1++) {
                    for (int i : edgeNodeSet[j].avalibleClient) {
                        demandEdge[t1] += demandDynBase[i][t1];//存的是该时刻边缘节点可以接入的需求
                    }
                }
            }else{
                for (int t1 = 0; t1 < T; t1++) {
                    for (int i = 0; i < edgeNodeSet[j].avalibleClient.size(); i++) {
                        demandEdge[t1] += demandDyn[edgeNodeSet[j].avalibleClient.get(i)][t1];//存的是该时刻边缘节点可以接入的需求
                    }
                }
            }
            int[] max5ThisEdge = sortList(demandEdge); //找到该节点接入的最大五个时刻*/

            int[] max5ThisEdge;
            if(edgeNodeSet[j].superEdge){
//                   System.out.println(111);
                max5ThisEdge = sortList2(j, j1, edgelist3.size(),10,1); //找到该节点接入的最大五个时刻

            }else {
                max5ThisEdge = sortList2(j, j1, edgelist3.size(),0,0);
            }
            System.out.println(max5ThisEdge.length);

/*			if(j1 == 0){
				for(int t : max5ThisEdge){
					System.out.println(t);
				}
			}*/
//            System.out.println(max5ThisEdge.length);
            for(int t : max5ThisEdge){
                int streamInd = 0;
                while(true) {
                    if(streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthAvalible[t] == 0) break;//如果流分完了或者该边缘节点的带宽分完了退出去
                    Stream s = streamT[t].get(streamInd);
                    if(!edgeNodeSet[j].avalibleClient.contains(s.source)){
                        streamInd++;
                        continue;
                    }
                    if(edgeNodeSet[j].bandwidthAvalible[t] >= s.demand) {//把能塞进该节点的都塞进去
                        s.destination = j;
                        demandT[t] -= s.demand;
                        if(s.demand > base_cost) demandDynBase[s.source][t] -= s.demand;
                        demandDyn[s.source][t] -= s.demand;
                        edgeNodeSet[j].bandwidthAvalible[t] -= s.demand;
                        streamT[t].remove(streamInd);//分了之后移除该streamInd
                        continue;
                    }
                    streamInd++;
                }
                edgeNodeSet[j].max5.add(t);
            }
        }

        for(int j : edgeSuper){
            for(int x = 0; x < T/20; x++){
                int t = maxL(demandT);
                int streamInd = 0;
                while(true) {
                    if(streamInd >= streamT[t].size() || edgeNodeSet[j].bandwidthAvalible[t] == 0) break;//如果流分完了或者该边缘节点的带宽分完了退出去
                    Stream s = streamT[t].get(streamInd);
                    if(!edgeNodeSet[j].avalibleClient.contains(s.source)){
                        streamInd++;
                        continue;
                    }
                    if(edgeNodeSet[j].bandwidthAvalible[t] >= s.demand) {//把能塞进该节点的都塞进去
                        s.destination = j;
                        demandT[t] -= s.demand;
                        demandDyn[s.source][t] -= s.demand;
                        edgeNodeSet[j].bandwidthAvalible[t] -= s.demand;
                        streamT[t].remove(streamInd);//分了之后移除该streamInd
                        continue;
                    }
                    streamInd++;
                }
                edgeNodeSet[j].max5.add(t);
            }
        }

        for(ClientNode c : clientSet){    //把度为1的流提前分完就好了
            if(c.avalibleEdgeNode.size() != 1) continue;
            int j = c.avalibleEdgeNode.get(0);
            for(int t = 0; t < T; t++){
                for(Stream s : c.demandStream[t]){
                    if(s.destination != -1) continue;
                    s.destination = j;
                    streamT[t].remove(s);
                    demandT[t] -= s.demand;
                    demandDyn[s.source][t] -= s.demand;
                    edgeNodeSet[j].bandwidthAvalible[t] -= s.demand;
                }
            }
        }





        //int count = 0;
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



                //return Integer.compare(o2.demand /o2.avalibleEdgeSize, o1.demand/o1.avalibleEdgeSize);
            }
        });



        int demandSum = 0;
        ArrayList<Integer> sortTList = sortT(demandT);
        for(int t = 0; t < sortTList.size(); t++){
            demandSum += demandT[t];
            //System.out.println(demandT[sortTList.get(t)]);
        }
        System.out.println(demandSum);


        for(Stream streamThis : streamAll){
            //	if(streamThis.destination != -1) continue;
            int clientInd = streamThis.source;
            int t = streamThis.time;
            int flag = 0;  //判断程序是否需要跳出


/*            //第一优先级 max5时刻还能往里塞
            for(int j : clientSet[clientInd].avalibleEdgeNode){
                if(edgeNodeSet[j].max5.contains(t) && edgeNodeSet[j].bandwidthAvalible[t] >= streamThis.demand){
                    //   System.out.println("第一优先级" +":"+streamThis.demand);
                    streamThis.destination = j;
                    edgeNodeSet[j].bandwidthAvalible[t] -= streamThis.demand;
                    flag = 1;
                    break;
                }
            }
            if(flag == 1) continue;*/
/*

            //第二优先级 对已使用过的边缘节点新建一个max5时刻
            for(int j : clientSet[clientInd].avalibleEdgeNode){
                if((!edgeNodeSet[j].max5.contains(t)) && edgeNodeSet[j].max5.size() < T/20 && edgeNodeSet[j].max5.size() > 0 && edgeNodeSet[j].bandwidthAvalible[t] > streamThis.demand){
                    //  System.out.println("第二优先级" +":"+streamThis.demand);
                    streamThis.destination = j;
                    edgeNodeSet[j].bandwidthAvalible[t] -= streamThis.demand;
                    edgeNodeSet[j].max5.add(t);
                    flag = 1;
                    break;
                }
            }
            if(flag == 1) continue;
*/

            //第三优先级 若该流需求不大于V,判断是否能够分到后95时刻上而不超过V
            if(streamThis.demand <= base_cost){
/*				for(int j1 = clientSet[clientInd].avalibleEdgeNode.size() - 1; j1 >= 0; j1--){
					int j = clientSet[clientInd].avalibleEdgeNode.get(j1);*/
                for(int j : clientSet[clientInd].avalibleEdgeNode2){
                    if((edgeNodeSet[j].bandwidth - edgeNodeSet[j].bandwidthAvalible[t] + streamThis.demand) > base_cost){
                        continue;
                    }
                    if((!edgeNodeSet[j].max5.contains(t)) ){
                        streamThis.destination = j;
                        edgeNodeSet[j].bandwidthAvalible[t] -= streamThis.demand;
                        flag = 1;
                        break;
                    }
                }
            }
            if(flag == 1) continue;

/*            //第四优先级 启用未使用节点的max5时刻
            for(int j : clientSet[clientInd].avalibleEdgeNode){
                if(edgeNodeSet[j].max5.size() < T/20 && edgeNodeSet[j].bandwidthAvalible[t] > streamThis.demand){
                    //   System.out.println("第四优先级" +":"+streamThis.demand);
                    streamThis.destination = j;
                    edgeNodeSet[j].bandwidthAvalible[t] -= streamThis.demand;
                    edgeNodeSet[j].max5.add(t);
                    flag = 1;
                    break;
                }
            }
            if(flag == 1) continue;*/

            //第五优先级 此时所有节点的max5时刻已用完,后95时刻一定会超过基本费用,需要找到不超过最大费用的边缘节点
            for(int j : clientSet[clientInd].avalibleEdgeNode2){
/*			for(int j1 = clientSet[clientInd].avalibleEdgeNode.size() - 1; j1 >= 0; j1--){
				int j = clientSet[clientInd].avalibleEdgeNode.get(j1);*/
                if(edgeNodeSet[j].bandwidth - edgeNodeSet[j].bandwidthAvalible[t] + streamThis.demand <= edgeNodeSet[j].price95){

                    //    System.out.println("第五优先级" +":"+streamThis.demand);
                    streamThis.destination = j;
                    edgeNodeSet[j].bandwidthAvalible[t] -= streamThis.demand;
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
                if(edgeNodeSet[j].bandwidthAvalible[t] < streamThis.demand) continue;


                //这里恐怕只是单纯考虑了带宽没有考虑度,是否可以考虑给度大的节点多一点容错空间
                double priceIfNow = Math.pow(edgeNodeSet[j].bandwidth - edgeNodeSet[j].bandwidthAvalible[t] + streamThis.demand - base_cost , 2)/(double) edgeNodeSet[j].bandwidth;

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
                edgeNodeSet[edgeInd].price = priceIf;
                edgeNodeSet[edgeInd].bandwidthAvalible[t] -= streamThis.demand;
                //	difference[edgeInd] = edgeNodeSet[edgeInd].bandwidth - edgeNodeSet[edgeInd].bandwidthAvalible[t] - edgeNodeSet[edgeInd].price95;
                edgeNodeSet[edgeInd].price95 = edgeNodeSet[edgeInd].bandwidth - edgeNodeSet[edgeInd].bandwidthAvalible[t];
                flag = 1;
            }

            if(flag == 0){
                System.out.println(streamThis.demand +","+ "该流的度" +":" + clientSet[clientInd].avalibleEdgeNode.size() );
            }


        }




        output();
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
/*        int sum = 0;
        for(int t = 0; t < T; t++){
            System.out.println(t + ":" + demandT[t]);
            sum += demandT[t];
        }
        System.out.println(sum);*/

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
        try {
            br.readLine();
            line1 = br.readLine();
            String[] x = line1.split("=");
            line1 = br.readLine();
            qos = Integer.parseInt(x[1]);
            x = line1.split("=");
            cost = Integer.parseInt(x[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new int[]{qos, cost};
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
    public static int[] sortAvaliable(int clientInd , int t){
        int[] result = new int[clientSet[clientInd].avalibleEdgeNode.size()];
        int count = 0;
        int temp = 0;
        int Du = 0;
        for(int j = 0; j < result.length ; j++) {
            for (int i : clientSet[clientInd].avalibleEdgeNode) {
                if (edgeNodeSet[i].bandwidthAvalible[t] > temp) {
                    temp = edgeNodeSet[i].bandwidthAvalible[t];
                    Du = edgeNodeSet[i].avalibleClient.size();
                    result[count] = i;
                } else if (edgeNodeSet[i].bandwidthAvalible[t] == temp) {
                    if (edgeNodeSet[i].avalibleClient.size() < Du) {
                        result[count] = i;
                    }
                }
            }
            count++;


        }
        return result;



    }


    public static int[] sortList2(int j, int j1, int j2, int a,int b){   //取的逻辑 可以优先取去掉小流的最大时刻,当此时发现比较小时,再选取不去掉小流的
        int[] result = new int[T/(20-a)-b];
        int[] demandEdgeBase = new int[T];

        //初始化大流需求矩阵
        for(int t = 0; t < T; t++){
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
/*            if(j1 < j2/2) {
                if (temp < edgeNodeSet[j].bandwidth* 2 / 3) break;
            }
            else {
                if(temp < edgeNodeSet[j].bandwidth/2) break;
            }*/
            if(temp < edgeNodeSet[j].bandwidth/3) break;
            demandEdgeBase[result[i]] = 0;
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
/*				if(o1.avalibleClient.size() > o2.avalibleClient.size()) return 1;   //这是度从小到大
				else if(o1.avalibleClient.size() < o2.avalibleClient.size()) return -1;
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

    public static ArrayList<Integer> sortEdge2(EdgeNode[] set){

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
    public static void sortEdge_top10(EdgeNode[] set){
//        ArrayList<Integer> result = new ArrayList<Integer>();
        ArrayList<EdgeNode> temp = new ArrayList<EdgeNode>();
        for (int i = 0; i < edgeNodeSize; i++) {
            temp.add(set[i]);
        }
        Collections.sort(temp,new Comparator<EdgeNode>(){
            @Override
            public int compare(EdgeNode o1, EdgeNode o2) {
                return Integer.compare(o2.bandwidth * o2.avalibleClient.size(), o1.bandwidth * o1.avalibleClient.size());
            }
        });
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < edgeNodeSize; j++) {
                if(temp.get(i).name.equals(set[j].name)){
                    System.out.println(temp.get(i).bandwidth * temp.get(i).avalibleClient.size());
                    edgeNodeSet[j].superEdge=true;
                    break;
                }
            }

        }
    }

    public static ArrayList<Integer> sortEdge4(EdgeNode[] set){

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
            int flagSuper = 0;
            for(EdgeNode e : edgeNodeSet){
                if(e.superEdge){
                    if(flagSuper == 1) bw.write(",");
                    bw.write(e.name);
                    System.out.println(e.name);
                    flagSuper = 1;
                }
            }
            bw.newLine();
            for (int t = 0; t < T; t++) {
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



